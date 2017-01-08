/**
 * Copyright (C) 2016, Sonia Singhal
 * 
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
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Configuration for the Nk Model Simulation
 * @author Sharad Singhal
 */
public class Configuration {
	/** Program version */
	public static final String version = "0.1.0";
	/** Program creation date */
	public static final String creationDate = "2017-01-07";
	public static final String banner = "# N-K Model Simulation Version "+Configuration.version+" dated "+Configuration.creationDate;
	public static final String copyright = "# Copyright (C) 2016  Sonia Singhal"+
    "\n# This program comes with ABSOLUTELY NO WARRANTY."+
    "\n# This is free software, and you are welcome to redistribute it"+
	"\n# under certain conditions; See accompanying license for details.";
	// define default values if not given
	/** debug level */
	private int debugLevel = 0;
	/** Number of genes in genome <= 30 */
	private int N = 10;
	/** Number of epistasis locations */
	private int K = 5;
	/** Initial population size in simulation */
	private int initial_population = 10;
	/** Maximum number of generations to simulate */
	private int max_generations = 10;
	/** Mutation strategy when replicating */
	private MutationStrategy mutationStrategy = MutationStrategy.SINGLE_RANDOM;
	/** Epistasis type */
	private Epistasis epistasis = Epistasis.ADJACENT;
	/** Seed for the random number generator */
	private long seed = 32767;
	/** seed for the random number generator for shocks */
	private long sseed = 79;
	/** Maximum possible genome values */
	private int maxValue = (int) (Math.pow(2, N));
	/** postfix to use on output files */
	private String outputFile = "out.txt";
	/** cutoff values */
	private Vector<Float> cutoffs = new Vector<Float>();
	/** shock values */
	private Vector<Float> shocks = new Vector<Float>();
	/** Landscape definition */
	private Landscape landscape,shockLandscape;
	/** simulation type */
	private SimulationType type = SimulationType.FAST;
	/** Mutation probability for mutating a gene in MULTI_RANDOM strategy */
	private float mutation_probability = (float) 0.1;
	/** file to read in initial population */
	private String populationFile = null;
	/** file to read in initial landscape definition */
	private String landscapeFile = null;
	/** Random number generator to use */
	private Random random;
	/** show progress every progress generations on screen */
	private int progress = 10;
	/** minimum fitness threshold for replication */
	private float minFit = 0.0F;
	/** maximum fitness threshold for replication */
	private float maxFit = 1.0F;
	
	/** Options from the command line or options file */
	private HashMap<String,String> options = new HashMap<String,String>();
	
	/**
	 * create a configuration based on given command-line options. The program reads in
	 * the command line options. Next, if "-i file" is given, options are read from the
	 * file. Options given on the command line override any options in the options file.
	 * @param args - command line options
	 */
	public Configuration(String [] args) {
		// read in options from the command line
		List<String> files = new Vector<String>();	// currently unused, but needed by getArgs
		getArgs(args,options,files);
		
		// if -i inputFile given on the command line, read in additional options from the input file
		if(options.containsKey("i")) readConfigurationFromFile(options,files);
		
		// initialize the random number generators, seeded with the -s/sseed options, if given
		if(options.containsKey("s")){
			seed = Long.parseLong(options.get("s"));
			random = new Random(seed);
		} else {
			seed = (long)(Math.random()*Long.MAX_VALUE);
			random = new Random(seed);
			options.put("s", Long.toString(seed));
		}
		if(options.containsKey("sseed")) sseed = Long.parseLong(options.get("sseed"));
			
		// now handle the remaining options
		if(options.containsKey("d")) debugLevel = Integer.parseInt(options.get("d"));	
		// note that we are currently limiting the simulation to N < 32
		if(options.containsKey("n")){
			N = Integer.parseInt(options.get("n"));
			if(N >= Integer.SIZE) throw new RuntimeException("N must be < "+Integer.SIZE+" found "+N);
			maxValue = (int)(Math.pow(2, N));
		}
		if(options.containsKey("k")) K = Integer.parseInt(options.get("k"));
		if(K >= N) throw new RuntimeException("K must be < "+N+" found "+K);
		if(options.containsKey("f")) populationFile = options.get("f");
		if(options.containsKey("t")) type = SimulationType.valueOf(options.get("t").toUpperCase());
		if(options.containsKey("l")) landscapeFile = options.get("l");
		if(options.containsKey("p")) initial_population = Integer.parseInt(options.get("p"));
		if(options.containsKey("g")) max_generations = Integer.parseInt(options.get("g"));
		if(options.containsKey("v")) progress = Integer.parseInt(options.get("v"));
		if(options.containsKey("e")) epistasis = Epistasis.valueOf(options.get("e").toUpperCase());
		if(options.containsKey("m")) mutationStrategy = MutationStrategy.valueOf(options.get("m").toUpperCase());
		if(options.containsKey("o")) outputFile = options.get("o");
		if(options.containsKey("r")) mutation_probability = Float.parseFloat(options.get("r"));
		if(mutation_probability <= 0. || mutation_probability >= 1.){
			throw new RuntimeException("Mutation probability must be 0 < r < 1; found "+mutation_probability);
		}
		if(options.containsKey("minfit")) minFit = Float.parseFloat(options.get("minfit"));
		if(options.containsKey("maxfit")) maxFit = Float.parseFloat(options.get("maxfit"));
		if(options.containsKey("c")){
			String value[] = options.get("c").split(" ");
			for(int i = 0; i < value.length; i ++){
				cutoffs.add(Float.valueOf(value[i].trim()));
			}
		}
		
		// generate the evolution landscape
		shockLandscape = landscape = new Landscape(this);
		// ensure that we have a default cutoff value
		if(cutoffs.isEmpty()) cutoffs.add((float) 0.5);
		
		// get the shock generations and values
		if(options.containsKey("shocks")){
			String value[] = options.get("shocks").split(" ");
			for(int i = 0; i < value.length; i++){
				shocks.add(Float.valueOf(value[i].trim()));
			}
		}
		
		// generate the shock landscape
		if(!shocks.isEmpty()){
			if(options.containsKey("a")){
				// if a file is given, use it. If it is generated, it will have peaks computed
				shockLandscape = new Landscape(getN(),getK(),getEpistasis(),sseed,options.get("a"),true);
			} else {
				// create a random landscape for the shocks. Don't bother computing peaks
				shockLandscape = new Landscape(getN(),getK(),getEpistasis(),sseed,null,false);
			}
		}
		// if debug, print out the options
		if(debugLevel > 2){
			for(String key : options.keySet()){
				System.out.println(key+" : "+options.get(key));
			}
		}
		// write out the configuration
		writeConfiguration(options);
		return;
	}
	
