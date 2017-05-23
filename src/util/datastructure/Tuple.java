package util.datastructure;

public class Tuple<T,S> implements Comparable<Tuple<T,S>>{
	public T e1;
	public S e2;
	
	public Tuple(T e1, S e2) {
		this.e1 = e1;
		this.e2 = e2;
	}
	
	public T first() {
		return e1;
	}
	
	public S second() {
		return e2;
	}

	@Override
	public int compareTo(Tuple<T,S> o) {
		if (!first().equals(o.first())) return ((Comparable)first()).compareTo(o.first());
		else return ((Comparable)second()).compareTo(o.second());
	}
	
//	public boolean equals(Tuple<T, S> tuple) {
//		if (first().equals(tuple.first()) && second().equals(tuple.second())) return true;
//		else return false;
//	}
}
