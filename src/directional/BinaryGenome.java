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

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

/**
 * A Genome that implements only [0,1] valued genes
 * @author Sharad Singhal
 */
public class BinaryGenome implements Genome, Comparable<Genome> {
	private Configuration config;
	/** encoded value of this Genome */
	private int value;
	/** Generation when this Genome was created */
	private int birthGeneration;
	/** Generation when this Genome was killed */
	private int deathGeneration;
	/** Fitness value of this Genome based on the landscape */
	private float fitness;
	/** Parent from which this Genome was created. Null for an initial Genome */
	// private BinaryGenome parent;
	/** Children spawned from this Genome */
	// private LinkedHashSet<Genome> children = new LinkedHashSet<Genome>();
	
	/**
	 * Create a random Genome with only binary [0,1] values based on a landscape
	 * @param config - configuration to use for this Genome
	 */
	public BinaryGenome(Configuration config) {
		this.config = config;
		Landscape landscape = config.getLandscape();
		value = config.getRandomGeneValue();
		birthGeneration = 0;
		deathGeneration = -1;
		fitness = landscape.getFitness(this);
		// parent = null;
		return;
	}
	
	/**
	 * Create a genome with given value and fitness
	 * @param g - value of the genome
	 * @param fitness - fitness for the genome
	 */
	public BinaryGenome(Configuration config, int g, float fitness) {
		this.config = config;
		this.value = g;
		this.fitness = fitness;
		birthGeneration = 0;
		deathGeneration = -1;
		return;
	}

	/**
	 * Create a Genome by mutating its parent
	 * @param parent - parent being mutated to construct this genome
	 * @see #mutate()
	 */
	private BinaryGenome(BinaryGenome parent){
		this.config = parent.config;
		deathGeneration = -1;
		birthGeneration = parent.birthGeneration+1;
		Landscape landscape = config.getLandscape();
		int N = config.getN();
		// this.parent = parent;
		switch(config.getMutationStrategy()){
		case REPLICATE:
			value = parent.value;
			fitness = parent.fitness;
			break;
		case SINGLE_RANDOM:
			int mask = 1 << config.randomInt(N);	// mask has a single random bit[0:N) = 1
			value = parent.value ^ mask;			// mutate that bit in the parent to generate this genome
			fitness = landscape.getFitness(this);	// get the fitness value for this genome
			break;
		case MULTI_RANDOM:
			float p = config.getMutationProbability();
			mask = 1;
			value = parent.value;
			for(int i = 0; i < N; i++){
				if(config.randomFloat() < p) value ^= mask;
				mask <<= 1;
			}
			break;
		default:
			throw new RuntimeException("BinaryGenome: "+config.getMutationStrategy()+" not yet implemented");
		}
		return;
	}
	
	/* (non-Javadoc)
	 * @see jnk.Genome#getFitness()
	 */
	@Override
	public float getFitness() {
		return fitness;
	}
	
	/* (non-Javadoc)
	 * @see jnk.Genome#getValue()
	 */
	@Override
	public int getValue() {
		return value;
	}
	
	/* (non-Javadoc)
	 * @see jnk.Genome#getGeneration()
	 */
	@Override
	public int getBirthGeneration() {
		return birthGeneration;
	}
	
	/*
	 * (non-Javadoc)
	 * @see jnk.Genome#getParent()
	 */
	public Genome getParent(){
		// return parent;
		return null;
	}
	
	public Set<Genome> getChildren(){
		// return children;
		return null;
	}

	/* (non-Javadoc)
	 * @see jnk.Genome#mutate(jnk.Genome.MutationStrategy)
	 */
	@Override
	public Genome mutate() {
		Genome child =  new BinaryGenome(this);
		// children.add(child);
		return child;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		int n = config.getN();
		long mask = 1L << n-1;
		for(int i = 0; i < n; i++){
			b.append((mask & value) > 0 ? "1" : "0");
			mask >>= 1;
		}
		return b.toString();
	}

	/* (non-Javadoc)
	 * @see jnk.Genome#getAncestors()
	 */
	@Override
	public Vector<Genome> getAncestors() {
		Vector<Genome> ancestors = new Vector<Genome>();
		Genome p = getParent();
		while(p != null){
			ancestors.add(p);
			p = p.getParent();
		}
		return ancestors;
	}

	/* (non-Javadoc)
	 * @see jnk.Genome#getDescendents()
	 */
	@Override
	public Set<Genome> getDescendants() {
		Set<Genome> descendants = new HashSet<Genome>();
		// for(Genome child : children){
		//	addChildren(child,descendants);
		//}
		return descendants;
	}
	
	/* (non-Javadoc)
	 * @see jnk.Genome#getDeathGeneration()
	 */
	@Override
	public int getDeathGeneration() {
		return deathGeneration;
	}

	/* (non-Javadoc)
	 * @see jnk.Genome#setDeathGeneration(long)
	 */
	@Override
	public void setDeathGeneration(int deathGeneration) {
		this.deathGeneration = deathGeneration;
		return;
	}
	
	/**
	 * Add the given gene, and all its descendants to the given set
	 * @param gene - gene to examine
	 * @param descendants - Set for collecting descendants
	 */
	private void addChildren(Genome gene, Set<Genome> descendants){
		descendants.add(gene);
		for(Genome child : gene.getChildren()){
			addChildren(child,descendants);
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Genome otherGenome) {
		double otherFitness = otherGenome.getFitness();
		if(equals(otherGenome)) return 0;
		return fitness > otherFitness ? 1 : -1;
	}

	// TODO: Check the relation between equals and CompareTo
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object arg0) {
		if(arg0 == null || !(arg0 instanceof Genome)) return false;
		Genome otherGenome = (Genome) arg0;
		if(arg0 instanceof BinaryGenome){
			BinaryGenome b = (BinaryGenome)arg0;
			return value == b.value || fitness == b.fitness;
		}
		return fitness == otherGenome.getFitness();
	}
	
}
