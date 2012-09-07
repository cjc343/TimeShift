package tazzernator.cjc.timeshift;

//java imports
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Logger;

//ebeans import
import javax.persistence.PersistenceException;

//bukkit imports
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import tazzernator.cjc.timeshift.TimeShiftConfiguration.TimeShiftMessaging;
import tazzernator.cjc.timeshift.settings.WorldSetting;
import tazzernator.cjc.timeshift.settings.LoopSetting;

/**
 * TimeShift for bukkit
 * 
 * @author Tazzernator (Andrew Tajsic), and cjc343
 * 
 */

public class TimeShift extends JavaPlugin {
	// Strings should always use this name
	public static String name;
	// store path to configuration files
	public static String path;

	// store server settings in key=worldname, value=setting
	protected static HashMap<String, WorldSetting> world_settings = new HashMap<String, WorldSetting>();// = new AbstractMap<Worldname,Setting>();
	protected static HashMap<String, String> command_aliases = new HashMap<String, String>(); // <Alias, CommandName>
	protected static HashMap<String, LoopSetting> loop_settings = new HashMap<String, LoopSetting>();
	
	// protected static HashMap<String, LoopSetting>
	// rate in 'ticks' between poll for current time
	@Deprecated
	private int rate;
	@Deprecated
	private boolean classic;
	
	protected TimeShiftMessaging tsm;
	protected TimeShiftConfiguration tsc;
	
    private FileConfiguration startupConfig = null;
    private File startupFile = null;

	protected Logger l = Logger.getLogger("minecraft");

	// onDisable must be implemented in a JavaPlugin: called when TimeShift is disabled by the server
	public void onDisable() {
		// stop all the timers
		getServer().getScheduler().cancelTasks(this);
	}

