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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.BitSet;
import java.util.Iterator;

/**
 * Class to Write out the simulation trace in a variety of formats
 * @author Sharad Singhal
 */
public class TraceWriter {
	private static TraceWriter writer = null;
	private PrintStream writerStream = null;
	private Configuration config;
	private String format = null;
	private TraceType traceType;
	/**
	 * Create Trace writer
	 */
	private TraceWriter(Configuration config) {
		this.config = config;
		traceType = config.hasOption("trace") ? TraceType.valueOf(config.getOption("trace").toUpperCase()) : TraceType.NONE;
		switch(traceType){
		case ODV:
			StringBuilder b = new StringBuilder();
			b.append(config.hasOption("cruise") ? config.getOption("cruise") : "DEV");
			b.append("\t");
			b.append(config.hasOption("station") ? config.getOption("station") : "DEV0");
			b.append("\t");
			b.append(config.hasOption("type") ? config.getOption("type") : "C");
			b.append("\t");
			b.append(ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
			b.append("\t");
			b.append(config.hasOption("latitude") ? config.getOption("latitude") : "0.0");
			b.append("\t");
			b.append(config.hasOption("longitude") ? config.getOption("longitude") : "0.0");
			b.append("\t");
			b.append("%d\t%d\t%d\t%9.6f\t%9.6f");
			format = b.toString();
			break;
		case TSV:
			format = "%d\t%d\t%d\t%9.6f\t%9.6f\t\t";
			break;
		case CSV:
			format = "%d,%d,%d,%9.6f,%9.6f,,";
			break;
		case NONE:
		default:
			break;
		}
		return;
	}

	/**
	 * Get an ODV Writer
	 * @param config - Simulation configuration
	 * @return - an ODV Spreadsheet writer if defined in the configuration, else null
	 */
	public static TraceWriter getWriter(Configuration config){
		return writer != null ? writer : (writer = new TraceWriter(config));
	}

	/**
	 * Open the trace file for writing
	 * @return - true on success, false otherwise
	 */
	public boolean open(){
		if(writerStream == null){
			String traceFile = null;
			switch(traceType){
			case ODV:
				traceFile = config.getFileName("odv-");
				break;
			case TSV:
				traceFile = config.getFileName("tsv-");
				break;
			case CSV:
				traceFile = config.getFileName("csv-");
				break;
			case NONE:
			default:
				return true;
			}
			try {
				writerStream = new PrintStream(new File(traceFile));
			} catch (FileNotFoundException e) {
				System.out.println(e.toString());
				writerStream.close();
				writerStream = null;
				return false;
			}
			writeHeader();
			return true;
		}
		return false;
	}
	
	/**
	 * Close the traceWriter, and free resources
	 */
	public void close(){
		if(writerStream != null){
			writerStream.close();
			writerStream = null;
		}
		return;
	}

	/**
	 * Write the current state of the simulation in the trace file
	 * @param generation - current simulation generation
	 * @param genomes - Bitset containing currently active genomes
	 * @param count	- count of each genome in the population
	 * @param fitness - fitness for each genome
	 * @param cutoff - cutoff for this generation
	 */
	public void write(int generation, BitSet genomes, int count[], float fitness [],float cutoff){
		switch(traceType){
		case ODV:
			Iterator<Integer> iter = genomes.stream().iterator();
			while(iter.hasNext()){
				int g = iter.next();
				writerStream.println(String.format(format,generation,g,count[g],fitness[g],cutoff));	
			}
			break;
		case TSV:
		case CSV:
			iter = genomes.stream().iterator();
			while(iter.hasNext()){
				int g = iter.next();
				writerStream.println(String.format(format,generation,g,count[g],fitness[g],cutoff));	
			}
			break;
		case NONE:
		default:
			break;
		}
		return;
	}

	/**
	 * Write the header for the trace
	 */
	private void writeHeader(){
		switch(traceType){
		case ODV:
			writerStream.println("//<Encoding>UTF-8</Encoding>\n//<Version>ODV Spreadsheet V4.6.4</Version>");
			if(config.hasOption("author")) writerStream.println("//<Creator>"+config.getOption("author")+"</Creator>");
			writerStream.println("//<CreateTime>"+ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)+"</CreateTime>");
			writerStream.println("//<Software>"+Configuration.banner.substring(2)+"</Software>");
			if(config.hasOption("i")) writerStream.println("//<Source>"+config.getOption("i")+"</Source>");
			writerStream.println("//");
			writerStream.println("//<MetaVariable>label=\"Cruise\" var_type=\"METACRUISE\" value_type=\"TEXT:21\" qf_schema=\"ODV\" significant_digits=\"0\" comment=\"\"</MetaVariable>");
			writerStream.println("//<MetaVariable>label=\"Station\" var_type=\"METASTATION\" value_type=\"TEXT:21\" qf_schema=\"ODV\" significant_digits=\"0\" comment=\"\"</MetaVariable>");
			writerStream.println("//<MetaVariable>label=\"Type\" var_type=\"METATYPE\" value_type=\"TEXT:2\" qf_schema=\"ODV\" significant_digits=\"0\" comment=\"\"</MetaVariable>");
			writerStream.println("//<MetaVariable>label=\"Longitude [degrees_east]\" var_type=\"METALONGITUDE\" value_type=\"FLOAT\" qf_schema=\"ODV\" significant_digits=\"3\" comment=\"\"</MetaVariable>");
			writerStream.println("//<MetaVariable>label=\"Latitude [degrees_north]\" var_type=\"METALATITUDE\" value_type=\"FLOAT\" qf_schema=\"ODV\" significant_digits=\"3\" comment=\"\"</MetaVariable>");
			writerStream.println("//<MetaVariable>label=\"Bot. Depth [m]\" var_type=\"METABOTDEPTH\" value_type=\"FLOAT\" qf_schema=\"ODV\" significant_digits=\"0\" comment=\"\"</MetaVariable>");
			writerStream.println("//");	
			writerStream.println("//<DataVariable>label=\"Genome [i]\" value_type=\"INTEGER\" qf_schema=\"ODV\" is_primary_variable=\"T\" comment=\"\"</DataVariable>");
			writerStream.println("//<DataVariable>label=\"Generation [i]\" value_type=\"INTEGER\" qf_schema=\"ODV\" is_primary_variable=\"F\" comment=\"\"</DataVariable>");
			writerStream.println("//<DataVariable>label=\"Count [i]\" value_type=\"INTEGER\" qf_schema=\"ODV\" is_primary_variable=\"F\" comment=\"\"</DataVariable>");
			writerStream.println("//<DataVariable>label=\"cutoff [f]\" value_type=\"FLOAT\" qf_schema=\"ODV\" significant_digits=\"6\" is_primary_variable=\"F\" comment=\"\"</DataVariable>");
			writerStream.println("//<DataVariable>label=\"Fitness [f]\" value_type=\"FLOAT\" qf_schema=\"ODV\" significant_digits=\"6\" is_primary_variable=\"F\" comment=\"\"</DataVariable>");
			writerStream.println("//");
			writerStream.println("Cruise\tStation\tType\tyyyy-mm-ddThh:mm:ss.sss\tLongitude [degrees_east]\tLatitude [degrees_north]\tGeneration [i]\tGenome [i]\tCount [i]\tFitness [f]\tCutoff [f]");
			break;
		case TSV:
			writerStream.println("Generation\tGenome\tCount\tFitness\tCutoff\tshock\tshockFitness");
			break;
		case CSV:
			writerStream.println("Generation,Genome,Count,Fitness,Cutoff,shock,shockFitness");
			break;
		case NONE:
		default:
			break;
		}
		return;
	}

