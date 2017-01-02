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

import java.util.Set;
import java.util.Vector;

/**
 * @author Sharad Singhal
 * Interface implemented by Genome classes
 */
public interface Genome {
	/**
	 * Mutate this genome
	 * @return - the child genome mutated from this genome
	 */
	public Genome mutate();
	/**
	 * Get the fitness for this genome
	 * @return - fitness value based on landscape when this genome was created
	 */
	public float getFitness();
	/**
	 * Get the (gene) value of this genome
	 * @return - value of the genome
	 */
	public int getValue();
	/**
	 * get the parent for this genome
	 * @return - parent of this genome
	 */
	public Genome getParent();
	
	/**
	 * Get the ancestors of this genome, ordered by generation
	 * @return list containing parent, parent of parent, ...
	 */
	public Vector<Genome> getAncestors();
	
	/**
	 * Get the immediate children of this Genome
	 * @return - immediate children of this genome
	 */
	public Set<Genome> getChildren();
	
	/**
	 * Get all descendants of this genome
	 * @return - all descendants of this genome
	 */
	public Set<Genome> getDescendants();
	/**
	 * Get the generation when this genome was created
	 * @return - birth generation for this genome
	 */
	public int getBirthGeneration();
	
	/**
	 * Get the generation when this genome was killed
	 * @return - death generation for this genome. -1 for active genomes in the population
	 */
	public int getDeathGeneration();
	
	/**
	 * Set the death generation for this genome
	 * @param deathGeneration - death generation
	 */
	public void setDeathGeneration(int deathGeneration);
}
