package util.math;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import java.util.Vector;

import util.datastructure.Counter;


public class Sample {

	static Random random = new Random(System.currentTimeMillis());
	
	public static void test() {
		Counter<Integer> count = new Counter<Integer>();
		for (int i = 0; i < 100000; i++) {
			int [] sample = randomIntegers(20, 5);
			for (int s : sample) count.increase(s);
		}
		for (int i = 0; i < 20; i++) {
			Integer value = count.get(i);
			if (value == null) System.out.println(String.format("%3d: %5d", i, 0));
			else System.out.println(String.format("%3d: %5d", i, value));
		}
	}
	
	public static <T> List<T> sample(List<T> list, int sampleSize) {
		Vector<T> sample = new Vector<T>(sampleSize); 
		int [] indicies = randomIntegers(list.size(), sampleSize);
		for (int index : indicies)
			sample.add(list.get(index));
		return sample;
	}
	
	/* Floyd's Algorithm */
	public static int [] randomIntegers(int numTotal, int sampleSize){
		if (numTotal <= sampleSize) {
			int [] result = new int[numTotal];
			for (int i = 0; i < result.length; i++)
				result[i] = i;
			return result;
		}
		
		int [] numbers = new int[numTotal];
		for (int i = 0; i < numTotal; i++)
			numbers[i] = i;

		TreeSet<Integer> sample = new TreeSet<Integer>();
		for(int i = numTotal - sampleSize; i < numTotal; i++){
			int pos = random.nextInt(i+1);
			int selected = numbers[pos];
			if (sample.contains(selected))
				sample.add(numbers[i]);
			else
				sample.add(selected);
		}
		
		int [] result = new int[sampleSize];
		int index = 0;
		Iterator<Integer> sampleIter = sample.iterator();
		while (sampleIter.hasNext())
			result[index++] = sampleIter.next();
		
		return result;
	}
	
	
	/* 
	 * Floyd¡¯s Algorithm
	 * http://eyalsch.wordpress.com/2010/04/01/random-sample/
	 */
//	public static <T> Set<T> randomSample4(List<T> items, int m){   
//		HashSet<T> res = new HashSet<T>(m); 
//		int n = items.size();
//		for(int i=n-m;i<n;i++){
//			int pos = rnd.nextInt(i+1);
//			T item = items.get(pos);
//			if (res.contains(item))
//				res.add(items.get(i));
//			else
//				res.add(item);
//		}
//	return res;
//	}

}
