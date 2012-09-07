package tazzernator.cjc.timeshift;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import tazzernator.cjc.timeshift.settings.LoopSetting;

class TimeShiftConfiguration {
	private TimeShift plugin;
	static FileConfiguration config;
	ConfigurationSection configuration;
	String ver = "config-version";
	String dt = "detect-time";
	String pr = "poll-rate";

	protected TimeShiftConfiguration(TimeShift instance) {
		plugin = instance;
		config = plugin.getConfig();
		configuration = config.getConfigurationSection("configuration");
		if (config.getConfigurationSection("commands").getKeys(false).size() == 0) {
			File configFile = new File(TimeShift.path, "/config.yml");
			File oldConfig = new File(TimeShift.path, "/config.yml.old");
			int i = 0;
			while (oldConfig.exists()) {
				oldConfig = new File(TimeShift.path, "/config.yml.old." + i++);// 0 and up
			}
			configFile.renameTo(oldConfig);
			plugin.l.warning("[TimeShift] Your config.yml has been saved as config.yml.old to preserve any comments.");
			plugin.l.warning("[TimeShift] Your config.yml is being updated to the a new version.");
			plugin.l.warning("[TimeShift] It is recommended that you delete the config, allow the default to be generated, and then re-apply your customizations.");
			plugin.l.warning("[TimeShift] Doing so will keep helpful comments inside the config.");
			config.options().copyDefaults(true);
			plugin.saveConfig();
		}
	}

	protected Boolean detectTime() {
		return configuration.getBoolean("detect-time");
	}

	protected int getRate() {
		return configuration.getInt("classic." + pr);
	}

	public boolean classicTime() {
		return configuration.getBoolean("classic.on");
	}

	private void cmdErr(String s, String key) {// Generic error that causes a command to not be parsed.
		plugin.l.warning("[" + TimeShift.name + "] The shift command '" + key + "' was skipped because " + s);
	}

	private String getString(String path, ConfigurationSection config) {
		if (config.contains(path)) {
			return config.getString(path).toLowerCase();
		} else {
			return "";
		}
	}

	protected boolean readLoops() {// Read in commands and register them
		ConfigurationSection commands = config.getConfigurationSection("commands");
		Set<String> keys = commands.getKeys(false);
		if (keys.size() == 0) {
			keys = commands.getDefaultSection().getKeys(false);
		}
		for (String key : keys) {// for each command defined
			String keyType = getString(key + ".type", commands);// type defines what it does

			if (keyType.equals("loop")) {// common type
				parseLoop(commands, key);// parse the loop command and register the loop type
			} else if (keyType.equals("startup")) {// startup setting identifier(s)
				readAliases(commands.getConfigurationSection(key), key, "TS STARTUP");// input commands with special value
			} else if (keyType.equals("stop")) {// cancel loop command
				readAliases(commands.getConfigurationSection(key), key, "TS STOP");
			} else {// fallback is loop with message.
				plugin.l.info("[TimeShift] The command '" + key + "' has an incorrect or missing type and is assumed to be a loop.");
				parseLoop(commands, key);
			}
		}
		return false;
	}

	private void parseLoop(ConfigurationSection commands, String key) {// parses a command
		ConfigurationSection command = commands.getConfigurationSection(key);
		if (command.contains("times")) {
			List<Integer> timeList = command.getIntegerList("times");// The times to loop
			int size = timeList.size();// must be divisible by 2
			if (size % 2 != 0) {
				cmdErr("it must have an even number of times.", key);
				readAliases(command, key, "TS ERR");// register command as a special case error.
				return;
			}// because we'll split them into sets of two, and we can't have any incomplete sets. Too bad sequence of mappings didn't fly so well, would have made for clearer, easier config.
			int[][] settings = new int[size / 2][2];
			int i = 0, x, y, z = -1;// check for mismatched pairs
			for (; i < size; i++) {
				x = timeList.get(i);
				y = timeList.get(++i);// get the next index after incrementing i.
				if (x > y) {// failure case
					cmdErr(x + " > " + y + ". You may have mismatched start/stop time pairs, or you may be trying to do: " + x + " -> 24000, 0 -> " + y + " which will create a loop that skips the time between those two numbers.", key);
					readAliases(command, key, "TS ERR");// register as error
					return;// don't parse a command with incorrect times.
				}
				settings[++z][0] = x;// the test for which operator is which is the initialization to -1.
				settings[z][1] = y;// save the pair
			}
			// save settings
			TimeShift.loop_settings.put(key, new LoopSetting(key, settings));
			// register commands
			readAliases(command, key);
		} else {
			cmdErr("it does not contain a 'times' key.", key);
			readAliases(command, key, "TS ERR");// register command as a special case error.
		}
	}

