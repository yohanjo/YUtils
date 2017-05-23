package util.io;

import java.io.BufferedReader;

public class YFileReader {
	private BufferedReader file;
	private String line;
	
	public YFileReader(String path) throws Exception {
		this.file = new BufferedReader(new java.io.FileReader(path));
	}
	
	public boolean readLine() throws Exception {
		this.line = this.file.readLine();
		if (this.line != null) return true;
		else return false;
	}
	
	public String getLine() {
		return this.line;
	}
	
	public void close() throws Exception {
		this.file.close();
	}
}
