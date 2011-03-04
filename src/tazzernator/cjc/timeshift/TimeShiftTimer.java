package com.bukkit.tazzernator.timeshift;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TimerTask;
import org.bukkit.Server;

/*
 * Credit for the concept of this class goes to feverdream
 * https://github.com/feverdream/
 */
public class TimeShiftTimer extends TimerTask{
	public Server server = null;
	public long wantedTime = 0;
	public int dayStart;
	ArrayList<String> data = new ArrayList<String>();
	
	private ArrayList<String> readLines(String filename) throws IOException {
		//Method to read our number in the temp file
		data.clear();
		FileReader fileReader = new FileReader(filename);
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		String line = null;
		while ((line = bufferedReader.readLine()) != null) {
            	data.add(line.toLowerCase());
            }
        bufferedReader.close();
        return data;
	}
	
	public void run() {
		long time = server.getTime();
		long relativeTime = time % 24000;
		long startOfDay = time - relativeTime;
				
		//Read number
		try {
			readLines("TimeShift.time");
		} catch (IOException e) {
		}
		
		//Number is loaded
		 for (String d : data){
	        	dayStart = Integer.parseInt(d);
	        }
			
		 //Number is checked, and if it applies, the time is set
		if( relativeTime > 12000 && dayStart == 0) {
            server.setTime(startOfDay + 24000);
        }else if((relativeTime > 22200 || relativeTime < 13700) && dayStart == 13800) {
            server.setTime(startOfDay+37700);
        }
	}

}
