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
import java.util.HashMap;
import java.util.Random;

/**
 * Class to hold a Fitness Landscape definition
 * @author Sharad Singhal
 */
public class Landscape {
	/** N for the N,K model */
	private int N;
	/** K for the N,K model */
	private int K;
	/** table [N][K+1] to hold epistasis relationships */
	private int epistasis_locations[][];
	/** fitness values [pow(2,K+1)][N] corresponding to epistasis table */
	private float fitness_table[][];
	/** Map containing landscape peaks */
	private HashMap<Integer,Float> peaks = new HashMap<Integer,Float>();
	/** max peak value in the landscape */
	private float maxPeak = -Float.MAX_VALUE;
	/** min peak value in the landscape */
	private float minPeak = Float.MAX_VALUE;
	/** Number of entries in the fitness table (pow(2,K+1)) */
	private int kMax;
	/** Maximum possible genome values (pow(2,N)) */
	private int maxGenomes;
	/** Landscape file to use, if any */
	private String landscapeFile;
	/** Random number to use, if any */
	private Random random = null;
	/** Seed used to initialize the random number generator */
	private long seed = 0;

	/**
	 * Create a new Landscape
	 * @param N - N value for the landscape
	 * @param K - K value for the landscape
	 * @param e - Epistasis type to use
	 * @param seed - seed for the random number generator. If 0, Math.random() is used
	 * @param landscapeFile - landscape file to use. If null, a random landscape is created. If file is given but does not exist, it is created
	 * @param findPeaks - if true, locate all peaks in the landscape
	 */
	public Landscape(int N, int K, Epistasis e, long seed, String landscapeFile, boolean findPeaks){
		if(N < 1 || N >= Integer.SIZE) throw new RuntimeException("N must be 0 < N < "+Integer.SIZE+" found "+N);
		if(K < 0 || K >= N) throw new RuntimeException("K must be 0 <= K < "+N+" found "+K);
		this.seed = seed;
		this.landscapeFile = landscapeFile;
		this.N = N;
		this.K = K;
		maxGenomes = (int)(Math.pow(2, N));
		kMax = (int)Math.pow(2,K+1);
		if(seed != 0) random = new Random(seed);
		epistasis_locations = new int[N][K+1];
		fitness_table = new float[kMax][N];
		// if we are not given a landscape, or if the landscape file cannot be read, create a random landscape
		if(landscapeFile == null || !readLandscape()){
			// create the epistasis table
			switch(e){
			case ADJACENT:
				// epistasis values contain current gene (i) and K nearest neighbors
				for(int i = 0; i < N; i++){
					for(int j = 0; j < K/2; j++){
						epistasis_locations[i][j] = (i+j-K/2+N) % N;
					}
					for(int j = K/2 ; j <= K; j++){
						epistasis_locations[i][j] = ((i+j-K/2+N) % N);
					}
				}
				break;
			case RANDOM:
				// epistasis values contain current gene (i) and K others chosen at random
				int candidates[] = new int[N-1];
				for(int i = 0; i < N; i++){
					// candidate locations 0 .. N-1 except ii = i, where we put in N-1
					for(int ii = 0; ii < N-1; ii++){
						candidates[ii] = ii;
					}
					epistasis_locations[i][0] = i;
					if(i != N-1) candidates[i] = N-1;
					for(int j = 1; j <= K; j++){
						// we can select values located in candidates[0 ... N-j)
						int ii = random != null ? random.nextInt(N-j) : (int) (Math.random()*(N-j));
						epistasis_locations[i][j] = candidates[ii];
						candidates[ii] = candidates[N-j-1];
					}
				}
				break;
			default:
				throw new RuntimeException("Internal Error. Not implemented Epistasis = "+e);
			}
			// create the fitness table
			for(int i = 0; i < N; i++){
				for(int j = 0; j < kMax; j++){
					fitness_table[j][i] = random != null ? random.nextFloat() : (float) (Math.random()*Float.MAX_VALUE);;
				}
			}
			// locate all peaks in the landscape
			if(findPeaks){
				locatePeaks();
			}
			// if landscape file was defined, but not available, write it out
			if(landscapeFile != null){
				writeLandscape(landscapeFile);
			}
		}
		return;
	}

	/**
	 * Create a landscape from a configuration
	 */
	public Landscape(Configuration config) {
		this(config.getN(),config.getK(),config.getEpistasis(),config.hasOption("s") ? Long.valueOf(config.getOption("s")) : 0L,
				config.getLandscapeFile(),true);
		return;
	}