	// onEnable: called when TimeShift is enabled
	// this class sets up TimeShift's initial configuration and ensures that
	// any first-run activities are completed
	public void onEnable() {
		try { // all of enable
			PluginDescriptionFile pdfFile = this.getDescription();
			name = pdfFile.getName(); // set plugin name

			setupConfigFolder();// makes sure config folder exists, defines folder path.
			setupConfigFile();// makes sure config file exists, copies in default if it doesn't.
			//also initializes startup config
			//Convert from bukkit persistence to a f
			convertDatabase(); // sets up db, if not yet set up.
			readSettings();// read startup settings from db, or from file on first use of db version.

			rate = tsc.getRate();

			// Register events
			// the preprocess event only controls /time, nothing else. It attempts to use any /time commands to cancel an active shift
			// without disrupting the command used.
			PluginManager pm = getServer().getPluginManager();
			pm.registerEvents(new TimeShiftWorldListener(this), this);
			// register the command executor properly now
			if (tsc.detectTime()) { // only register PlayerListener if user wants it. On by default because it's really nifty and cancels asap for other commands.
				// Register a command preprocess event: runs before commands are processed to detect /time [x] commands.
				pm.registerEvents(new TimeShiftPlayerListener(this), this);
			}
			
			getCommand("shift").setExecutor(new TimeShiftCommandParser(this));
			
			// Starts one timer for each world with a configured setting
			scheduleTimers();

			l.info(name + " version " + pdfFile.getVersion() + " is enabled!");
		} catch (Exception e) {
			System.out.println("[" + name + "] Exception thrown in onEnable");
			e.printStackTrace();
		}
	}

	
	//loads startupConfig from YML
    protected void loadStartupConfig() {
        if (startupFile == null) {
        	startupFile = new File(path, "/startup.yml");
        }
        startupConfig = YamlConfiguration.loadConfiguration(startupFile);
    }
    //save startupConfig to YML
    protected void saveCustomConfig() {
        if (startupConfig == null || startupFile == null) {
        	return;
        }
        try {
            startupConfig.save(startupFile);
        } catch (IOException ex) {
            l.severe("[TimeShift] Could not save startup configuration to " + startupFile);
        }
    }
    //convert old Persistence database to yml. Bukkit's persistence layer appears entirely unflexible when updating it.
	@Deprecated
    private void convertDatabase() {
		try {
			//TODO: Remove conversion.
			HashMap<String, Integer> oldsettings = new HashMap<String, Integer>();
			for (TimeShiftWorldSetting dbSetting : getDatabase().find(TimeShiftWorldSetting.class).findList()) {
				oldsettings.put(dbSetting.getWorldName(), dbSetting.getSetting());
			}
			removeDDL(); //doesn't actually throw the sqlexception but still outputs it
			String setting = ".setting";
			// Convert old db settings to new persistent storage format using default config values.
			for (Entry<String, Integer> e : oldsettings.entrySet()) {
				String wname = e.getKey();
				int value = e.getValue();
				switch (value) {
				case 0:
					// day
					startupConfig.set(wname + setting, "day");
					break;
				case 13800:
					// night
					startupConfig.set(wname + setting, "night");
					break;
				case 12000:
					// sunset
					startupConfig.set(wname + setting, "sunset");
					break;
				case 22000:
					// sunrise
					startupConfig.set(wname + setting, "sunrise");
					break;
				case -2:
					// setrise
					startupConfig.set(wname + setting, "setrise");
					break;
				case -1:
				default:
					break;
				}
			}
			try {
				startupConfig.save(startupFile);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			l.info(name + " has converted your startup settings to a new storage format.");
		} catch (PersistenceException ex2) {
			// old db doesn't exist, don't care, only checking to convert settings. removeDDL above assures this is thrown forever after.
			// I tried to do this with converting to a new database format, but it's surprisingly hard to update bukkit's persistence while suppressing harmless errors.
		}
	}	
	
	// database will store StartupSetting objects
	@Deprecated
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
				l.warning(name + " could not create necessary folder structure for settings.");
			}
		}
	}

	// setup config file if it doesn't exist.
	// this method checks for the existence of the localization and configuration file
	// it copies in defaults if it doesn't exist.
	private void setupConfigFile() {
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
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		tsc = new TimeShiftConfiguration(this);
		tsc.readLoops();
		classic = tsc.classicTime();
		if (classic) {
			l.severe("[TimeShift] I see you're using classic mode! Classic mode is less efficient and I plan to remove it. Please let me know why you've chosen to use it if you want it to stay. -cjc343");
		}
		tsm = tsc.new TimeShiftMessaging();// set up messaging strings now that config file is loaded
		loadStartupConfig();
	}

	private void readSettings() {
		Set<String> keys = startupConfig.getKeys(false);
		for (String key : keys) {
			String setting = startupConfig.getString(key + ".setting");//type defines what it does
			WorldSetting worldSetting = new WorldSetting();
			worldSetting.setLoopName(setting);
			worldSetting.setWorldName(key);
			worldSetting.setTid(-1);			
			world_settings.put(key, worldSetting);
		}
	}

	// write new setting to database or modify existing setting
	protected void persistentWriter(String cmdname, World w) {
		startupConfig.set(w.getName() + ".setting", cmdname);
		try {
			startupConfig.save(startupFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// schedules a timer for each world with an active loop setting.
	private void scheduleTimers() {
		for (World w : getServer().getWorlds()) {
			String wname = w.getName();
			WorldSetting setting = world_settings.get(wname);

			if (setting != null && setting.getLoopName() != "") {
				LoopSetting loopSetting = loop_settings.get(setting.getLoopName());
				if (loopSetting == null) {
					l.warning("[TimeShift] The startup setting for the world '" + wname + "' has been ignored because the setting '" + setting.getLoopName() + "' could not be found in the configuration.");
					continue;
				}
				int start = loopSetting.getStartTime(0);
				w.setTime(start);
				scheduleTimer(w, wname, setting, loopSetting);
			}
		}
	}
	
	//TODO Only change time if it must be for next period, figure out why riseset/setrise is so fucked
	
	//schedule timer of the future!
	protected void scheduleTimer(World world, LoopSetting loop_setting, int nextIdx, long diff) {
		// schedule a one off timer that reschedules for the next time (using this method).
		final TimeShiftShifter tss = new TimeShiftShifter();//set info for TSS to use
		tss.world = world;
		tss.plugin = this;
		tss.loop_setting = loop_setting;
		tss.currentIdx = nextIdx;
		tss.stopTime = loop_setting.getStopTime(nextIdx);
		tss.nextStartTime = loop_setting.getStartTime(loop_setting.getNextIdx(nextIdx));
		String wname = world.getName();//get world name and associated setting
		WorldSetting worldSetting = world_settings.get(wname);
		//cancel if the current task is still queued.
		if (getServer().getScheduler().isQueued(worldSetting.getTid())) {
			getServer().getScheduler().cancelTask(worldSetting.getTid());
		}
		int tid = -1;
		while (tid == -1) {
			tid = getServer().getScheduler().scheduleSyncDelayedTask(this, tss, diff);
		}
		worldSetting.setTid(tid);//(re)scheduled
		world_settings.put(wname, worldSetting);//save for cancellation purposes (world unload, etc)
	}
	//TODO keep or kill
	protected void scheduleTimer(World world, LoopSetting loop_setting, WorldSetting world_setting, int nextIdex, long diff) {
		
	}
	//way of the past? schedule repeating tasks to monitor time
	protected void scheduleTimer(World w, String wname, WorldSetting worldSetting, LoopSetting loopSetting) {
		if (classic) {//if you really wanted to do it this way
			w.setTime(loopSetting.getStartTime(0));
			final TimeShiftRunnable tsr = new TimeShiftRunnable();
			tsr.world = w; // set timer's world
			tsr.loop_setting = loopSetting;
			tsr.lastTime = w.getTime();
			tsr.stop = loopSetting.getStopTime(0);
			// schedule task
			if (worldSetting.getTid() > 0) {
				getServer().getScheduler().cancelTask(worldSetting.getTid());
			}//Set Task ID when task scheduled
			int tid = -1;
			while (tid == -1) {
				tid = getServer().getScheduler().scheduleSyncRepeatingTask(this, tsr, rate, rate);
			}
			worldSetting.setTid(tid); //rate is user configured, 20 = approx 1/sec 100 = every 5 sec.
			world_settings.put(wname, worldSetting);
		} else {// a good choice!
			world_settings.put(wname, worldSetting); // save what's been done so far because scheduleTimer will re-retrieve on next call.
			int start = loopSetting.getStartTime(0);
			w.setTime(start);
			scheduleTimer(w, loopSetting, 0, loopSetting.getStopTime(0) - start);
		}
	}
	// Schedule a timer after finding a loop setting
	protected void scheduleTimer(World w, String wname, WorldSetting setting) {
		scheduleTimer(w, wname, setting, loop_settings.get(setting.getLoopName()));
	}
	// cancels task and removes loop name setting from a WorldSetting. 
	protected void cancelShift(WorldSetting setting) {
		getServer().getScheduler().cancelTask(setting.getTid());//cancel task
		setting.setTid(-1);//set tid
		setting.setLoopName("");//remove loop name
		world_settings.put(setting.getWorldName(), setting);//overwrite old setting
	}
	
	//Want an API? and you made it this far... let me know.
//	// A public method for canceling a shift, not used internally.
//	public boolean cancelShift(String wname) {
//		WorldSetting setting = world_settings.get(wname);
//		if (setting != null && setting.getTid() != -1) {
//			cancelShift(setting);
//			return true;
//		}
//		return false;
//	}
//	// A public method for listing the loops, not used internally
//	public String[] getLoopNames() {
//		return loop_settings.keySet().toArray(new String[0]);
//	}
//	// A public method for starting a shift, not used internally.
//	public boolean startShift(String worldName, String loopName) {
//		World w = getServer().getWorld(worldName);
//		worldName = w.getName();
//		LoopSetting l = loop_settings.get(loopName);
//		if (l != null && w != null) {
//			WorldSetting ws = world_settings.get(worldName);
//			if (ws == null) {
//				ws = new WorldSetting();
//				ws.setWorldName(worldName);
//				ws.setLoopName(loopName);
//				ws.setTid(-1);
//			}
//			scheduleTimer(w, worldName, ws, l);
//			return true;
//		}
//		return false;
//	}
}