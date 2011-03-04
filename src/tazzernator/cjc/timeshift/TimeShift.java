package com.bukkit.tazzernator.timeshift;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Timer;

import org.bukkit.Server;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;

/**
 * TimeShift for bukkit
 * 
 * @author Tazzernator (Andrew Tajsic)
 *
 */
public class TimeShift extends JavaPlugin{
	private Server server = getServer();
	private Timer tick = null;
	private int rate = 1000;
	private final TimeShiftPlayerListener playerListener = new TimeShiftPlayerListener(this, server);

	public TimeShift(PluginLoader pluginLoader, Server instance,
			PluginDescriptionFile desc, File folder, File plugin,
			ClassLoader cLoader) {
		super(pluginLoader, instance, desc, folder, plugin, cLoader);
	}

	public void onDisable() {
		//Reset before power off.
		fileCreate();
	}
	
	static void fileCreate(){
		//Method used for creating our temp file
		try{
			FileWriter fstream = new FileWriter("TimeShift.time");
			BufferedWriter out = new BufferedWriter(fstream);
			out.write("-1");
			out.close();
		}catch (Exception e){
		}
	}

	@Override
	public void onEnable() {
		//Lets create our file.
		fileCreate();
		
		//Lets start the Timer instance.
		TimeShiftTimer tst = new TimeShiftTimer();
		tst.server = getServer();
		tick = new Timer();
		tick.schedule(tst, 0, rate);
		
		// Register our events
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvent(Event.Type.PLAYER_COMMAND, playerListener, Priority.Normal, this);

        // Here we just output some info so we can check all is well
        PluginDescriptionFile pdfFile = this.getDescription();
        System.out.println( pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!" );
	}

}