	private void checkAlias(String command, String alias, String newCommand) {
		if (command != null) {
			plugin.l.warning("[TimeShift] The alias '" + alias + "' to the command '" + command + "' is being changed to the command '" + newCommand + "' because '" + alias + "' was found multiple times.");
		}
	}

	private void readAliases(ConfigurationSection command, String cmdName) {// registers commands and their aliases to a command name
		checkAlias(TimeShift.command_aliases.put(cmdName.toLowerCase(), cmdName), cmdName, cmdName);// register command
		if (command.contains("aliases")) {
			List<String> aliases = command.getStringList("aliases");// get the list of aliases
			for (String e : aliases) {// register aliases
				checkAlias(TimeShift.command_aliases.put(e.toLowerCase(), cmdName), e, cmdName);
			}
		} else {
			plugin.l.info("[TimeShift] The aliases section was ignored or not present for the '" + cmdName + "' command.");
		}
	}

	private void readAliases(ConfigurationSection command, String key, String cmdName) {// registers commands and their aliases to a command name
		checkAlias(TimeShift.command_aliases.put(key.toLowerCase(), cmdName), cmdName, key);// register command
		if (command.contains("aliases") && cmdName != "TS ERR") {
			List<String> aliases = command.getStringList("aliases");// get the list of aliases
			for (String e : aliases) {// register aliases
				checkAlias(TimeShift.command_aliases.put(e.toLowerCase(), cmdName), e, key);
			}
		} else if (cmdName != "TS ERR") {
			plugin.l.info("[TimeShift] The aliases section was ignored or not present for the '" + key + "' command.");
		}
	}

	protected boolean colorizeStrings() {
		return configuration.getBoolean("colorize-strings");
	}

	protected class TimeShiftMessaging {
		ConfigurationSection strings = config.getConfigurationSection("strings");
		ConfigurationSection help = strings.getConfigurationSection("help");
		ConfigurationSection errors = strings.getConfigurationSection("errors");
		// strings:
		String sh = "shift.";
		String ca = "cancel.";
		String st = "startup.";

		String str = "string";
		String dest = "destination";
		boolean colorize;
		
		protected TimeShiftMessaging() {
			colorize = colorizeStrings();
		}

		// called to send messages to anyone.
		// sends messages concerning changes to current 'shift' state
		protected void sendMessage(CommandSender cs, String worldName, String setting, boolean startup) {
			if (!startup) { // if not a startup message
				if (setting == "") {// a non-startup cancel message
					String d = strings.getString(ca + dest); // get the cancel message
					send(d, getCancelString(cs, worldName), cs, worldName); // send the message with variables parsed
				} else {// a non-startup shift message
					String d = strings.getString(sh + dest); // get the shift message
					send(d, getShiftString(cs, worldName, setting), cs, worldName); // send the cancel message with variables parsed
				}
			} else { // a startup message
				if (setting == "") {// a startup cancel
					String d = strings.getString(st + ca + dest);
					send(d, getStartupCancelString(cs, worldName), cs, worldName);
				} else {// a startup shift
					String d = strings.getString(st + sh + dest);
					send(d, getStartupShiftString(cs, worldName, setting), cs, worldName);
				}
			}
		}

		// called to send errors to anyone
		// error chosen based on int value. (future: enum?)
		protected void sendError(CommandSender cs, ErrorType type, String worldName) {
			boolean log = errors.getBoolean("error-logging"); // get logging status. only needs to be done once. move out of sendError.
			String msg = "";
			if (type == ErrorType.WORLD_DNE) { // world doesn't exist
				msg = errors.getString("dne");
			} else if (type == ErrorType.SHIFT_PERM) { // lacking timeshift.shift permission node
				msg = errors.getString("shift-permission");
			} else if (type == ErrorType.START_PERM) { // lacking timeshift.startup permission node
				msg = errors.getString("startup-permission");
			} else if (type == ErrorType.CONSOLE_WORLD_NEEDED) { // world not specified in console command
				msg = errors.getString("console-specify");
			} else if (type == ErrorType.NO_PERMS) { // lacking all permissions
				msg = errors.getString("no-perm");
			} else if (type == ErrorType.STOP_PERM) {
				msg = errors.getString("stop-permission");// lacking timeshift.cancel permission node
			}
			msg = parseVars(msg, cs, worldName, ""); // parse anything but setting... which shouldn't apply here
			cs.sendMessage(msg); // send error to user of command
			if (log) {// log error to console if logging is on.
				if (cs instanceof Player) {
					plugin.l.info(((Player) cs).getName() + " receieved this error: " + msg + " from " + TimeShift.name);
				}
			}
		}

