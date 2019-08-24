/**
 * Copyright (C) 2019, Sonia Singhal
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
 * Test Program for Tunable Landscape
 * @author Sharad Singhal
 */
public class Tune {
	/**
	 * Test program for a tunable landscape
	 * @param args - program arguments
	 */
	public static void main(String[] args) {
		int N = 4;
		int K = 2;
		int nMax = 16;
		
		TunableLandscape landscape = new TunableLandscape(N,K,Epistasis.ADJACENT,0,"landscape.txt",true);
		double error = 500;
		int i = 0;
		for(i = 0; i < 200; i++) {
			double sum = 0.0;
			for(int j = 0; j < nMax; j++) {
				float desired = (float) ((j & 0x3)/3.0);
				float fitness = landscape.getFitness(j);
				System.out.println("i = "+i+" j = "+j+" desired = "+desired+" found = "+fitness);
				sum += (fitness-desired)*(fitness-desired);
				landscape.tune(j, desired, (float) 0.05);
			}
			if(Math.abs(error-sum) < 0.001 || error < sum) break;
			System.out.println("** Error -- "+sum);
			error = sum;
			if(i%10 == 0) {
				landscape.locatePeaks();
				landscape.writeLandscape("output"+i+".txt");
			}
		}
		landscape.locatePeaks();
		landscape.writeLandscape("output"+i+".txt");
	}

}
