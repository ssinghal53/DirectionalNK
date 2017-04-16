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
import java.util.BitSet;
import java.util.Iterator;

/**
 * Class to Write out the simulation trace in a variety of formats
 * @author Sharad Singhal
 */
public class TraceWriter {
	/** trace writer to be used simulation wide */
	private static TraceWriter writer = null;
	/** print stream to be used simulation wide */
	private PrintStream writerStream = null;
	/** Simulation configuration */
	private Configuration config;
	/** format for writing a trace line */
	private String format = null;
	/** Trace header */
	private String header = null;
	/** Trace type */
	private TraceType traceType;
	/**
	 * Create Trace writer
	 */
	private TraceWriter(Configuration config) {
		this.config = config;
		traceType = config.hasOption("trace") ? TraceType.valueOf(config.getOption("trace").toUpperCase()) : TraceType.NONE;
		switch(traceType){
		case TSV:
			format = "%d\t%d\t%d\t%9.6f\t%9.6f\t%9.6f\t%9.6f";
			header = "Generation\tGenome\tCount\tFitness\tCutoff\tshock\tshockFitness";
			break;
		case CSV:
			format = "%d,%d,%d,%9.6f,%9.6f,%9.6f,%9.6f";
			header = "Generation,Genome,Count,Fitness,Cutoff,shock,shockFitness";
			break;
		case NONE:
		default:
			break;
		}
		return;
	}

	/**
	 * Get an Trace Writer
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
			writerStream.println(header);
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
	 * Write out the trace values at the current generation
	 * @param generation - current generation
	 * @param genomes - bitset containing the genomes
	 * @param count - array giving the count values for the genomes
	 * @param fitness - genome fitness on the replication landscape
	 * @param cutoff - replication cutoff value
	 * @param shockFitness - array giving fitness on the shock landscape
	 * @param shock - current shock value
	 */
	public void write(int generation, BitSet genomes, int[] count, float[] fitness, float cutoff, float [] shockFitness, float shock) {
		switch(traceType){
		case TSV:
		case CSV:
			Iterator<Integer> iter = genomes.stream().iterator();
			while(iter.hasNext()){
				int g = iter.next();
				writerStream.println(String.format(format,generation,g,count[g],fitness[g],cutoff,shockFitness[g],shock));	
			}
			break;
		case NONE:
		default:
			break;
		}
	}
}
