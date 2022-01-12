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
	/** Print stream to write detailed population */
	private PrintStream popout = null;
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
		pop = new PopulationCounter(config);
		return;
	}
	
	/**
	 * Method to run the simulation
	 */
	public void runSimulation(){
		// write out the landscapes
		config.getLandscape().writeLandscape(config.getFileName("land-"));
		if (config.getShockState()) config.getShockLandscape().writeLandscape(config.getFileName("sland-"));
		
		// open stats file to write the simulation statistics
		String statsFile = config.getFileName("stats-");
		// report progress on stdout every progress generations
		int progress = config.progressIndicator();
		try {
			// open the population for writing simulation trace
			pop.open();
			// open the stats file
			out = new PrintStream(new File(statsFile));
			out.println("gen population uniques average stdev diversity evenness max cutoff shockAvg shockStd shockMax shockCut"); //header for file

			boolean tracePopulation = config.tracePopulation();
			if(tracePopulation) popout = new PrintStream(new File(config.getFileName("popt-")));
			
			
			// run through the population
			if(progress > 0) System.out.println("gen   population uniques   average  stdev diversity evenness cutoff maxFit"); //header for console
			int maxGenerations = config.getMaxGenerations();
			writeStats();	// write the initial population statistics
			while(pop.getGeneration() <= maxGenerations){
				if(tracePopulation) pop.writePopulation(popout);	// write out the population trace
				// Check whether population is likely to go extinct this round; if so, write it out.
				if(config.getShock(pop.getGeneration()+1) > pop.getMaxShockFit() || config.getCutoff(pop.getGeneration()+1) > pop.getMaxFit()){
					pop.writePopulation();
				}
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
			if(pop.populationSize() > 0) pop.writePopulation();
		} catch (FileNotFoundException e) {
			// should not happen
			System.out.println(e.toString());
		} finally {
			if(out != null) out.close();;
			pop.close();
			if(popout != null) popout.close();
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
		double ev = pop.getEvenness();
		double cut = config.getCutoff(generation);
		double maxfit = pop.getMaxFit();
		double asf = pop.getAverageShockFitness();
		double ssd = pop.getShockStDev();
		double smax = pop.getMaxShockFit();
		double scut = config.getShock(generation);
		if(scut < 0) scut = 0;
		int progress = config.progressIndicator();
		if(progress > 0 && generation % progress == 0){
				System.out.println(String.format("%4d %8d %8d %9.3f %9.3f %9.3f %9.3f %6.2f %6.2f",generation,size,unique,af,sd,div,ev,cut,maxfit)); //console print
		}
		out.println(String.format("%4d %8d %8d %9.3f %9.3f %9.3f %9.3f %6.2f %6.2f %9.3f %9.3f %9.3f %6.2f",generation,size,unique,af,sd,div,ev,maxfit,cut,asf,ssd,smax,scut)); //file print
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
