package tazzernator.cjc.timeshift;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.bukkit.World;
import org.bukkit.entity.Player;

public class TimeShiftFileReaderWriter {

	public static ArrayList<String> readLines(String filename) throws IOException {
		// Method to read our numbers in the startup file
		TimeShift.data.clear();
		FileReader fileReader = new FileReader(filename);
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		String line = null;
		while ((line = bufferedReader.readLine()) != null) {
			TimeShift.data.add(line.toLowerCase());
		}
		bufferedReader.close();
		return TimeShift.data;
	}

	private static void initializeFile() {
		FileWriter fstream;
		try {
			fstream = new FileWriter(TimeShift.path);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write("world=-1");
			out.close();
			// input it
			readSettings();
		} catch (IOException f) {
			System.out.println("Could not create file for " + TimeShift.name);
		}
	}
	// method to take those numbers, parse them, and input them into memory
	public static void readSettings() {
		try {
			try {
				readLines(TimeShift.path);
			} catch (IOException e) {
				// create a file if unreadable
				initializeFile();
				try {
					readLines(TimeShift.path);
				} catch (Exception p) {
				}
			}
			// iterate through strings, splitting at =

			for (String d : TimeShift.data) {
				if (d.length() >= 3) {
					//System.out.println(d);
					String[] sets = d.split("=");
					int setting = Integer.parseInt(sets[1]);
					String world = sets[0];
					//System.out.println(world + " and " + setting + " with sets.length : " +  sets.length);
					if (sets.length == 2) {
						try {
						TimeShift.settings.put(world, setting);
						} catch (Exception e) {
							System.out.println("Error parsing " + TimeShift.name + "'s settings file.");
							e.printStackTrace();
						}
					}
				}
			}
		} catch (Exception e) {

			initializeFile();
			System.out.println("There was a problem parsing " + TimeShift.name + "'s data. World startup states have been reset.");
		}
	}

	// build and write string to file for persistent settings.
	public static void persistentWriter(int setting, Player player) {
	//	System.out.println("persistent state attempted: " + setting + "  in world : " + player.getWorld().hashCode());
		String output = "";
		
		//read in file
		//modify correct setting
		//output to file
		try {
				readLines(TimeShift.path);
		} catch (Exception e) {
		}
		
		World w = player.getWorld();

		Boolean isSet = false;
		for (String d : TimeShift.data) {
			String[] sets = d.split("=");

			if (sets.length == 2) {
				if (sets[0].equals(w.getName())) {
					isSet = true;
					output = output + sets[0] + "=" + setting + "\n";
				} else {
					output = output + sets[0] + "=" + sets[1] + "\n";
				}
			}
		}
		if (!isSet) {
			output = output + w.getName() + "=" + setting + "\n";
		}
		
		//write out the output.
		FileWriter fstream;
		try {
			fstream = new FileWriter(TimeShift.path);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(output);
			out.close();
		} catch (IOException e) {
		}
	}
}
