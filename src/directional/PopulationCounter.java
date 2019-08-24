/**
 * Copyright (C) 2016, 2017 Sonia Singhal
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package directional;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;

/**
 * PopulationCounter - tracks populations by their counts
 * @author Sharad Singhal
 */
public class PopulationCounter implements Population {
	/** Simulation configuration */
	private Configuration config;
	/** current generation */
	private int generation;
	/** maximum possible genomes */
	private int maxGenomes;
	/** bitset containing active genomes. Indexed by genome value */
	private BitSet genomes;
	/** bit set used as working set in the simulation. Indexed by genome value */
	private BitSet workingSet;
	/** array containing genome counts in population. Indexed by genome value */
	private int count[];
	/** count of offspring during replication */
	private int offspringCount[];
	/** fitness values of the genomes. Indexed by genome value */
	private float fitness[];
	/** fitness values of the genomes under the shock landscape. Indexed by genome value */
	private float shockFitness[];
	/** Genome size in bits */
	private int N;
	/** Trace writer to write the simulation trace to a file */
	private TraceWriter writer = null;
	/** average fitness for the population */
	private double average;
	/** average fitness for the population under shocks */
	private double shockAverage;
	/** standard deviation for the population */
	private double stdev;
	/** standard deviation for the population under shocks */
	private double shockStDev;
	/** shannon diversity for the population */
	private double diversity;
	/** maximum population size limit */
	private int maxPopulation;
	/** growth factor per generation */
	private double alpha;
	/** current population size */
	private int populationSize;
	/** evenness of population */
	private double even;
	/** Replication Landscape being used */
	private Landscape landscape;
	/** Shock landscape being used */
	private Landscape shockLandscape;
	
	/**
	 * Create a population counter
	 * @param config - Configuration to use
	 */
	public PopulationCounter(Configuration config) {
		this.config = config;
		N = config.getN();
		generation = 0;
		maxGenomes = config.getMaxGenomes();
		maxPopulation = config.getMaxPopulation();
		alpha = config.getAlpha();
		genomes = new BitSet(maxGenomes);
		workingSet = new BitSet(maxGenomes);
		count = new int[maxGenomes];
		offspringCount = new int[maxGenomes];
		fitness = new float[maxGenomes];
		shockFitness = new float[maxGenomes];
		landscape = config.getLandscape();
		// note we read landscapes before population, to allow computation of fitness
		shockLandscape = config.getShockLandscape();
		String populationFile = config.getPopulationFile();
		if(populationFile == null || !readPopulation()){
			// if no population file given, or if we could not read it, create an initial random population
			for(int i = 0; i < config.getInitialPopulationSize(); i++){
				int val = (int) config.getRandomGeneValue();
				genomes.set(val);
				count[val]++;
				fitness[val] = landscape.getFitness(val);
				shockFitness[val] = shockLandscape.getFitness(val);
			}
			// if populationFile was given, and we were not able to read it, create it
			if(populationFile != null){
				writePopulation(populationFile);
			}
		}
		// compute statistics based on initial population (generation = 0)
		computeStatistics();
		
		return;
	}

	/**
	 * Compute the population statistics for the current population generation
	 */
	private void computeStatistics(){
		// compute the current population size
		populationSize = 0;
		Iterator<Integer> iter = genomes.stream().iterator();
		while(iter.hasNext()){
			int g = iter.next();
			populationSize += count[g];
		}
		// if everyone is extinct, no stats can be obtained
		if(populationSize == 0) {
			average = shockAverage = stdev = shockStDev = diversity = even = -1;
			return;
		}
		// compute the descriptive statistics
		double size = populationSize;
		double n = uniqueGenomes();
		double sum = 0, sumA = 0, sumOfSquares = 0;
		double shockSumA = 0, shockSumOfSquares = 0;
		double denom = Math.log(size);
		iter = genomes.stream().iterator();
		while(iter.hasNext()){
			int genome = iter.next();
			double f = fitness[genome] * count[genome];			// sum of fitness for all genomes with this value on replication landscape
			double sf = shockFitness[genome] * count[genome];	// sum of fitness for all genomes with this value on shock landscape
			sumA += f;
			shockSumA += sf;
			sumOfSquares += fitness[genome] * f;				// sum of fitness^2 for all genomes with this value
			shockSumOfSquares += shockFitness[genome] * sf;
			sum += count[genome] * (Math.log(count[genome])-denom);		// sum pi * ln(pi); where pi = count[i]/uniques	
		}
		diversity = -sum/size;
		even = diversity / Math.log(n);
		average = sumA/size;
		stdev = Math.sqrt((sumOfSquares - sumA * sumA / size)/(size-1));
		shockAverage = shockSumA/size;
		shockStDev = Math.sqrt((shockSumOfSquares - shockSumA * shockSumA / size)/(size-1));
		return;
	}
	
	/* (non-Javadoc)
	 * @see jnk.Population#getGeneration()
	 */
	@Override
	public int getGeneration() {
		return generation;
	}
	
