package com.bukkit.tazzernator.timeshift;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TimerTask; //import org.bukkit.Server;
import org.bukkit.World;

/*
 * Credit for the concept of this class goes to feverdream
 * https://github.com/feverdream/
 */

//this class has been modified by cjc and Tazzernator. Dates of modification are unknown, but currently prior(or equal) to Feb. 06, 2011, 
//the date this was added, and certainly later than Jan. 16, 2011, the date Noon('s source) was first publicly available.
//as such, this class, as a work, and as specified by the AGPL license Noon was released under, is also AGPL licensed.

// in order to make that a little more 'prominent', I'm just going to repeat that this class is AGPL licensed.

// this class is AGPL licensed.

// ******************************** THIS CLASS IS AGPL LICENSED!!!1! **********************************************

public class TimeShiftTimer extends TimerTask {
	public World world = null;
	public long wantedTime = 0;
	public int dayStart;
	ArrayList<String> data = new ArrayList<String>();

	private ArrayList<String> readLines(String filename) throws IOException {
		// Method to read our number in the temp file
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
		long time = world.getTime();
		long relativeTime = time % 24000;
		long startOfDay = time - relativeTime;

		// Read number
		try {
			readLines("plugins/TimeShift/TimeShift.time");
		} catch (IOException e) {
		}

		// Number is loaded
		for (String d : data) {
			dayStart = Integer.parseInt(d);
		}

		// Number is checked, and if it applies, the time is set
		if (relativeTime > 12000 && dayStart == 0) {
			world.setTime(startOfDay + 24000);
		} else if ((relativeTime > 22200 || relativeTime < 13700) && dayStart == 13800) {
			world.setTime(startOfDay + 37700);
		}
	}

}
