package tazzernator.cjc.timeshift;

//java imports
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.persistence.PersistenceException;

//bukkit imports
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

//permissions-related imports
import com.nijikokun.bukkit.Permissions.Permissions;

/**
 * TimeShift for bukkit
 * 
 * @author Tazzernator (Andrew Tajsic), and cjc343
 * 
 */

//feb 18 cjc moved read/write into own class/out of all other classes and to use yml to claim /shift
//also moved to new file name, but not to a yml file, just a world=setting

public class TimeShift extends JavaPlugin {

	final TimeShift plugin = this;
	// public memory
	// Strings should always use this name
	public static String name;
	// for permissions implementation
	static Permissions Permissions = null;
	// store server settings in key=worldname, int setting
	protected static HashMap<String, Integer> settings = new HashMap<String, Integer>();// = new AbstractMap<String,Integer>();
	// store path to TimeShift.time
	public static String path;

	// private memory
	private int rate = 20;

	private final TimeShiftPlayerListener tspl = new TimeShiftPlayerListener();
	private TimeShiftServerListener tssl = new TimeShiftServerListener(this);
	private TimeShiftPersistentReaderWriter tsprw = new TimeShiftPersistentReaderWriter(this);
	private final TimeShiftCommandParser commandParser = new TimeShiftCommandParser(this, tsprw);
	
	// holds temporary file input
	static ArrayList<String> data = new ArrayList<String>();

	// onDisable
	public void onDisable() {
		// stop the timers
		getServer().getScheduler().cancelTasks(this);
	}
	
	
	// onEnable
	public void onEnable() {		
		try { // all of enable
			name = this.getDescription().getName(); // set plugin name
			
			setupConfigFolder();//makes sure config folder exists, defines folder path.
			
			setupConfigFile();//makes sure config file exists, copies it over if it doesn't.
			
			setupPermissions();//sets up permissions
			
			setupDatabase(); // sets up db, if not yet set up.
			
			tsprw.readSettings();// read from db, or from file on first use.

			// Lets start the timers.
			for (World w : getServer().getWorlds()) {
				scheduleTimer(w);
			}// currently set at 20, should check about once per second/once per 2 seconds 

			// Register our events
			// this event only controls /time, nothing else. It attempts to use any /time commands to cancel an active shift
			// without disrupting the command used. If other plugins don't use this method, we may not see /time commands.
			PluginManager pm = getServer().getPluginManager();
			pm.registerEvent(Event.Type.PLAYER_COMMAND_PREPROCESS, tspl, Priority.Low, this);
			// Listen for Permissions enable
			pm.registerEvent(Event.Type.PLUGIN_ENABLE, tssl, Priority.Low, this);
			// Here we just output some info so we can check all is well
			PluginDescriptionFile pdfFile = this.getDescription();
			System.out.println(pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!");
		} catch (Exception e) {
			System.out.println("[" + name + "] Exception thrown in onEnable");
			e.printStackTrace();
		}
	}
	
	private void setupDatabase() {
		try {
			getDatabase().find(TimeShiftWorldSetting.class).findRowCount();
		} catch (PersistenceException ex) {
			System.out.println("Installing " + name + "'s database due to first time use.");
			installDDL();
		}
	}
	
    @Override
    public List<Class<?>> getDatabaseClasses() {
    	List<Class<?>> list = new ArrayList<Class<?>>();
    	list.add(TimeShiftWorldSetting.class);
    	return list;
    }
	
	//setup folder for config if it doesn't exist, and define path variable.
	private void setupConfigFolder() {
		if (this.getDataFolder().exists()) {//check that folder exists
			path = this.getDataFolder().getPath();//set path
		} else {//if it doesn't, make it.
			if (this.getDataFolder().mkdirs()) {
				path = this.getDataFolder().getPath();// + "/" + name + ".startup";
			} else {
				System.out.println(name + " could not create necessary folder structure for settings.");
			}
		}
	}
	
	//setup config file if it doesn't exist.
	private void setupConfigFile() {
		getConfiguration().load();
		try {//check for config file
			File config = new File(path, "/config.yml");
			if (!config.exists()) {//if it doesn't exist:
				//copy it over from the jar.
				InputStream defaultConf = getClass().getResourceAsStream("/config.yml");
				FileWriter confWrite = new FileWriter(config);
				for (int i = 0; (i = defaultConf.read()) > 0;) {
					confWrite.write(i);
				}
				confWrite.flush();
				confWrite.close();
				defaultConf.close();
				getConfiguration().load();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		TimeShiftMessaging.setup(this);//set up messaging strings
	}

    private void setupPermissions() {
    	//setup permissions
        Plugin plugin = this.getServer().getPluginManager().getPlugin("Permissions");
        if (TimeShift.Permissions == null) {
            if (plugin != null) {
            	TimeShift.Permissions = (Permissions)plugin;
                System.out.println("[" + TimeShift.name + "] hooked into Permissions.");
            }
        }
    }
	
	// now thread safe? I have no way of actually knowing since I had no repeatable method to test any resulting bugs before.
	public void scheduleTimer(World w) {
		final TimeShiftRunnable tst = new TimeShiftRunnable();
		tst.world = w; //set timer's world
		//schedule task
		getServer().getScheduler().scheduleAsyncRepeatingTask(plugin, new Runnable() {
			public void run() {
				getServer().getScheduler().scheduleSyncDelayedTask(plugin, tst); // task, in server thread, to run asap
			}
		}, rate, rate); //about once a second?
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
		// passes off the work of received commands to the class that was once built for that.
		// this doesn't intercept the /time command, just the /shift commands.
		// (right now, someone could have changed that) /time is handled by player events still
		return commandParser.handleCommand(sender, command, commandLabel, args);
	}
}
