package util;

import java.util.Vector;

public class Test {
	public enum Color {RED, BLUE};
	
	public static void main (String [] args) throws Exception {
		Vector<Integer> v = new Vector<Integer>();
		for (int i = 0; i < 10; i++) {
			v.add(i);
		}
		
		for (Integer i : v) {
			System.out.println(i);
			if (i % 2 == 0) v.remove(i);
		}
	}
}