	/**
	 * Show help for the program
	 */
	public static void showHelp(){
		System.out.println("Use: java Configuration [optionName optionValue]...");
		System.out.println("where optionName is (default in [])");
		System.out.println("\t-c {initCut [nextGen nextCut] ... }  : cutoff values [{0.5}]");
		System.out.println("\t-d level  : debug level [0]");
		System.out.println("\t-e value  : epistatis strategy {adjacent|random} [adjacent]");
		System.out.println("\t-f file   : read initial population from file instead of generating a random population [null]");
		System.out.println("\t-g value  : maximum generations [10]");
		System.out.println("\t-h        : print help (this message). Must be first option");
		System.out.println("\t-i name   : input file name [null]");
		System.out.println("\t-k value  : K value for (N,K) model [5]");
		System.out.println("\t-l file   : read landscape from file instead of generating a random landscape [null]");
		System.out.println("\t-n value  : N value for (N,K) model [10]");
		System.out.println("\t-m value  : mutation strategy {replicate|single_random|mult_random} [single_random]");
		System.out.println("\t-o name   : output file name [out.txt]");
		System.out.println("\t-p value  : initial population size [10]");
		System.out.println("\t-r value  : probability of a bit switch in multi-random strategy [0.1]");
		System.out.println("\t-s value  : starting random number seed [32767]");
		System.out.println("\t-t value  : simulation type {detailed|fast} [fast]");
		System.out.println("\t-v value  : show simulation progress every value steps [10]");
		System.out.println("\t-trace value  : write out a trace of the simulation {TSV|CSV|NONE} [NONE]");
		System.out.println("\t-a file   : use landscape from file instead of generating a random landscape for shocks [null]");
		System.out.println("\t-shocks {[Gen shock] ... } value : Do a shock selection after given generations [null]" );
		System.out.println("\t-sseed value : random number generator seed for shocks [79]" );
		System.out.println("\t-minfit value : minimum fitness for replication [0.0]" );
		System.out.println("\t-maxfit value : maximum fitness for replication [1.0]" );
		return;
	}
	
	/**
	 * Get a hashmap containing {name, value} pairs passed as an argument list.
	 * Each {name, value} pair is represented as [-name value] ... in the argument list. In
	 * case the value contains spaces, it can be enclosed in braces.
	 * The returned Hashmap is keyed by name (sans the '-') and contains the corresponding
	 * value. Note that names are converted to lower case, so are case insensitive
	 * @param argv - string array to be parsed
	 * @param options - options returned in the map
	 * @param files - any additional options not preceded by a -name
	 */
	private static void getArgs(String [] argv, Map<String,String> options, List<String> files){
		for(int i=0; i<argv.length; i++ ){
			String token = argv[i];
			if(!token.startsWith("-")){
				files.add(token);
				continue;
			} else if(i < argv.length-1){
				token = token.substring(1); // strip the - sign in front
				String value = argv[++i];	// get the value, and move forward
				if(value.startsWith("{")){	// have a quoted value, collect it
					StringBuilder b = new StringBuilder(value);
					if(!value.endsWith("}")){
						while(i < argv.length-1){
							b.append(" ");
							b.append(argv[++i]);
							if(argv[i].endsWith("}")) break;
						}
					}
					value = b.substring(1,b.length()-1).trim();	// strip braces and extra whitespace from value
				}
				options.put(token.toLowerCase(), value);
			}
		}
		return;
	}
	
