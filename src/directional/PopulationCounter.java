/**
 * Copyright (C) 2016, Sonia Singhal
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
	/** fitness values of the genomes. Indexed by genome value */
	private float fitness[];
	/** Genome size in bits */
	private int N;
	/** Trace writer to write the simulation trace to a file */
	private TraceWriter writer = null;
	/** average fitness for the population */
	private double average;
	/** standard deviation for the population */
	private double stdev;
	/** shannon diversity for the population */
	private double diversity;
	/** maximum population size limit */
	private int maxPopulation;
	/** growth factor per generation */
	private double alpha;
	/** current population size */
	private int populationSize;
	
	/**
	 * Create a population counter
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
		fitness = new float[maxGenomes];
		Landscape landscape = config.getLandscape();
		// note we read landscapes before population, to allow computation of fitness
		String populationFile = config.getPopulationFile();
		if(populationFile == null || !readPopulation()){
			// if no population file given, or if we could not read it, create an initial random population
			for(int i = 0; i < config.getInitialPopulationSize(); i++){
				int val = (int) config.getRandomGeneValue();
				genomes.set(val);
				count[val]++;
				fitness[val] = landscape.getFitness(val);
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
	 * Compute the population statistics for the current population
	 */
	private void computeStatistics(){
		populationSize = 0;
		Iterator<Integer> iter = genomes.stream().iterator();
		while(iter.hasNext()){
			int g = iter.next();
			populationSize += count[g];
		}
		if(populationSize == 0) {
			average = stdev = diversity = -1;
			return;
		}
		
		double size = populationSize;
		double n = uniqueGenomes();
		double sum = 0, sumA = 0, sumOfSquares = 0;
		double denom = Math.log(size);
		iter = genomes.stream().iterator();
		while(iter.hasNext()){
			int g = iter.next();
			double f = fitness[g] * count[g];
			sumA += f;
			sumOfSquares += f * f;
			sum += count[g] * (Math.log(count[g])-denom);		// sum pi * ln(pi); where pi = count[i]/size
		}
		diversity = -sum/size;
		average = sumA/size;
		stdev = Math.sqrt((n*sumOfSquares - sumA * sumA))/n;
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

	/* (non-Javadoc)
	 * @see jnk.Population#getAverageFitness()
	 */
	@Override
	public double getAverageFitness() {
		return average;
	}

	/* (non-Javadoc)
	 * @see jnk.Population#getStandardDeviation()
	 */
	@Override
	public double getStandardDeviation() {
		return stdev;
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
		float cutoff = config.getCutoff(generation);	// fitness cutoff for this generation
		Landscape landscape = config.getLandscape();	// landscape for this generation
		
		// Recall that Java does not let us modify the bitset while we are iterating on it,
		// so we must use two different bitsets
		
		// Replication phase: mutate genes and update population for existing genomes
		// compute probability of replication
		double prob = maxPopulation == 0 ? 1.0 : Math.max(0.0, alpha * (1.-(double)populationSize/(double)maxPopulation));
		workingSet.clear();	// clear the working set
		Iterator<Integer> iter = genomes.stream().iterator();
		while(iter.hasNext()){	// collect the off-spring in the working set
			int g = iter.next();
			double replProb = config.getReplicationProbability(fitness[g]) * prob;
			for(int i = 0; i < count[g]; i++){	// for each individual of this genotype
				// flip a coin to see if it generates offspring
				if(config.randomFloat() < replProb){
					int offspring = mutate(g);
					float ofit = fitness[offspring] > 0 ? fitness[offspring] : 
						(fitness[offspring] = landscape.getFitness(offspring));
					// if the offspring would survive the selection phase, add it to the population
					if(ofit >= cutoff){
						count[offspring]++;	
						if(!genomes.get(offspring)) workingSet.set(offspring);
					}
				}
			}
		}
		// mark the new offspring in the genome population
		genomes.or(workingSet);

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
			writer.write(generation, genomes, count, fitness, cutoff);
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
		
		// note that the shock landscape can be different from replication, so all fitness values have to be computed
		Landscape shockScape = config.getShockLandscape();
		
		// we just have a selection phase for the shocks
		workingSet.clear();
		Iterator<Integer> iter = genomes.stream().iterator();
		while(iter.hasNext()){	// collect genes in the working set
			int g = iter.next();
			if(shockScape.getFitness(g) < shock){
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
			writer.write(generation, genomes, count, fitness,(float)0.0,shock);
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
			out.println("# genome count");
			Iterator<Integer> iter = genomes.stream().iterator();
			while(iter.hasNext()){
				int g = iter.next();
				out.println(String.format("%d %d", g,count[g]));
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
		try {
			BufferedReader inp = new BufferedReader(new InputStreamReader(new FileInputStream(new File(populationFile))));
			String line;
			while((line = inp.readLine()) != null){
				if(line.isEmpty() || line.startsWith("#")) continue;
				String [] parts = line.split(" ");
				if(parts.length != 2){
					inp.close();
					throw new IOException("Unable to read population file "+populationFile+" Expected 2 values found "+parts.length);
				}
				int g = Integer.valueOf(parts[0]);
				genomes.set(g);
				count[g] = Integer.valueOf(parts[1]);
				fitness[g] = landscape.getFitness(g);
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
			if(status) writer.write(generation, genomes, count, fitness, config.getCutoff(generation));
		}
		return status;
	}

}