	/**
	 * Locate all peaks in this landscape.
	 * Note that this method can be expensive in memory and/or time if N or K are large
	 */
	private void locatePeaks() {
		BitSet candidateSet = new BitSet(maxGenomes);
		float[] fitness = new float[maxGenomes];
		// search through all candidates, and mark those that are NOT peaks
		for(int i = 0; i < maxGenomes; i++){
			if(candidateSet.get(i)) continue;	// already tested earlier as not a peak; no need to search further
			float ifit = fitness[i] = getFitness(i);
			int mask = 1;
			// search candidates that are 1 Hamming distance away from this candidate
			// note that there are exactly N such candidates
			for(int j = 0; j < N; j++){
				int neighbor = i ^ mask;
				float neighborFit = fitness[neighbor] > 0 ? fitness[neighbor] : (fitness[neighbor] = getFitness(neighbor));
				if(neighborFit > ifit){
					// at least one neighbor is higher, mark us and continue to the next candidate
					candidateSet.set(i);
					break;
				} else if(neighborFit < ifit && !candidateSet.get(neighbor)){
					// the neighbor is NOT a peak, and was not already marked, mark it
					candidateSet.set(neighbor);
				} 
				mask <<= 1;
			}
			// if(i % 1000 == 0) System.out.println(i);
		}
		// at this  point, all points with value 0 in the candidateSet are peaks
		for(int i = 0; i < maxGenomes; i++){
			if(!candidateSet.get(i)){
				peaks.put(i, fitness[i]);
				if(maxPeak < fitness[i]) maxPeak = fitness[i];
				if(minPeak > fitness[i]) minPeak = fitness[i];
			}
		}
		return;
	}

	/**
	 * Get the value of fitness for a genome for this landscape
	 * @param value - value of genome to be evaluated
	 * @return - fitness value of the genome
	 */
	public float getFitness(int value) {
		float fitness = 0;
		for(int i = 0; i < N; i++){
			// gather the dependencies for each bit location
			int gene = 0;
			for(int j = K; j >=0; j--){
				gene <<= 1;
				if((value & (1L << epistasis_locations[i][j])) > 0){
					gene |= 1;
				}
			}
			// add the fitness value for the bit location
			fitness += fitness_table[gene][i];
		}
		// overall fitness is the average of the N locations
		fitness /= N;
		return fitness;
	}

	/**
	 * Get the value of fitness for a genome for this landscape
	 * @param g - genome to be evaluated
	 * @return - fitness value of the genome
	 */
	public float getFitness(Genome g) {
		return getFitness(g.getValue());
	}

	/**
	 * Get the fitness of the tallest peak in the landscape
	 * @return - fitness of the tallest peak. Returns -Float.MAXVALUE if no peaks are available
	 */
	public float getMaxPeak(){
		return maxPeak;
	}

	/**
	 * get the fitness of the smallest peak in the landscape
	 * @return - fitness of the smallest peak. Returns Float.MAXVALUE if no peaks are available.
	 */
	public float getMinPeak(){
		return minPeak;
	}

	/**
	 * Get the number of peaks in the landscape
	 * @return - number of peaks in the landscape
	 */
	public int getNumberOfPeaks(){
		return peaks.size();
	}
	
	/**
	 * Get all peaks available for this landscape
	 * @return - Map containing genome <genome,peakheight>
	 */
	public HashMap<Integer,Float> getPeaks(){
		return peaks;
	}

	/**
	 * Write out the landscape values to a file
	 */
	public void writeLandscape(String fileName){
		try {
			PrintStream out = new PrintStream(new File(fileName));
			out.println(Configuration.banner);
			out.println("# File: "+fileName+" created "+ZonedDateTime.now().toString());
			if(seed == 0) out.println(String.format("# N = %d, K = %d",N,K));
			else out.println(String.format("# N = %d, K = %d, Seed = %d",N,K,seed));
			out.println("# Epistasis Locations[N][K+1]:");
			for(int i = 0; i < N; i++){
				for(int j = 0; j <=K; j++){
					out.print(String.format(" %d", epistasis_locations[i][j]));
				}
				out.print("\n");
			}
			out.println("# Fitness Table[pow(2,K+1)][N]:");
			for(int i=0; i < kMax; i++){
				for(int j = 0; j < N; j++){
					out.print(String.format("%f ",fitness_table[i][j]));
				}
				out.print("\n");
			}
			if(peaks.size() > 0){
				out.println("# Fitness Peaks "+peaks.size());
				for(Integer key : peaks.keySet()){
					out.println(String.format("%d %f", key,peaks.get(key)));
				}
			}
			out.close();
		} catch (FileNotFoundException e) {
			System.out.println("Unable to open file "+fileName);
		}
	}

