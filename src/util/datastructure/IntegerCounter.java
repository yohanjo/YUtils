package util.datastructure;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

public class IntegerCounter<K> extends TreeMap<K,Integer> {
	public void increase(K key) {
		Integer value = this.get(key);
		if (value == null) this.put(key, 1);
		else this.put(key, value+1);
	}
	
	public void increase(K key, int n) {
		Integer value = this.get(key);
		if (value == null) this.put(key, n);
		else this.put(key, value+n);
	}
	
	public void increaseAll(Collection<? extends K> keys) {
		for (K key : keys)
			increase(key);
	}
	
	public void decrease(K key) throws Exception {
		Integer value = this.get(key);
		if (value == null) throw new Exception("Error: no such element");
		else this.put(key, value-1);
	}
	
	public void decrease(K key, int n) throws Exception {
		Integer value = this.get(key);
		if (value == null) throw new Exception("Error: no such element");
		else this.put(key, value-n);
	}
	
	public void removeIfLessThan(int threshold) {
		Vector<K> remove = new Vector<K>();
		for (Map.Entry<K,Integer> entry : this.entrySet())
			if (entry.getValue() < threshold) remove.add(entry.getKey());
		
		for (K key : remove)
			this.remove(key);
	}
	
	public void removeIfGreaterThan(int threshold) {
		Vector<K> remove = new Vector<K>();
		for (Map.Entry<K,Integer> entry : this.entrySet())
			if (entry.getValue() > threshold) remove.add(entry.getKey());
		
		for (K key : remove)
			this.remove(key);
	}

	public void add(IntegerCounter<K> c) {
		for (Map.Entry<K,Integer> entry : c.entrySet()) {
			Integer value = this.get(entry.getKey());
			if (value == null) this.put(entry.getKey(), entry.getValue());
			else this.put(entry.getKey(), value+entry.getValue());
		}
	}
	
	public String toCSVFormat() {
		String str = "";
		for (Map.Entry<K, Integer> entry : this.entrySet()) {
			K key = entry.getKey();
			Integer value = entry.getValue();
			if (key instanceof String) str += "\"" + key.toString().replaceAll("\"", "\"\"") + "\"," + value + "\n";
			else str += key + "," + value + "\n";
		}
		return str;
	}
	
	public void writeInCSVFormat(String outFilePath) throws Exception {
		PrintWriter outFile = new PrintWriter(new FileWriter(outFilePath));
		for (Map.Entry<K, Integer> entry : this.entrySet()) {
			K key = entry.getKey();
			Integer value = entry.getValue();
			if (key instanceof String) outFile.println( "\"" + key.toString().replaceAll("\"", "\"\"") + "\"," + value );
			else outFile.println( key + "," + value );
		}
		outFile.close();
	}
	
	
	public String toCSVFormat(String delimitPattern) {
		String str = "";
		for (Map.Entry<K, Integer> entry : this.entrySet()) {
			K key = entry.getKey();
			Integer value = entry.getValue();
			if (key instanceof String) {
				String [] tokens = ((String) key).split(delimitPattern);
				for (String token : tokens) str += "\"" + token.replaceAll("\"", "\"\"") + "\",";
				str += value + "\n";
			}
			else str += key + "," + value + "\n";
		}
		return str;
	}
}
