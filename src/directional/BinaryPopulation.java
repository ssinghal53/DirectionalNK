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
import java.util.HashMap;
import java.util.HashSet;

/**
 * @author Sharad Singhal
 *
 */
public class BinaryPopulation implements Population {
	/** All genomes in the current population */
	private HashSet<Genome> currentGenomes = new HashSet<Genome>();
	/** Current generation being used */
	private int generation = 0;
	/** current configuration being used */
	Configuration configuration;
	
	/**
	 * Create a Genome population with known size and landscape
	 */
	public BinaryPopulation(Configuration config) {
		this.configuration = config;
		String populationFile = config.getPopulationFile();
		if(populationFile == null || !readPopulation()){
			// create an initial random population
			for(int i = 0; i < config.getInitialPopulationSize(); i++){
				currentGenomes.add(new BinaryGenome(config));
			}
			// if populationFile was given, and we were not able to read it, create it
			if(!(populationFile == null)){
				writePopulation(populationFile);
			}
		}
		return;
	}
	
	/*
	 * (non-Javadoc)
	 * @see jnk.Population#advance()
	 */
	public boolean advance(){
		double cutoff = configuration.getCutoff(generation);
		select(cutoff);
		if(currentGenomes.size() == 0) return false;
		replicate();
		generation++;
		return true;
	}
	
	/**
	 * Reduce the population by killing all genomes that are below the given fitness threshold
	 * @param fitness_cutoff - fitness threshold to use for killing individuals
	 */
	private void select(double fitness_cutoff){
		HashSet<Genome> killset = new HashSet<Genome>();
		for(Genome g : currentGenomes){
			if(g.getFitness() < fitness_cutoff){
				killset.add(g);
				g.setDeathGeneration(generation);
			}
		}
		currentGenomes.removeAll(killset);
		return;
	}
	
	/**
	 * Replicate each gene in the population by mutation
	 * All new children are added to the population
	 */
	private void replicate(){
		HashSet<Genome> children = new HashSet<Genome>();
		for(Genome g : currentGenomes){
			children.add(g.mutate());
		}
		currentGenomes.addAll(children);
		return;
	}
	
	/*
	 * (non-Javadoc)
	 * @see jnk.Population#uniqueSize()
	 */
	public int uniqueGenomes(){
		HashSet<Integer> unique = new HashSet<Integer>();
		for(Genome g : currentGenomes){
			Integer value = g.getValue();
			if(!unique.contains(new Integer(value))){
				unique.add(value);
			}
		}
		return unique.size();
	}
	
	/*
	 * (non-Javadoc)
	 * @see jnk.Population#totalSize()
	 */
	public int populationSize(){
		return currentGenomes.size();
	}
	
	/*
	 * (non-Javadoc)
	 * @see jnk.Population#getShannonDiversity()
	 */
	public double getShannonDiversity(){
		HashMap<Integer,Integer> unique = new HashMap<Integer,Integer>();
		for(Genome g : currentGenomes){
			Integer value = g.getValue();
			if(unique.containsKey(value)){
				unique.replace(value, unique.get(value)+1);
			} else {
				unique.put(value, new Integer(1));
			}
		}
		double size = currentGenomes.size();
		double sum = 0;
		for(Integer v : unique.values()){
			sum += v * Math.log(v/size);
		}
		return -sum/size;
	}
	
	/*
	 * (non-Javadoc)
	 * @see jnk.Population#getAverageFitness()
	 */
	public double getAverageFitness(){
		if(currentGenomes.size() == 0){
			return -1;
		}
		double sum = 0;
		for(Genome g : currentGenomes){
			sum += g.getFitness();			
		}
		return sum / currentGenomes.size();
	}
	
	/*
	 * (non-Javadoc)
	 * @see jnk.Population#getStandardDeviation()
	 */
	public double getStandardDeviation(){
		if(currentGenomes.size() == 0){
			return -1;
		}
		double n = currentGenomes.size();
		double sum = 0;
		double sumOfSquares = 0;
		for(Genome g : currentGenomes){
			double v = g.getFitness();
			sum += v;
			sumOfSquares += v * v;
		}
		return Math.sqrt((n*sumOfSquares - sum * sum))/n;
	}
	
	/**
	 * Dump the genomes in the population to a file
	 */
	public void writePopulation() {
		String outputFile = configuration.getFileName("pop-");
		writePopulation(outputFile);
		return;
	}

	/* (non-Javadoc)
	 * @see jnk.Population#getGeneration()
	 */
	@Override
	public int getGeneration() {
		return generation;
	}

	/* (non-Javadoc)
	 * @see jnk.Population#readPopulation()
	 */
	@Override
	public boolean readPopulation() {
		String populationFile = configuration.getPopulationFile();
		int N = configuration.getN();
		try {
			BufferedReader inp = new BufferedReader(new InputStreamReader(new FileInputStream(new File(populationFile))));
			String line;
			while((line = inp.readLine()) != null){
				if(line.isEmpty() || line.startsWith("#")) continue;
				String [] parts = line.split(" ");
				if(parts.length != 3){
					inp.close();
					throw new IOException("Unable to read population file "+populationFile+" Expected 3 values found "+parts.length);
				}
				if(parts[0].length() != N){
					inp.close();
					throw new IOException("Expected Genome size "+N+" found "+parts[0].length());
				}
				int g = Integer.parseUnsignedInt(parts[0], 2);
				int count = Integer.valueOf(parts[1]);
				float fitness = Float.valueOf(parts[2]);
				for(int i = 0; i < count; i++){
					currentGenomes.add(new BinaryGenome(configuration,g,fitness));
				}
			}
			inp.close();
			return true;
		} catch (IOException e) {
			System.out.println(e.toString());
		}
		return false;
	}
	
	/**
	 * Write a population file
	 * @param populationFile
	 */
	private void writePopulation(String populationFile) {
		// obtain counts and fitness value for each genome in the population
		HashMap<String,Integer> counts = new HashMap<String,Integer>();
		HashMap<String,Genome> unique = new HashMap<String,Genome>();
		for(Genome g : currentGenomes){
			String genome = g.toString();
			if(!unique.containsKey(genome)){
				unique.put(genome, g);
			}
			if(counts.containsKey(genome)){
				counts.replace(genome, counts.get(genome)+1);
			} else {
				counts.put(genome, 1);
			}
		}
		try {
			PrintStream out = new PrintStream(new File(populationFile));
			out.println("# created "+ZonedDateTime.now().toString());
			out.println("# by BinaryPopulation");
			out.println("# genome count fitness");
			for(String key : unique.keySet()){
				Genome g = unique.get(key);
				out.println(String.format("%s %d %f", key,counts.get(key), g.getFitness()));
			}
			out.close();
		} catch (FileNotFoundException e) {
			System.out.println("Unable to open file "+populationFile);
		}
		return;

	}

	/* (non-Javadoc)
	 * @see jnk.Population#open()
	 */
	@Override
	public boolean open() {
		// TODO: Currently we do not generate traces from here...
		return true;
	}

	/* (non-Javadoc)
	 * @see jnk.Population#close()
	 */
	@Override
	public void close() {
		// TODO: This needs to be finished if we end up using this implementation...
	}

	/* (non-Javadoc)
	 * @see jnk.Population#shock()
	 */
	@Override
	public boolean shock(float shock) {
		throw new RuntimeException("BinaryPopulation.shock() is not yet implemented");
	}
}