	/**
	 * Read the population values from a file
	 * @return - true if successful, false if read failed
	 */
	public boolean readLandscape() {
		if(landscapeFile != null){
			try {
				BufferedReader inp = new BufferedReader(new InputStreamReader(new FileInputStream(new File(landscapeFile))));
				int i = 0;
				// read in the epistasis locations
				while(i < N){
					String line = inp.readLine();
					if(line == null){
						inp.close();
						throw new IOException("Unable to read "+landscapeFile);
					}
					line = line.trim();
					if(line.isEmpty() || line.startsWith("#")) continue;
					String [] parts = line.split(" ");
					if(parts.length != (K+1)){
						inp.close();
						throw new IOException("Illegal Landscape file "+landscapeFile+" Expected "+(K+1)+" values found "+parts.length);
					}
					for(int j = 0; j < K+1; j++) epistasis_locations[i][j] = Integer.parseInt(parts[j]);
					i++;
				}
				// read in the fitness values
				i = 0;
				while(i < kMax){
					String line = inp.readLine();
					if(line == null){
						inp.close();
						throw new IOException("Unable to read fitness values from "+landscapeFile);
					}
					line = line.trim();
					if(line.isEmpty() || line.startsWith("#")) continue;
					String [] parts = line.split(" ");
					if(parts.length != N){
						inp.close();
						throw new IOException("Unable to read "+landscapeFile);
					}
					for(int j = 0; j < N; j++){
						fitness_table[i][j] = Float.parseFloat(parts[j]);
					}
					i++;
				}
				// read in the fitness peaks
				while(true){
					String line = inp.readLine();
					if(line == null) break;
					line = line.trim();
					if(line.isEmpty() || line.startsWith("#")) continue;
					String [] parts = line.split(" ");
					if(parts.length < 2){
						inp.close();
						throw new IOException("Illegal Landscape file "+landscapeFile+" Expected 2 values found "+parts.length);
					}
					Integer g = Integer.valueOf(parts[0]);
					Float f = Float.valueOf(parts[1]);
					if(maxPeak < f) maxPeak = f;
					if(minPeak > f) minPeak = f;
					peaks.put(g, f);
				}
				inp.close();
				return true;
			} catch (IOException e) {
				System.out.println(e.toString());
			}
		}
		return false;
	}

	/**
	 * Main program to (re) generate a landscape file
	 * @param args
	 * -n N - value of N in the N,K model<br>
	 * -k K - value of K in the N,K model<br>
	 * -L fileName - name of the landscape file<br>
	 * -s seed - random number generator seed<br>
	 * -e [random | adjacent] - epistasis type<br>
	 */
	public static void main(String[] args) {
		// show help and exit if program called with -h
		if(args.length == 0 || args[0].startsWith("-h")) {
			System.out.println("Use Landscape -N N -K K -L landscapeFile [-s seed] [-e [adjacent | random]] [-f [true|false]");
			return;
		}
		// program defaults
		int N = 10;
		int K = 4;
		long seed = 0;
		Epistasis e = Epistasis.ADJACENT;
		String landscapeFile = "landscape.txt";
		boolean findPeaks = true;
		// now process arguments given
		for(int i = 0; i < args.length; i += 2){
			switch(args[i].toLowerCase()){
			case "-n":
				N = Integer.valueOf(args[i+1]);
				break;
			case "-k":
				K = Integer.valueOf(args[i+1]);
				break;
			case "-l":
				landscapeFile = args[i+1];
				break;
			case "-e":
				e = Epistasis.valueOf(args[i+1].toUpperCase());
				break;
			case "-s":
				seed = Long.valueOf(args[i+1]);
				break;
			case "-f":
				findPeaks = Boolean.valueOf(args[i+1]);
				break;
			default:
				System.out.println("Skipped unknown option "+args[i]+" "+args[i+1]);
				break;
			}
		}
		System.out.println(Configuration.banner);
		System.out.println(Configuration.copyright);
		System.out.println("N = "+N+ " K = "+K);
		long time = System.currentTimeMillis();
		@SuppressWarnings("unused")
		Landscape landscape = new Landscape(N,K,e,seed,landscapeFile,findPeaks);
		time = System.currentTimeMillis()-time;
		System.out.println("Done "+landscapeFile+" in "+time+" ms");
		return;
	}
}
