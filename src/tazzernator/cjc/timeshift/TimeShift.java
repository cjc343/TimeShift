package com.bukkit.tazzernator.timeshift;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Timer;

import org.bukkit.Server; //import org.bukkit.World;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;

//permissions
import com.nijikokun.bukkit.Permissions.Permissions;
import com.nijiko.permissions.PermissionHandler;
import org.bukkit.plugin.Plugin;

//file read
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * TimeShift for bukkit
 * 
 * @author Tazzernator (Andrew Tajsic)
 * 
 */
// includes
public class TimeShift extends JavaPlugin {
	private Server server = getServer();
	private Timer tick = null;
	private int rate = 1000;
	private final TimeShiftPlayerListener playerListener = new TimeShiftPlayerListener(this, server);

	static ArrayList<String> data = new ArrayList<String>();

	// public Boolean useP = true;
	public static PermissionHandler Permissions = null;

	public TimeShift(PluginLoader pluginLoader, Server instance, PluginDescriptionFile desc, File folder, File plugin, ClassLoader cLoader) {
		super(pluginLoader, instance, desc, folder, plugin, cLoader);

		folder.mkdirs();

	}

	public void onDisable() {
		// Reset before power off.
		fileCreate();
	}

	private static ArrayList<String> readLines(String filename) throws IOException {
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

	static void fileCreate() {
		// Method used for creating our temp file
		try {
			FileWriter fstream = new FileWriter("plugins/TimeShift/TimeShift.time");
			BufferedWriter out = new BufferedWriter(fstream);

			try {
				readLines("plugins/TimeShift/TimeShift-Startup.time");
			} catch (IOException e) {
				out.write("-1");
			}
			for (String d : data) {
				out.write(d);
			}
			// out.write("-1");
			out.close();
		} catch (Exception e) {
		}
	}

	@Override
	public void onEnable() {
		setupPermissions();
		// Lets create our file.
		fileCreate();

		// Lets start the Timer instance.
		TimeShiftTimer tst = new TimeShiftTimer();
		// cjc343 made a change here. tst.server is now actually a world, but
		// has not been renamed.
		tst.world = getServer().getWorlds()[0];
		tick = new Timer();
		tick.schedule(tst, 0, rate);

		// Register our events
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Event.Type.PLAYER_COMMAND, playerListener, Priority.Normal, this);

		// Here we just output some info so we can check all is well
		PluginDescriptionFile pdfFile = this.getDescription();
		System.out.println(pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!");
	}

	public void setupPermissions() {
		Plugin test = this.getServer().getPluginManager().getPlugin("Permissions");

		if (TimeShift.Permissions == null) {
			if (test != null) {
				TimeShift.Permissions = ((Permissions) test).getHandler();
			} else {
				// useP = false;
				// log.info(Messaging.bracketize("GeneralEssentials") +
				// " Permission system not enabled. Disabling plugin.");
				// this.getServer().getPluginManager().disablePlugin(this);
			}
		}
	}

}
