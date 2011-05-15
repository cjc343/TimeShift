package tazzernator.cjc.timeshift;

import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.config.ConfigurationNode;

public class TimeShiftMessaging {

	private static TimeShift plugin;
	
	private static ConfigurationNode shift;
	private static ConfigurationNode cancel;
	private static ConfigurationNode startupShift;
	private static ConfigurationNode startupCancel;
	
	private static ConfigurationNode errors;
	private static ConfigurationNode help;
	
	private static String dest = "destination";
	
	//setup ConfigurationNodes from config file
	public static void setup(TimeShift plg) {
		plugin = plg;
		
		//remember all the important nodes
		ConfigurationNode stringSettings = plugin.getConfiguration().getNode("strings");
		shift = stringSettings.getNode("shift");
		cancel = stringSettings.getNode("cancel");
		ConfigurationNode startup = stringSettings.getNode("startup");
		startupShift = startup.getNode("shift");
		startupCancel = startup.getNode("cancel");
		errors = stringSettings.getNode("errors");
		help = stringSettings.getNode("help");
	}
	
	//called to send messages to anyone.
	public static void sendMessage(CommandSender cs, World w, int setting, boolean startup) {
		if (!startup) {
			if (setting == -1) {//a non-startup cancel
				String d = cancel.getString(dest);
				send(d, getCancelString(cs, w), cs, w);
			} else {//a non-startup shift
				String d = shift.getString(dest);
				send(d, getShiftString(cs, w, setting), cs, w);
			}
		} else {
			if (setting == -1) {//a startup cancel
				String d = startupCancel.getString(dest);
				send(d, getStartupCancelString(cs, w), cs, w);
			} else {//a startup shift
				String d = startupShift.getString(dest);
				send(d, getStartupShiftString(cs, w, setting), cs, w);
			}
		}
	}
	
	//called to send errors to anyone
	//error chosen based on int value.
	public static void sendError(CommandSender cs, int type, String wrld) {
		boolean log = errors.getBoolean("error-logging", true);
		String msg = "";
		if (type == 0) {
			msg = errors.getString("dne");
		} else if (type == 1) {
			msg = errors.getString("shift-permission");
		} else if (type == 2) {
			msg = errors.getString("startup-permission");
		} else if (type == 3) {
			msg = errors.getString("console-specify");
		} else if (type == 4) {
			msg = errors.getString("no-perm");
		}
		msg = parseVars(msg, cs, wrld, -5); // parse anything but setting... which shouldn't apply here
		cs.sendMessage(msg);
		if (log) {//log error to console if logging is on.
			if (cs instanceof Player) {
				System.out.println(((Player)cs).getName() + " receieved this error: " + msg + " from " + TimeShift.name);
			}
		}
	}
	
	//called to send help to anyone
	//help type chosen based on int value
	public static void sendHelp(CommandSender cs, int type) {
		if (type == 3) {
			cs.sendMessage(help.getString("shift-startup"));
		} else if (type == 0) {
			cs.sendMessage(help.getString("console"));
		} else if (type == 1) {
			cs.sendMessage(help.getString("shift-only"));
		} else if (type == 2) {
			cs.sendMessage(help.getString("startup-only"));
		}
	}
		
	//called by sendMessage to send to proper destination
	private static void send(String d, String msg, CommandSender cs, World w) {
		if (msg == "") {
			return;
		}
		if (d.equals("player")) {
			cs.sendMessage(msg);//send to sender
		} else if (d.equals("server-announce")) {
			plugin.getServer().broadcastMessage(msg);//send to server
			System.out.println(msg);//and to console
		} else if (d.equals("world-announce")) {
			for (Player p : w.getPlayers()) {//send to all players in a world
				p.sendMessage(msg);
			}
			System.out.println(msg);//send to console
		}
	}
	
	//returns the parsed custom shift string
	private static String getShiftString(CommandSender p, World w, int setting) {
		return parseString(shift, p, w, setting);
	}
	
	//returns the parsed custom cancel string
	private static String getCancelString(CommandSender p, World w) {
		return parseString(cancel, p, w, -1);		
	}
	
	//returns the parsed custom startup shift string
	private static String getStartupShiftString(CommandSender p, World w, int setting) {
		return parseString(startupShift, p, w, setting);
	}
	
	//returns the parsed custom startup cancel string
	private static String getStartupCancelString(CommandSender p, World w) {
		return parseString(startupCancel, p, w, -1);
	}
	
	//parses setting values into human readable strings
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
	
	//retrieves custom string and returns the parsed version
	private static String parseString(ConfigurationNode cn, CommandSender p, World w, int setting) {
		String s = cn.getString("string");
		return parseVars(s, p, w.getName(), setting);
	}
	
	//parses the %world %setting and %player variables.
	private static String parseVars(String s, CommandSender p, String w, int setting) {
		s = s.replaceAll("%world", w);//replace with world name or attempted world name
		s = s.replaceAll("%setting", parseSetting(setting));//replace with valid, non-'stop' setting
		if (p instanceof Player) {//replace with playername or console
			s = s.replaceAll("%player", ((Player)p).getName());
		} else {
			s = s.replaceAll("%player", "The Console");
		}
		return s;
	}
}