		// called to send help to anyone
		// help type chosen based on int value (future: enum?)
		protected void sendHelp(CommandSender cs, HelpType type) {
			if (type == HelpType.FULL) { // full help
				cs.sendMessage(help.getString("shift-startup"));
			} else if (type == HelpType.CONSOLE) { // console help
				cs.sendMessage(help.getString("console"));
			} else if (type == HelpType.SHIFT) { // shift help
				cs.sendMessage(help.getString("shift-only"));
			} else if (type == HelpType.STARTUP) { // startup help
				cs.sendMessage(help.getString("startup-only"));
			}
		}

		// called by sendMessage to send to proper destination
		private void send(String d, String msg, CommandSender cs, String worldName) {
			if (msg == "") {
				return;
			}
			// check who to broadcast message to: player, server, or world
			if (d.equals("player")) {
				cs.sendMessage(msg);// send to sender (could also be console)
				plugin.l.info(msg);//log to console
			} else if (d.equals("server-announce")) {
				plugin.getServer().broadcastMessage(msg);// send to server (all worlds + console)
			} else if (d.equals("world-announce")) {
				for (Player p : plugin.getServer().getWorld(worldName).getPlayers()) {// send to all players in effected world
					p.sendMessage(msg);
				}
				plugin.l.info(msg);//log to console
			}
		}

		// returns the parsed custom shift string
		private String getShiftString(CommandSender p, String worldName, String setting) {
			return parseString((sh), p, worldName, setting);
		}

		// returns the parsed custom cancel string
		private String getCancelString(CommandSender p, String worldName) {
			return parseString((ca), p, worldName, "");
		}

		// returns the parsed custom startup shift string
		private String getStartupShiftString(CommandSender p, String worldName, String setting) {
			return parseString((st + sh), p, worldName, setting);
		}

		// returns the parsed custom startup cancel string
		private String getStartupCancelString(CommandSender p, String worldName) {
			return parseString((st + ca), p, worldName, "");
		}

		// retrieves custom string and returns the parsed version
		private String parseString(String cn, CommandSender p, String worldName, String setting) {
			String s = strings.getString(cn + "string");
			return parseVars(s, p, worldName, setting);
		}

		// parses the %world %setting and %player variables.
		// should eventually parse color codes too if necessary.
		private String parseVars(String s, CommandSender p, String worldName, String setting) {
			s = s.replaceAll("%world", worldName);// replace with world name or attempted world name
			s = s.replaceAll("%setting", setting);// replace with valid, non-'stop' setting
			if (p instanceof Player) {// replace with playername or console
				s = s.replaceAll("%player", ((Player) p).getName());
			} else {
				s = s.replaceAll("%player", "The Console");
			}
			return colorize(s);
		}

		private String colorize(String s) {
			if (colorize) {
				s = s.replaceAll("&0", ChatColor.getByChar('0').toString())
					 .replaceAll("&1", ChatColor.getByChar('1').toString())
					 .replaceAll("&2", ChatColor.getByChar('2').toString())
					 .replaceAll("&3", ChatColor.getByChar('3').toString())
					 .replaceAll("&4", ChatColor.getByChar('4').toString())
					 .replaceAll("&5", ChatColor.getByChar('5').toString())
					 .replaceAll("&6", ChatColor.getByChar('6').toString())
					 .replaceAll("&7", ChatColor.getByChar('7').toString())
					 .replaceAll("&8", ChatColor.getByChar('8').toString())
					 .replaceAll("&9", ChatColor.getByChar('9').toString())
					 .replaceAll("&a", ChatColor.getByChar('a').toString())
					 .replaceAll("&b", ChatColor.getByChar('b').toString())
					 .replaceAll("&c", ChatColor.getByChar('c').toString())
					 .replaceAll("&d", ChatColor.getByChar('d').toString())
					 .replaceAll("&e", ChatColor.getByChar('e').toString())
					 .replaceAll("&f", ChatColor.getByChar('f').toString());
			}
			return s;
		}
	}

	// Help types, need to fix help output.
	protected enum HelpType {
		CONSOLE, SHIFT, STARTUP, FULL
	}

	// Error types with semi-descriptive names
	protected enum ErrorType {
		WORLD_DNE, SHIFT_PERM, START_PERM, CONSOLE_WORLD_NEEDED, NO_PERMS, STOP_PERM
	}
}