	/**
	 * Read in a configuration file and set options from it. Comment lines are indicated by a '# as the first character
	 * and skipped.
	 * @param options - options to set, if not already present in the map
	 * @param files - additional scanned options not preceded by -option flag
	 */
	private void readConfigurationFromFile(Map<String,String> options, List<String> files) {
		String fileName = options.get("i");
		File fd = new File(fileName);
		if(!fd.exists()){
			System.out.println("Input File "+fileName+" does not exist");
			System.exit(1);
		}
		try {
			BufferedReader inp = new BufferedReader(new InputStreamReader(new FileInputStream(fd)));
			String line;
			outer:	while((line = inp.readLine()) != null){
				StringTokenizer tokenizer = new StringTokenizer(line);
				while(tokenizer.hasMoreTokens()){
					String token = tokenizer.nextToken();
					if(token.startsWith("#")) continue outer;	// reached a comment value; read next line
					if(!token.startsWith("-")){
						files.add(token);
						continue;
					} else if(tokenizer.hasMoreTokens()){
						token = token.substring(1).toLowerCase(); 		// strip the - sign in front and convert to lower case
						String value = tokenizer.nextToken();			// get the value, and move forward
						if(value.startsWith("{")){						// have a quoted value
							StringBuilder b = new StringBuilder(value);
							if(!value.endsWith("}")){
								while(tokenizer.hasMoreTokens()){
									b.append(" ");
									value = tokenizer.nextToken();
									b.append(value);
									if(value.endsWith("}")) break;
								}
							}
							value = b.substring(1,b.length()-1).trim();	// strip braces and extra whitespace from value
						}
						// set option only if not already given
						if(!options.containsKey(token)) options.put(token, value);	 
					}
				}
			}
			inp.close();
		} catch (IOException e) {
			System.out.println(e.toString());
			System.exit(1);
		}
	}
	/**
	 * Write out the current options value to an output file
	 * @param options - current command line/input options
	 */
	private void writeConfiguration(Map<String,String>options){
		String fileName = getFileName("conf-");
		try {
			PrintStream out = new PrintStream(new File(fileName));
			out.println(banner);
			out.println("# created "+ZonedDateTime.now().toString());
			out.println("# by Configuration.java");
			for(String key : options.keySet()){
				if(key.equals("c") || key.equals("shocks")){
					out.println("-"+key+"\t{"+options.get(key)+"}");
				} else {
					out.println("-"+key+"\t"+options.get(key));
				}
			}
			out.close();
		} catch (FileNotFoundException e) {
			System.out.println("Unable to open file "+fileName);
		}
		return;
	}
	
	/**
	 * Get an option value from the configuration
	 * @param optionName - name of the option
	 * @return - value of the option. Null if no option with this name was defined
	 */
	public String getOption(String optionName){
		return options.get(optionName);
	}
	
	/**
	 * Check if a particular option is present in the options
	 * @param optionName - name of the option to test
	 * @return - true if the option exists, false otherwise
	 */
	public boolean hasOption(String optionName){
		return options.containsKey(optionName);
	}
	
	/* 
	 * *********************************************
	 *  Convenience methods to get specific options
	 * *********************************************
	 */
	
	/**
	 * Get the type of simulation to run
	 * @return - type of simulation
	 */
	public SimulationType getType(){
		return type;
	}

	/**
	 * Get the epistasis strategy to use
	 * @return - epistasis strategy
	 */
	public Epistasis getEpistasis() {
		return epistasis;
	}
	
	/**
	 * Get the mutation strategy being used
	 * @return - mutation strategy in use
	 */
	public MutationStrategy getMutationStrategy() {
		return mutationStrategy;
	}

	/**
	 * Get the genome size N
	 * @return - size of the genome
	 */
	public int getN() {
		return N;
	}

	/**
	 * Get the Epistasis length
	 * @return - Epistatis length K
	 */
	public int getK() {
		return K;
	}

	/**
	 * Get the maximum possible for a genomes
	 * @return - maximum number of genomes possible
	 */
	public int getMaxGenomes() {
		return maxValue;
	}

	/**
	 * Get the current landscape
	 * @return - current landscape
	 */
	public Landscape getLandscape(){
		return landscape;
	}
	
