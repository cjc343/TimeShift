package tazzernator.cjc.timeshift;

import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.config.ConfigurationNode;

public class TimeShiftMessaging {

	private static TimeShift plugin;

	// primarily store easily localizable message strings
	private static ConfigurationNode shift;
	private static ConfigurationNode cancel;
	private static ConfigurationNode startupShift;
	private static ConfigurationNode startupCancel;

	private static ConfigurationNode errors;
	private static ConfigurationNode help;
	// a very common shared node that controls who gets a message
	private static String dest = "destination";

	// setup ConfigurationNodes from config file
	public static void setup(TimeShift plg) {
		plugin = plg;

		// remember all the important nodes. start by getting top level node
		ConfigurationNode stringSettings = plugin.getConfiguration().getNode("strings");
		// get sub node for shift messages
		shift = stringSettings.getNode("shift");
		// sub node for cancel messages
		cancel = stringSettings.getNode("cancel");
		// sub node for startup messages
		ConfigurationNode startup = stringSettings.getNode("startup");
		// sub-sub node for startup shift messages
		startupShift = startup.getNode("shift");
		// sub-sub node for startup cancel messages
		startupCancel = startup.getNode("cancel");
		// sub node for error messages
		errors = stringSettings.getNode("errors");
		// sub node for help messages
		help = stringSettings.getNode("help");
	}

	// called to send messages to anyone.
	// sends messages concerning changes to current 'shift' state
	public static void sendMessage(CommandSender cs, World w, int setting, boolean startup) {
		if (!startup) { // if not a startup message
			if (setting == -1) {// a non-startup cancel message
				String d = cancel.getString(dest); // get the cancel message
				send(d, getCancelString(cs, w), cs, w); // send the message with variables parsed
			} else {// a non-startup shift message
				String d = shift.getString(dest); // get the shift message
				send(d, getShiftString(cs, w, setting), cs, w); // send the cancel message with variables parsed
			}
		} else { // a startup message
			if (setting == -1) {// a startup cancel
				String d = startupCancel.getString(dest);
				send(d, getStartupCancelString(cs, w), cs, w);
			} else {// a startup shift
				String d = startupShift.getString(dest);
				send(d, getStartupShiftString(cs, w, setting), cs, w);
			}
		}
	}

	// called to send errors to anyone
	// error chosen based on int value. (future: enum?)
	public static void sendError(CommandSender cs, int type, String wrld) {
		boolean log = errors.getBoolean("error-logging", true); // get logging status. only needs to be done once. move out of sendError.
		String msg = "";
		if (type == 0) { // world doesn't exist
			msg = errors.getString("dne");
		} else if (type == 1) { // lacking timeshift.shift permission node
			msg = errors.getString("shift-permission");
		} else if (type == 2) { // lacking timeshift.startup permission node
			msg = errors.getString("startup-permission");
		} else if (type == 3) { // world not specified in console command
			msg = errors.getString("console-specify");
		} else if (type == 4) { // lacking all permissions
			msg = errors.getString("no-perm");
		}
		msg = parseVars(msg, cs, wrld, -5); // parse anything but setting... which shouldn't apply here
		cs.sendMessage(msg); // send error to user of command
		if (log) {// log error to console if logging is on.
			if (cs instanceof Player) {
				System.out.println(((Player) cs).getName() + " receieved this error: " + msg + " from " + TimeShift.name);
			}
		}
	}

	// called to send help to anyone
	// help type chosen based on int value (future: enum?)
	public static void sendHelp(CommandSender cs, int type) {
		if (type == 3) { // full help
			cs.sendMessage(help.getString("shift-startup"));
		} else if (type == 0) { // console help
			cs.sendMessage(help.getString("console"));
		} else if (type == 1) { // shift help
			cs.sendMessage(help.getString("shift-only"));
		} else if (type == 2) { // startup help
			cs.sendMessage(help.getString("startup-only"));
		}
	}

	// called by sendMessage to send to proper destination
	private static void send(String d, String msg, CommandSender cs, World w) {
		if (msg == "") {
			return;
		}
		// check who to broadcast message to: player, server, or world
		if (d.equals("player")) {
			cs.sendMessage(msg);// send to sender (could also be console)
		} else if (d.equals("server-announce")) {
			plugin.getServer().broadcastMessage(msg);// send to server (all worlds)
			System.out.println(msg);// and to console
		} else if (d.equals("world-announce")) {
			for (Player p : w.getPlayers()) {// send to all players in effected world
				p.sendMessage(msg);
			}
			System.out.println(msg);// send to console
		}
	}

	// returns the parsed custom shift string
	private static String getShiftString(CommandSender p, World w, int setting) {
		return parseString(shift, p, w, setting);
	}

	// returns the parsed custom cancel string
	private static String getCancelString(CommandSender p, World w) {
		return parseString(cancel, p, w, -1);
	}

	// returns the parsed custom startup shift string
	private static String getStartupShiftString(CommandSender p, World w, int setting) {
		return parseString(startupShift, p, w, setting);
	}

	// returns the parsed custom startup cancel string
	private static String getStartupCancelString(CommandSender p, World w) {
		return parseString(startupCancel, p, w, -1);
	}

	// parses setting values into human readable strings
	private static String parseSetting(int setting) {
		if (setting == 0) {
			return "day";
		} else if (setting == 13800) {
			return "night";
		} else if (setting == 12000) {
			return "sunset";
		} else if (setting == 22000) {
			return "sunrise";
		} else if (setting == -2) {
			return "sunrise and sunset";
		}
		return "";
	}

	// retrieves custom string and returns the parsed version
	private static String parseString(ConfigurationNode cn, CommandSender p, World w, int setting) {
		String s = cn.getString("string");
		return parseVars(s, p, w.getName(), setting);
	}

	// parses the %world %setting and %player variables.
	// should eventually parse color codes too if necessary.
	private static String parseVars(String s, CommandSender p, String w, int setting) {
		s = s.replaceAll("%world", w);// replace with world name or attempted world name
		s = s.replaceAll("%setting", parseSetting(setting));// replace with valid, non-'stop' setting
		if (p instanceof Player) {// replace with playername or console
			s = s.replaceAll("%player", ((Player) p).getName());
		} else {
			s = s.replaceAll("%player", "The Console");
		}
		return s;
	}
}