	/**
	 * @param generation - generation after which a shock is being given
	 * @param genomes - genomes surviving the shock
	 * @param count - count of genomes
	 * @param fitness - fitness of the genomes on the normal landscape
	 * @param cutoff - cutoff for this generation, if any
	 * @param shock - value of the shock given, if any
	 */
	public void write(int generation, BitSet genomes, int[] count, float[] fitness, float cutoff, float shock) {
		Landscape shockLandscape = config.getShockLandscape();
		switch(traceType){
		case ODV:
			Iterator<Integer> iter = genomes.stream().iterator();
			while(iter.hasNext()){
				int g = iter.next();
				writerStream.println(String.format(format,generation,g,count[g],fitness[g],cutoff));	
			}
			break;
		case TSV:
			iter = genomes.stream().iterator();
			while(iter.hasNext()){
				int g = iter.next();
				writerStream.println(String.format("%d\t%d\t%d\t%9.6f\t%9.6f\t%9.6f\t%9.6f",generation,g,count[g],fitness[g],cutoff,shock,shockLandscape.getFitness(g)));	
			}
			break;
		case CSV:
			iter = genomes.stream().iterator();
			while(iter.hasNext()){
				int g = iter.next();
				writerStream.println(String.format("%d,%d,%d,%9.6f,%9.6f,%9.6f,%9.6f",generation,g,count[g],fitness[g],cutoff,shock,shockLandscape.getFitness(g)));	
			}
			break;
		case NONE:
		default:
			break;
		}
	}
}
