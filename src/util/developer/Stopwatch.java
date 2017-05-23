package util.developer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import util.datastructure.Tuple;



public class Stopwatch {

	private Date start;
	private Vector<Tuple<String,Date>> laps = new Vector<Tuple<String,Date>>();
	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
	private final String lastLapKey = "LAST LAP";
	
	public void start() {
		reset();
		start = new Date();
	}

	public void lap(String key) {
		laps.add(new Tuple<String,Date>(key, new Date()));
	}
	
	public void stop() {
		lap(lastLapKey);
	}
	
	public void reset() {
		start = null;
		laps.clear();
	}
	
	public long getElapsedMSecs(String key) throws Exception {
		for (Tuple<String,Date> lap : laps) {
			if (lap.first().equals(key))
				return lap.second().getTime() - start.getTime();
		}
		throw new Exception("No such key: " + key);
	}
	
	public long getElapsedSecs(String key) throws Exception {
		return getElapsedMSecs() / 1000;
	}
	
	public long getElapsedMSecs() {
		return laps.lastElement().second().getTime() - start.getTime();
	}

	public long getElapsedSecs() {
		return (laps.lastElement().second().getTime() - start.getTime()) / 1000;
	}
	
	@Override
	public String toString() {
		String str = "====================================================\n";
		str += "Start Time: " + dateFormat.format(start) + "\n";
		long prevTime = start.getTime();
		for (Tuple<String,Date> lap : laps) {
			Date lapDate = lap.second();
			str += "\"" + lap.first() + "\": " + dateFormat.format(lapDate) + " [" + durationToString(lapDate.getTime() - prevTime) + "]\n"; 
			prevTime = lapDate.getTime();
		}

		long elapsedTotal = getElapsedMSecs();
		str += "Total Elapsed Time: " + durationToString(elapsedTotal) + "\n"; 
		str += "====================================================\n";
		
		return str;
	}
	
	private String durationToString(long durationInMS) {
		long msec = durationInMS % 1000;
		long sec = (durationInMS / 1000) % 60;
		long min = (durationInMS / (60 * 1000)) % 60;
		long hour = durationInMS / (60 * 60 * 1000);
		
		return (hour==0 ? "" : hour + "hr ") + (hour==0 && min==0 ? "" : String.format("%2d", min) + "min ") + (hour==0 && min==0 && sec==0 ? "" : String.format("%2d", sec) + "s ") + msec + "ms";
	}
}
