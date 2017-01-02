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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

/**
 * Main class for simulating the NK Model
 * @author Sharad Singhal
 */
public class Simulation {
	/** Print stream to write statistics */
	private PrintStream out = null;
	/** Configuration to use */
	private Configuration config;
	/** Population to use in the simulation */
	private Population pop;
		
	/**
	 * Create a NK-Model simulation
	 * @param args - command-line arguments to the program
	 */
	public Simulation(String [] args){
		// get the configuration
		config = new Configuration(args);
		// create the initial population
		switch(config.getType()){
		case FAST:
			pop = new PopulationCounter(config);
			break;
		case DETAILED:
			pop = new BinaryPopulation(config);
			break;
		default:
			throw new RuntimeException("Unknown simulation type - "+config.getType());
		}
		return;
	}
	
	/**
	 * Method to run the simulation
	 */
	public void runSimulation(){
		// write out the landscapes
		config.getLandscape().writeLandscape(config.getFileName("land-"));
		config.getShockLandscape().writeLandscape(config.getFileName("sland-"));
		// open trace file to write the simulation statistics
		String statsFile = config.getFileName("stats-");
		// report progress on stdout every progress generations
		int progress = config.progressIndicator();
		try {
			// open the population for writing traces
			pop.open();
			out = new PrintStream(new File(statsFile));
			out.println("gen population uniques average stdev diversity cutoff");

			// run through the population
			if(progress > 0) System.out.println("gen   population uniques   average  stdev diversity cutoff");
			int maxGenerations = config.getMaxGenerations();
			writeStats();	// write the initial population statistics
			while(pop.getGeneration() <= maxGenerations){
				// advance the population (replicate then select)
				if(!pop.advance()){
					System.out.println("No individuals in population after Generation "+pop.getGeneration());
					break;
				}
				// periodically we give the population a shock
				float shock = config.getShock(pop.getGeneration());
				if(shock > 0 && !pop.shock(shock)){
					System.out.println("No individuals in (shocked) population after Generation "+pop.getGeneration());
					break;
				}
				// write out the stats at the end of each generation
				writeStats();
			}
			// write the population remaining at the end of the simulation
			pop.writePopulation();
		} catch (FileNotFoundException e) {
			// should not happen
			System.out.println(e.toString());
		} finally {
			if(out != null) out.close();;
			pop.close();
		}
		return;
	}
	
	/** 
	 * write a trace of the simulation
	 */
	private void writeStats(){
		int generation = pop.getGeneration();
		int size = pop.populationSize();
		int unique = pop.uniqueGenomes();
		double af = pop.getAverageFitness();
		double sd = pop.getStandardDeviation();
		double div = pop.getShannonDiversity();
		double cut = config.getCutoff(generation);
		int progress = config.progressIndicator();
		if(progress > 0 && generation % progress == 0){
			System.out.println(String.format("%4d %8d %8d %9.3f %9.3f %9.3f %6.2f",generation,size,unique,af,sd,div,cut));
		}
		out.println(String.format("%4d %8d %8d %9.3f %9.3f %9.3f %6.2f",generation,size,unique,af,sd,div,cut));
		return;
	}
	
	/**
	 * Main program to run the NK-model simulations.
	 * @param args - program arguments. Run with "Simulation -h" to get help
	 */
	public static void main(String[] args) {
		System.out.println(Configuration.banner);
		System.out.println(Configuration.copyright);
		// show help and exit if program called with -h
		if(args.length > 0 && args[0].startsWith("-h")) {
			Configuration.showHelp();
			return;
		}
		// create the simulation
		Simulation sim = new Simulation(args);
		// run the simulation
		sim.runSimulation();
		return;
	}
}
