package tazzernator.cjc.timeshift;

//java imports
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

//ebeans import
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

//permissions import
import com.nijikokun.bukkit.Permissions.Permissions;

/**
 * TimeShift for bukkit
 * 
 * @author Tazzernator (Andrew Tajsic), and cjc343
 * 
 */

// Partial Change History:
// feb 18 cjc moved read/write into own class/out of all other classes.
// plugin.yml file is now used to claim the "shift" command in-game.
// TimeShift.startup used for persistent storage (This has since changed, ebeans is now used).

public class TimeShift extends JavaPlugin {
	// for use when setting up tasks
	final TimeShift plugin = this;
	// Strings should always use this name
	public static String name;
	// store path to configuration files
	public static String path;

	// for permissions implementation
	static Permissions Permissions = null;
	// store server settings in key=worldname, int setting
	protected static HashMap<String, Integer> settings = new HashMap<String, Integer>();// = new AbstractMap<String,Integer>();
	// rate in 'ticks' between poll for current time
	private int rate = 20;

	
	private TimeShiftWorldListener tswl = new TimeShiftWorldListener(this);
	// Listens for use of "time [x]" commands
	private final TimeShiftPlayerListener tspl = new TimeShiftPlayerListener();
	// Listens for activation of Permissions plugin
	private TimeShiftServerListener tssl = new TimeShiftServerListener(this);
	// All IO
	private TimeShiftPersistentReaderWriter tsprw = new TimeShiftPersistentReaderWriter(this);
	// Parses "shift" commands
	private final TimeShiftCommandParser commandParser = new TimeShiftCommandParser(this, tsprw);

	// holds temporary file input (doesn't really belong here, belongs in IO)
	static ArrayList<String> data = new ArrayList<String>();

	// onDisable must be implemented in a JavaPlugin: called when TimeShift is disabled by the server
	public void onDisable() {
		// stop the timers
		getServer().getScheduler().cancelTasks(this);
	}

	// onEnable: called when TimeShift is enabled
	// this class sets up TimeShift's initial configuration and ensures that
	// any first-run activities are completed
	public void onEnable() {
		try { // all of enable
			name = this.getDescription().getName(); // set plugin name

			setupConfigFolder();// makes sure config folder exists, defines folder path.

			setupConfigFile();// makes sure config file exists, copies in default if it doesn't.

			setupPermissions();// sets up permissions if Permissions plugin is present

			setupDatabase(); // sets up db, if not yet set up.

			tsprw.readSettings();// read startup settings from db, or from file on first use of db version.

			// Starts one timer for each world
			for (World w : getServer().getWorlds()) {
				scheduleTimer(w);
			}

			// Register events
			// the preprocess event only controls /time, nothing else. It attempts to use any /time commands to cancel an active shift
			// without disrupting the command used.
			PluginManager pm = getServer().getPluginManager();
			
			pm.registerEvent(Event.Type.WORLD_LOAD, tswl, Priority.Lowest, this);
			// Register a command preprocess event: runs before commands are processed
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

	// set up database if nonexistent
	private void setupDatabase() {
		try {
			getDatabase().find(TimeShiftWorldSetting.class).findRowCount();
		} catch (PersistenceException ex) { // database not yet created
			System.out.println("Installing " + name + "'s database due to first time use.");
			installDDL();
		}
	}

	// database will store TimeShiftWorldSetting objects
	@Override
	public List<Class<?>> getDatabaseClasses() {
		List<Class<?>> list = new ArrayList<Class<?>>();
		list.add(TimeShiftWorldSetting.class);
		return list;
	}

	// setup folder for config if it doesn't exist, and define path variable.
	// this method ensures that the proper folder structure exists and creates it if it doesn't
	private void setupConfigFolder() {
		if (this.getDataFolder().exists()) {// check that folder exists
			path = this.getDataFolder().getPath();// set path
		} else {// if it doesn't, make it.
			if (this.getDataFolder().mkdirs()) {
				path = this.getDataFolder().getPath();// set path variable
			} else {
				System.out.println(name + " could not create necessary folder structure for settings.");
			}
		}
	}

	// setup config file if it doesn't exist.
	// this class checks for the existence of the localization and configuration file
	// it copies in defaults if it doesn't exist.
	private void setupConfigFile() {
		getConfiguration().load(); // move to end
		try {// check for config file
			File config = new File(path, "/config.yml");
			if (!config.exists()) {// if it doesn't exist:
				// copy over defaults from the jar.
				InputStream defaultConf = getClass().getResourceAsStream("/config.yml");
				FileWriter confWrite = new FileWriter(config);
				for (int i = 0; (i = defaultConf.read()) > 0;) {
					confWrite.write(i);
				}
				confWrite.flush();
				confWrite.close();
				defaultConf.close();
				getConfiguration().load(); // move to end
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		TimeShiftMessaging.setup(this);// set up messaging strings now that config file is loaded
	}

	// this method will hook into the Permissions plugin if Permissions is loaded before TimeShift
	private void setupPermissions() {
		// setup permissions
		Plugin plugin = this.getServer().getPluginManager().getPlugin("Permissions");
		if (TimeShift.Permissions == null) { // not yet set up
			if (plugin != null) { // Permissions plugin is present
				TimeShift.Permissions = (Permissions) plugin;
				System.out.println("[" + TimeShift.name + "] hooked into Permissions.");
			}
		}
	}

	// This should be reduced to a single repeating Sync task
	// this method schedules a delayed repeating poll for each world
	public void scheduleTimer(World w) {
		final TimeShiftRunnable tst = new TimeShiftRunnable();
		tst.world = w; // set timer's world
		//System.out.println("TimeShift is starting a timer for world: " + w.getName());
		// schedule task
		getServer().getScheduler().scheduleAsyncRepeatingTask(plugin, new Runnable() {
			public void run() {
				getServer().getScheduler().scheduleSyncDelayedTask(plugin, tst); // task, in server thread, to run asap
			}
		}, rate, rate); // about once a second
	}

	// the onCommand method is used when CommandExecutors aren't. TimeShift was originally written prior to the introduction of
	// both onCommand and CommandExecutors, but is better suited to using a CommandExecutor in the near future.
	@Override
	public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
		// passes off the work of received commands to the class that was once built for that.
		// this doesn't intercept the /time command, just the /shift commands.
		// (right now, someone could have changed that) /time is handled by player events still

		//System.out.println("TimeShift received a registered command (presumably /shift) and will now try to execute it.");
		// this should be done as a CommandExecutor in onEnable instead.
		return commandParser.handleCommand(sender, command, commandLabel, args);
	}
}