	/**
	 * Get the landscape to be used to give shocks to the population
	 * @return - landscape for giving shocks
	 */
	public Landscape getShockLandscape(){
		return shockLandscape;
	}
	
	/**
	 * Get the maximum generations to run
	 * @return - maximum generations needed
	 */
	public int getMaxGenerations() {
		return max_generations;
	}
	
	/**
	 * Get the initial population size
	 * @return - initial population
	 */
	public int getInitialPopulationSize(){
		return initial_population;
	}
	
	/**
	 * Get the mutation probability for a bit-flip in multi-random strategy
	 * @return - mutation probability
	 */
	public float getMutationProbability(){
		return mutation_probability;
	}
	
	/**
	 * Get the initial population file
	 * @return - name of population file, null if none defined
	 */
	public String getPopulationFile(){
		return populationFile;
	}
	
	/**
	 * Get the initial landscape file
	 * @return - name of landscape file, null if none defined
	 */
	public String getLandscapeFile(){
		return landscapeFile;
	}
	
	/**
	 * Get the debug level for this configuration
	 * @return - debug level
	 */
	public int debugLevel(){
		return debugLevel;
	}
	
	/**
	 * Get the value of progress indicator in trace
	 * @return - value of progress indicator
	 */
	public int progressIndicator(){
		return progress;
	}
	
	/* 
	 * *********************************************
	 *  Helper methods for the simulation
	 * *********************************************
	 */
	
	/**
	 * Get a random integer in the range [0,bound)
	 * @param bound - bound for the random integer
	 * @return - random integer with given bound
	 */
	public int randomInt(int bound){
		return random.nextInt(bound);
	}
	
	/**
	 * get the (randomized) value of a genome
	 * @return - value of the genome
	 */
	public int getRandomGeneValue() {
		return random.nextInt(maxValue);
	}
	
	/**
	 * Get a random value
	 * @return - next random value
	 */
	public float randomFloat(){
		return random.nextFloat();
	}
	
	/**
	 * Get an output filename with the prefix prepended to it
	 * @param prefix - prefix to use
	 * @return - name of the file
	 */
	public String getFileName(String prefix){
		String fileName = outputFile == null ? "out.txt" : outputFile;
		int index = fileName.lastIndexOf("/");
		if(index > 0){
			File outputDirectory = new File(fileName.substring(0,index));
			if(!outputDirectory.exists()) outputDirectory.mkdirs();
		}
		return fileName.substring(0, index+1)+prefix+fileName.substring(index+1);
	}
	
	/**
	 * Get the cutoff at a given generation
	 * @param generation - desired generation
	 * @return - value of fitness cutoff
	 */
	public float getCutoff(int generation){
		float cutoff = cutoffs.get(0);
		for(int i = 1; i < cutoffs.size()-1; i += 2){
			int nextCutoffGeneration = cutoffs.get(i).intValue();
			if(nextCutoffGeneration < generation){
				cutoff = cutoffs.get(i+1);
				continue;
			}
			break;
		}
		if(cutoff > landscape.getMaxPeak()){
			System.out.println("*Warning* - Cutoff "+cutoff+" at generation "+generation+" > maximum peak ("+landscape.getMaxPeak()+") in the landscape");
		}
		return cutoff;
	}
	
	/**
	 * Get the shock value after given generation
	 * @param generation - desired generation
	 * @return - value of shock cutoff. -ve number returned if no shock needed after this generation
	 */
	public float getShock(int generation){
		if(shocks.isEmpty()) return -1;
		float shock = -Float.MAX_VALUE;
		for(int i = 0; i < shocks.size()-1; i += 2){
			int nextShockGeneration = shocks.get(i).intValue();
			if(nextShockGeneration == generation){
				shock = shocks.get(i+1);
				break;
			}
		}
		if(shock > shockLandscape.getMaxPeak()){
			System.out.println("*Warning* - Shock  "+shock+" at generation "+generation+" > maximum peak ("+shockLandscape.getMaxPeak()+") in the shock landscape");
		}
		return shock;
	}
	/**
	 * Get the replication probability at a given fitness value
	 * @param fitness - fitness value for parent
	 * @return - probability [0,1] that the parent is able to replicate
	 */
	public double getReplicationProbability(float fitness){
		return Math.abs(maxFit-minFit) > .01 ?  Math.min(1.0F, Math.max(0.0F, (fitness-minFit)/(maxFit-minFit))) : fitness > minFit ? 1.0 : 0.0;
	}

	/**
	 * Get the maximum sustainable population.
	 * @return maximum population. 0 returned if not defined
	 */
	public int getMaxPopulation() {
		return options.containsKey("mp") ? Integer.valueOf(options.get("mp")) : 0;
	}

	public double getAlpha() {
		return options.containsKey("alpha") ? Double.valueOf(options.get("alpha")) : 0;
	}
}
