package util.text;

import java.util.List;

public class Performance {

	public static double accuracy(List<? extends Object> data, List<? extends Object> answer) throws Exception {
		if (data.size() != answer.size()) throw new Exception("Data and answer should have the same size");
		
		int correct = 0;
		for (int i = 0; i < data.size(); i++)
			if (data.get(i).equals(answer.get(i))) correct++; 
		
		return (double)correct/data.size();
	}
	
	public static double precision(List<Object> data, List<Object> answer) throws Exception {
		if (data.size() != answer.size()) throw new Exception("Data and answer should have the same size");
		
		int correct = 0;
		for (int i = 0; i < data.size(); i++)
			if (data.get(i).equals(answer.get(i))) correct++; 
		
		return (double)correct/data.size();
	}


}
