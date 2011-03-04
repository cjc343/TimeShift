package com.bukkit.tazzernator.timeshift;

//java imports
import java.io.File;
import java.util.ArrayList;
import java.util.Timer;
import java.util.Vector;

//bukkit imports
import org.bukkit.Server; 
import org.bukkit.World;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;

//permissions imports
import com.nijikokun.bukkit.Permissions.Permissions;
import com.nijiko.permissions.PermissionHandler;
import org.bukkit.plugin.Plugin; //req. by permissions

//file read/write imports
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
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
	//private static Server instance;
	private Timer tick = null;
	private int rate = 1000;
	private final TimeShiftPlayerListener playerListener = new TimeShiftPlayerListener(this, server);

	//Strings should always use this name
	public static String name = "TimeShift";
	//holds temporary file input
	static ArrayList<String> data = new ArrayList<String>();
	// for permissions implementation
	public static PermissionHandler Permissions = null;
	// store server settings
	public static Vector<Integer> settings = new Vector<Integer>();
	// store path to TimeShift.time
	public static String path;
	public TimeShift(PluginLoader pluginLoader, Server instance, PluginDescriptionFile desc, File folder, File plugin, ClassLoader cLoader) {
		super(pluginLoader, instance, desc, folder, plugin, cLoader);

	//	TimeShift.instance = instance;
		//make path to TimeShift.time
		folder.mkdirs();
		//set path
		path = folder.getPath() + "/"+ name + ".time";
		//read file
		readSettings();
		
	}

	public void onDisable() {
		//stop the timers
		tick.cancel();
	}

	private static ArrayList<String> readLines(String filename) throws IOException {
		// Method to read our numbers in the startup file
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


//method to take those numbers, parse them, and input them into memory
	static void readSettings() {
		try {
			try {
				readLines(path);
			} catch (IOException e) {
				//create a file if unreadable
				FileWriter fstream;
				try {
					fstream = new FileWriter(path);
					BufferedWriter out = new BufferedWriter(fstream);
					out.write("-1");
					out.close();
					//input it
					readSettings();
				} catch (IOException f) {
					System.out.println("Could not create file for " + name);
				}
			}
			//iterate through strings, splitting at ,
			for (String d : data) {
				//System.out.println("Data point : " + d);
				String[] sets = d.split(",");
				for (String e : sets) {
					//add
					settings.add(Integer.parseInt(e));
				}
			}
		} catch (Exception e) {
			//parsing issues mostly?
			//rewrite file
			FileWriter fstream;
			try {
				fstream = new FileWriter(path);
				BufferedWriter out = new BufferedWriter(fstream);
				out.write("-1");
				out.close();
				//input it
				readSettings();
			} catch (IOException f) {
	//			System.out.println("Could not create file for " + name);
			}
			System.out.println("There was a problem parsing " + name + "'s data. World startup states have been reset.");
		}
		//empty file, add -1.
		if (settings.size() == 0) {
			FileWriter fstream;
			try {
				fstream = new FileWriter(path);
				BufferedWriter out = new BufferedWriter(fstream);
				out.write("-1");
				out.close();
				//input it.
				readSettings();
			} catch (IOException f) {
				System.out.println("Could not create file for " + name);
			}
		}
	}

	@Override
	public void onEnable() {
		setupPermissions();//setup permissions
		
		//settings already read.		
		
		// Lets start the Timer instance.

		//this is where changes were once made when server stopped housing the get/set Time functions.

		int i = 0;
		tick = new Timer(true);
		for (World w : getServer().getWorlds()) {
			TimeShiftTimer tst = new TimeShiftTimer();
			tst.world = w;
			tst.index = i;
			tick.schedule(tst, 0, rate);
			i++;
		}

		// Register our events
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Event.Type.PLAYER_COMMAND, playerListener, Priority.Normal, this);

		// Here we just output some info so we can check all is well
		PluginDescriptionFile pdfFile = this.getDescription();
		System.out.println(pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!");
	}

	//modified setup method from Permissions thread by Niji
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
