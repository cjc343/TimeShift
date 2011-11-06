package tazzernator.cjc.timeshift;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.bukkit.World;

public class TimeShiftPersistentReaderWriter {

	private static TimeShift instance;

	public TimeShiftPersistentReaderWriter(TimeShift instance) {
		TimeShiftPersistentReaderWriter.instance = instance;
	}

	public ArrayList<String> readLines(String filename) throws IOException {
		// Method to read our numbers in the startup file
		TimeShift.data.clear();
		FileReader fileReader = new FileReader(filename);
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		String line = null;
		while ((line = bufferedReader.readLine()) != null) {
			TimeShift.data.add(line);
		}
		bufferedReader.close();
		// returns a list of lines, each line contains settings for a world.
		// data variable should be moved to this class
		return TimeShift.data;
	}

	// method to take those numbers, parse them, and input them into memory
	// this can be cut down in next release since all TimeShift.startup files should be converted already
	//there is a bug in the conversion process (luckily it only affected a small subset of users) when the existing
	//TimeShift.startup file contains multiple entries for a single world. This was due to a much older bug which
	//caused duplicate entries in a small number of cases, which, when fixed, did not remove the duplicate entries.
	//The fix is to remove the database and allow a clean one to be created.
	public void readSettings() {

		try {
			// get old setting file, if it exists still
			readLines(TimeShift.path + "/TimeShift.startup");
			System.out.println(TimeShift.name + " is converting the old setting file to persistent storage!");
			// for each line of the file
			instance.getDatabase().beginTransaction();
			for (String d : TimeShift.data) {
				// string length is at least l=0
				if (d.length() >= 3) {
					String[] sets = d.split("=");
					if (sets.length == 2) { // if there were two keys, parse:
						int setting = Integer.parseInt(sets[1]); // setting on right
						String world = sets[0];// world name on left

						// then create a db entry, add to settings HashMap, and delete the file.
						try {
							TimeShiftWorldSetting dbSetting = new TimeShiftWorldSetting();
							dbSetting.setSetting(setting);
							dbSetting.setWorldName(world);
							instance.getDatabase().insert(dbSetting);
							// add the setting to our HashMap
							TimeShift.settings.put(world, setting);
							File settingFile = new File(TimeShift.path + "/TimeShift.startup");
							settingFile.delete();
						} catch (Exception e) {
							// error putting
							System.out.println("Error creating database from settings file.");
							e.printStackTrace();
						}
					}
				}
			}
			instance.getDatabase().commitTransaction();
		} catch (IOException e) { // TimeShift.startup never existed or has been deleted! (All the above will disappear next release)
			// settings file did not exist. Yay!
			// retrieve all startup settings from db and load into memory for current behavior.
			for (TimeShiftWorldSetting dbSetting : instance.getDatabase().find(TimeShiftWorldSetting.class).findList()) {
				TimeShift.settings.put(dbSetting.getWorldName(), dbSetting.getSetting());
			}
		}
	}

	// write new setting to database or modify existing setting
	public void persistentWriter(int setting, World w) {
		TimeShiftWorldSetting dbSetting = instance.getDatabase().find(TimeShiftWorldSetting.class).where().ieq("worldName", w.getName()).findUnique();
		if (dbSetting == null) {
			dbSetting = new TimeShiftWorldSetting();
			dbSetting.setWorldName(w.getName());
		}
		dbSetting.setSetting(setting);
		instance.getDatabase().save(dbSetting);
	}
}