	/*
	 * (non-Javadoc)
	 * @see jnk.Population#uniqueGenomes()
	 */
	@Override
	public int uniqueGenomes() {
		return genomes.cardinality();
	}

	/* (non-Javadoc)
	 * @see jnk.Population#totalSize()
	 */
	@Override
	public int populationSize() {
		return populationSize;
	}
	
	/* (non-Javadoc)
	 * @see jnk.Population#getShannonDiversity()
	 */
	@Override
	public double getShannonDiversity() {
		return diversity;
	}
	
	/*
	 * (non-Javadoc)
	 * @see directional.Population#getEvenness()
	 */
	@Override
	public double getEvenness(){
		return even;
	}

	/* (non-Javadoc)
	 * @see jnk.Population#getAverageFitness()
	 */
	@Override
	public double getAverageFitness() {
		return average;
	}
	/*
	 * (non-Javadoc)
	 * @see directional.Population#getAverageShockFitness()
	 */
	@Override
	public double getAverageShockFitness(){
		return shockAverage;
	}

	/* (non-Javadoc)
	 * @see jnk.Population#getStandardDeviation()
	 */
	@Override
	public double getStandardDeviation() {
		return stdev;
	}
	
	/*
	 * (non-Javadoc)
	 * @see directional.Population#getShockStDev()
	 */
	@Override
	public double getShockStDev(){
		return shockStDev;
	}
	
	/*
	 * (non-Javadoc)
	 * @see directional.Population#getMaxFit()
	 */
	@Override
	public float getMaxFit(){
		float maxFit = 0;
		for(int i = 0; i < maxGenomes; i++){
			if(maxFit < fitness[i]) maxFit = fitness[i];
		}
		return maxFit;
	}
	
	/*
	 * (non-Javadoc)
	 * @see directional.Population#getMaxShockFit()
	 */
	@Override
	public float getMaxShockFit(){
		float maxShockFit = 0;
		Iterator<Integer> iter = genomes.stream().iterator();
		while(iter.hasNext()){
			int g = iter.next();
			float sfit = shockFitness[g];
			if(maxShockFit < sfit) maxShockFit = sfit;
		}
		return maxShockFit;
	}
	
	/* (non-Javadoc)
	 * @see jnk.Population#advance()
	 */
	@Override
	public boolean advance() {
		if(genomes.cardinality() == 0){
			// we have nothing left in the population, update statistics and return
			computeStatistics();
			return false;	
		}
		// increment the generation counter
		generation++;
		float cutoff = config.getCutoff(generation);	// replication fitness cutoff for this generation
		
		// Recall that Java does not let us modify the bitset while we are iterating on it,
		// so we must use two different bitsets
		
		// Replication phase: mutate genes and update population for existing genomes
		// compute probability of replication
		float prob = maxPopulation == 0 ? 1.0F : Math.max(0.0F, Math.min(1.0F, (float)(alpha * (1.-(double)populationSize/(double)maxPopulation))));
		workingSet.clear();	// clear the working set
		Arrays.fill(offspringCount, 0);	// reset the offspring counts
		Iterator<Integer> iter = genomes.stream().iterator();
		while(iter.hasNext()){	// collect the off-spring in the working set
			int g = iter.next();
			float replProb = config.getReplicationProbability(fitness[g]) * prob;
			int imax = count[g];
			for(int i = 0; i < imax; i++){	// for each individual of this genotype
				// flip a coin to see if it generates offspring
				if(config.randomFloat() < replProb){
					int offspring = mutate(g);
					float ofit = fitness[offspring] > 0 ? fitness[offspring] : 
						(fitness[offspring] = landscape.getFitness(offspring));
					if(shockFitness[offspring] == 0) shockFitness[offspring] = shockLandscape.getFitness(offspring);
					// if the offspring would survive the selection phase, add it to the population
					if(ofit >= cutoff){
						offspringCount[offspring]++;	
						if(!genomes.get(offspring)) workingSet.set(offspring);
					}
				}
			}
		}
		// mark the new offspring in the genome population
		genomes.or(workingSet);
		for(int i= 0; i < maxGenomes; i++){
			count[i] += offspringCount[i];
		}
		// Selection phase: remove any genes that fall below the cutoff. 
		workingSet.clear();
		iter = genomes.stream().iterator();
		while(iter.hasNext()){	// collect low-fitness genes in the working set
			int g = iter.next();
			if(fitness[g] < cutoff){
				workingSet.set(g);
				count[g] = 0;	// set their count to zero
			}
		}
		genomes.andNot(workingSet);		// and clear them from the pool
		
		// compute the statistics for this generation
		computeStatistics();
		if(genomes.cardinality() == 0){
			return false;	// return if nothing left after selection
		}

		// At this point, we have gone through selection; write out the trace
		if(writer != null){
			writer.write(generation, genomes, count, fitness, cutoff, shockFitness, 0.0F);
		}				
		return true;
	}
	
