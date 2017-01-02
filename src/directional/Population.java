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

/**
 * Interface implemented by population classes
 * @author Sharad Singhal
 */
public interface Population {
	/**
	 * Get the number of unique gene patterns in the population
	 * @return - number of unique gene patterns in the population
	 */
	public int uniqueGenomes();
	
	/**
	 * Get the total number of individuals in the population
	 * @return - total number of individuals
	 */
	public int populationSize();
	
	/**
	 * Get the current shannon diversity in the population
	 * @return - shannon diversity value
	 */
	public double getShannonDiversity();
	
	/**
	 * Get the average fitness of the population
	 * @return - average fitness value; -1 returned if population has no individuals
	 */
	public double getAverageFitness();
	
	/**
	 * Get the standard deviation of the population fitness
	 * @return - standard deviation of the population. -1 if the population has no individuals
	 */
	public double getStandardDeviation();
	
	/**
	 * Advance the population by one generation. The individuals in the population that do not
	 * meet the cutoff are killed, and the remaining individuals produce children by one generation
	 * @return - true if the population has individuals remaining in it, false otherwise
	 */
	public boolean advance();
	
	/**
	 * Give an environmental shock to the population. The individuals in the population that do not
	 * meet the shock threshold are culled. The generation is not advanced
	 * @param shock - value of the shock threshold
	 * @return - true if the population has individuals remaining in it after the shock, false otherwise
	 */
	public boolean shock(float shock);
	
	/**
	 * write the population information to a file
	 */
	public void writePopulation();
	
	/**
	 * read the population information from a file
	 * @return true if successfully read, false otherwise
	 */
	public boolean readPopulation();

	/**
	 * Get the current generation for the population
	 * @return - current generation
	 */
	public int getGeneration();
	
	/**
	 * Initialize any persistent resources needed for this population 
	 * @return - true if successful, false otherwise
	 */
	public boolean open();
	/**
	 * Close any persistent resources used by this population
	 */
	public void close();

}