	/*
	 * (non-Javadoc)
	 * @see jnk.Population#shock()
	 */
	public boolean shock(float shock){
		if(genomes.cardinality() == 0) return false; // we have nothing left in the population, return
		if(shock < 0) return true;	// nothing affected
		System.out.println("Generation - "+generation+" Shock "+shock);
		
		// we just have a selection phase for the shocks
		workingSet.clear();
		Iterator<Integer> iter = genomes.stream().iterator();
		while(iter.hasNext()){	// collect genes in the working set
			int g = iter.next();
			//if(shockLandscape.getFitness(g) < shock){
			if(shockFitness[g] < shock){
				workingSet.set(g);	// this genome will not survive
				count[g] = 0;		// set its count to zero
			}
		}
		genomes.andNot(workingSet);	// and clear killed individuals from the pool
		
		// re-compute the new statistics after the shock
		computeStatistics();
		
		if(genomes.cardinality() == 0){
			return false;	// return if nothing left after selection
		}

		// At this point, we have gone through shock selection; write out the trace
		if(writer != null){
			writer.write(generation, genomes, count, fitness,(float)0.0,shockFitness,shock);
		}
		return true;
	}
	
	/**
	 * Mutate a gene
	 * @param g -gene value to mutate
	 * @return - value of mutated gene
	 */
	private int mutate(int g) {
		switch(config.getMutationStrategy()){
		case REPLICATE:
			return g;
		case SINGLE_RANDOM:
			int mask = 1 << (int)(config.randomFloat()*N);	// mask has a single random bit[0:N) = 1
			int value = g ^ mask;			// mutate that bit in the parent to generate this genome
			return value;
		case MULTI_RANDOM:
			float p = config.getMutationProbability();
			mask = 1;
			value = g;
			for(int i = 0; i < N; i++){
				if(config.randomFloat() < p) value ^= mask;
				mask <<= 1;
			}
			return value;
		default:
			break;
		}
		throw new RuntimeException("PopulationCounter: "+config.getMutationStrategy()+" not yet implemented");
	}
	
	/* (non-Javadoc)
	 * @see jnk.Population#writePopulation()
	 */
	@Override
	public void writePopulation() {
		String outputFile = config.getFileName("pop-");
		writePopulation(outputFile);
		return;
	}
	
	/**
	 * Write the current population to a file
	 * @param outputFile - file to write
	 */
	private void writePopulation(String outputFile){
		try {
			PrintStream out = new PrintStream(new File(outputFile));
			out.println(Configuration.banner);
			out.println("# Created "+ZonedDateTime.now().toString());
			out.println("# N = "+N+", K = "+config.getK()+", seed = "+config.getSeed()+", shockseed = "+config.getSseed());
			out.println("# gen genome count fitness shockfitness");
			Iterator<Integer> iter = genomes.stream().iterator();
			while(iter.hasNext()){
				int g = iter.next();
				out.println(String.format("%d %d %d %f %f", generation, g,count[g], fitness[g], shockFitness[g]));
			}
			out.close();
		} catch (FileNotFoundException e) {
			System.out.println("Unable to open file "+outputFile);
		}
		return;
	}

	/**
	 * Read the population values from a file
	 * @return - true if successful, false if read failed
	 */
	public boolean readPopulation() {
		String populationFile = config.getPopulationFile();
		Landscape landscape = config.getLandscape();
		Landscape shockLandscape = config.getShockLandscape();
		try {
			BufferedReader inp = new BufferedReader(new InputStreamReader(new FileInputStream(new File(populationFile))));
			String line;
			while((line = inp.readLine()) != null){
				if(line.isEmpty() || line.startsWith("#")) continue;
				String [] parts = line.split(" ");
				if(parts.length < 2){
					inp.close();
					throw new IOException("Unable to read population file "+populationFile+" Expected at least 2 values found "+parts.length);
				} else if(parts.length == 2){
					// have genome count
					int g = Integer.valueOf(parts[0]);
					genomes.set(g);
					count[g] = Integer.valueOf(parts[1]);
					fitness[g] = landscape.getFitness(g);
					shockFitness[g] = shockLandscape.getFitness(g);
				} else {
					// have generation genome count fitness shockfitness
					int g = Integer.valueOf(parts[1]);
					genomes.set(g);
					count[g] = Integer.valueOf(parts[2]);
					fitness[g] = landscape.getFitness(g);
					shockFitness[g] = shockLandscape.getFitness(g);
				}
			}
			inp.close();
			return true;
		} catch (IOException e) {
			System.out.println(e.toString());
		}
		return false;
	}
		
	/*
	 * (non-Javadoc)
	 * @see jnk.Population#close()
	 */
	public void close(){
		if(writer != null) writer.close();
	}

	/* (non-Javadoc)
	 * @see jnk.Population#open()
	 */
	@Override
	public boolean open() {
		boolean status = false;
		if(config.hasOption("trace")){
			writer = TraceWriter.getWriter(config);
			status =  writer.open();
			if(status) writer.write(generation, genomes, count, fitness, config.getCutoff(generation), shockFitness, 0.0F);
		}
		return status;
	}

}
