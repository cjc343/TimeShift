package tazzernator.cjc.timeshift;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.World;

public class TimeShiftCommandParser {
	private TimeShift instance;
	// show up in a lot of places. Actual string only shows up here. Some places still need adjustment
	private final static String cmd = "shift";
	private final static String time = "time";
	public static final String cmdPerm = time + cmd + "." + cmd; // currently used in Player Listener
	public static final String cmdCancel = time + cmd + ".cancel"; // help uses for canceling shifts
	private final String cmdStart = time + cmd + ".startup"; // cannot cancel shifts via /shift stop commands,

	private final String wrld = "World [";
	private final String will = "] will loop ";
	private final String wont = "] will not loop ";
	private final String dne = "] doesn't exist.";
	private final String onStart = " on startup";
	public static final String norm = "The " + time + " appears to be back to normal on [";
	private final String suddenShift = "The " + time + " suddenly " + cmd + "s on [";
	private final String sp = " ";
	private final String sep = " | ";
	private final String sl = "/";
	public static final String clbr = "]";

	private final String day = "day";
	private final String night = "night";
	private final String sunrise = "sunrise";
	private final String sunset = "sunset";
	private final String stop = "stop";
	private final String setrise = "sunrise and sunset";
	
	private final String use = "Usage: ";
	private final String need = "You need ";
	private final String perm = " permission.";
	private final String specWorld = need + "to specify a world when using the console.";
	
	private final String opts = sp + day + sep + night + sep + stop + sep + sunrise + sep + sunset + sep + setrise;

	public TimeShiftCommandParser(TimeShift instance) {
		this.instance = instance;
	}

	// ---------------------- setting persistent (in file) settings
	private void setPersist(int setting, CommandSender sender, String[] split) {
		if (split.length > 2) { // split length > 2, multi-world command
			for (int i = 2; i < split.length; i++) {
				World w = this.instance.getServer().getWorld(split[i]);// try to get a world for each worldname
				if (w != null) {
					TimeShiftFileReaderWriter.persistentWriter(setting, w);// world exists, write persistent setting
					printPersist(setting, sender, w); // print result
				} else {
					sender.sendMessage(wrld + split[i] + dne); // world name doesn't exist
				}
			}
		} else {// command on current player world
			if (sender instanceof Player) {
				Player player = (Player) sender;
				TimeShiftFileReaderWriter.persistentWriter(setting, player.getWorld());
				printPersist(setting, sender, player.getWorld());
			} else {
				sender.sendMessage(specWorld);
			}
		}
	}

	// send notice to user of startup change.
	private void printPersist(int setting, CommandSender player, World w) {
		if (setting == 0) {
			player.sendMessage(wrld + w.getName() + will + day + onStart);
		} else if (setting == -1) {
			player.sendMessage(wrld + w.getName() + wont + onStart);
		} else if (setting == -2) {
			player.sendMessage(wrld + w.getName() + will + setrise + onStart);
		} else if (setting == 12000) {
			player.sendMessage(wrld + w.getName() + will + sunset + onStart);
		} else if (setting == 22000) {
			player.sendMessage(wrld + w.getName() + will + sunrise + onStart);
		} else {
			player.sendMessage(wrld + w.getName() + will + night + onStart);
		}
	}

	// ------------------ setting temporary (in memory only) settings
	private void setSetting(int setting, CommandSender sender, String[] split) {
		if (split.length > 1) {// multi world
			for (int i = 1; i < split.length; i++) {
				if (setSettingByName(setting, split[i])) {
					if (setting == -1) {
						instance.getServer().broadcastMessage(norm + split[i] + clbr);
					} else {
						instance.getServer().broadcastMessage(suddenShift + split[i] + clbr);
					}
				} else {
					sender.sendMessage(wrld + split[i] + dne);
				}
			}
		} else {// single world
			if (sender instanceof Player) {
				Player player = (Player) sender;
				if (setting == -1) {
					instance.getServer().broadcastMessage(norm + player.getWorld().getName() + clbr);
				} else {
					instance.getServer().broadcastMessage(suddenShift + player.getWorld().getName() + clbr);
				}
				setSettingPlayer(setting, player);
			} else {
				sender.sendMessage(specWorld);
			}
		}
	}

	// changes one of the worlds in memory.
	private void setSettingPlayer(int setting, Player player) {
		World w = instance.getServer().getWorld(player.getWorld().getName());
		// if the previous value at key was null, the world never got a timer.
		if (TimeShift.settings.put(w.getName(), setting) == null) {
			instance.scheduleTimer(w);
		}
	}

	// same, by worldname instead of by player.
	private Boolean setSettingByName(int setting, String world) {
		World w = this.instance.getServer().getWorld(world);
		if (w == null) {
			return false;
		} else {
			// if the previous value at key was null, the world never got a
			// timer.
			if (TimeShift.settings.put(w.getName(), setting) == null) {
				instance.scheduleTimer(w);
			}
			return true;
		}
	}

	// checks if a player has permission to use permission.
	public boolean checkPermissions(Player p, String permission) {
		if (TimeShift.Permissions != null) { // if permissions are defined
			if (!TimeShift.Permissions.getHandler().has(p, permission)) { // and player has no permission
				return false;
			}
		}
		// Permissions not defined, or player has permission
		return true;
	}

	public boolean checkShift(Player player) {
		if (checkPermissions(player, cmdPerm))
			return true;
		return false;
	}

	private boolean checkShiftError(Player player) {
		if (checkShift(player))
			return false;
		player.sendMessage(need + cmdPerm + perm);
		return true;
	}

	public boolean checkStartup(Player player) {
		if (checkPermissions(player, cmdStart))
			return true;
		return false;
	}

	// feb 17th and 18th
	// reworked to be 'handleCommand' and to not handle /time, only /shift commands, which are claimed in yml.
	// this has caused this class to part into a commandparser and a playerlistener.
	// the player listener handles only the /time command, and hopefully does so in a way that does not
	// interfere with other plugins.

	// also, general fixes.
	// moved permission checking into a function, simplified setSetting, simplified world-finding
	// changed from vectors to mapping world names to (int) values.

	// feb. 5 2011
	// mostly rewritten to do switch/case instead of if/else
	// also added new functionality:
	// --optional Permissions support
	// --Ability to set an on server-load/server-reload default

	public boolean handleCommand(CommandSender sender, Command command, String commandLabel, String[] split) throws NullPointerException, ArrayIndexOutOfBoundsException {
		// get basic info about
		String commandName = command.getName().toLowerCase();
		// String[] split = args;
		// cast sender to player, check if it 'worked' and return if it didn't.
		Player player;
		Boolean isPlayer;
		if (sender instanceof Player) {
			player = (Player) sender;
			isPlayer = true;
		} else {
			player = null;
			isPlayer = false;
		}

		/*
		 * Depending on the player's command, the number in the temp file is changed. The number is checked by the Time class every second or so to determine what the time should be in the server.
		 */
		//
		if (commandName.equals(cmd)) {
			// shift command used, only checking for future ability to. right now, it'd be assumed.

			if (split.length == 0) {
				if (isPlayer) {
					// if it didn't have args, it's a help request. check what permission level they have and display customized permissions.
					// if permissions isn't installed, checkPermissions returns true, so users will see "full help"
					if (checkStartup(player) && checkShift(player)) {
						// has both
						player.sendMessage(use + sl + cmd + opts + sep + "startup <x>");
					} else if (checkShift(player)) {
						// has shift only
						player.sendMessage(use + sl + cmd + opts);
					} else if (checkStartup(player)) {
						// has startup only
						player.sendMessage(use + sl + cmd + " startup [" + opts + clbr + " -- sets startup and " + sl + "reload behavior only.");
					} else { // has nothing
						player.sendMessage(need + perm);
						System.out.println(player.getName() + " tried to use the " + cmd + " command, but does not have" + perm);
					}
				} else {
					sender.sendMessage(use + cmd + opts + " <worlds>");
				}
				// command handled
				return true;
			}

			// length was at least 1.
			// match a string to enum.
			SubCommand T1 = null;
			try {
				// try to match first argument to argument list
				T1 = asSubCmd(split[0]);
				if (T1 == null) {
					// this means a false 1st argument was used.
					// returning false displays the "full" help from the uml file.
					// the /time command is not included under its own command so that bukkit won't touch it.
					return false;
				}
			} catch (NullPointerException ex) {
			}
			
			if (isPlayer) {
				if (T1 == SubCommand.STARTUP) {
					if (!checkStartup(player)) {
						player.sendMessage(need + cmdStart + perm);
						return true;
					}
				} else {
					if (checkShiftError(player))
						return true; // displays a "need permission" message. May be a good thing to log too... if anyone cared...
				}
			}

			switch (T1) {
			// self explanatory
			case DAY:
				setSetting(0, sender, split);
				return true;
			case NIGHT:
				setSetting(13800, sender, split);
				return true;
			case STOP:
				setSetting(-1, sender, split);
				return true;
			case SUNSET:
				setSetting(12000, sender, split);
				return true;
			case SUNRISE:
				setSetting(22000, sender, split);
				return true;
			case RISESET:
			case SETRISE:
				setSetting(-2, sender, split);
				return true;
			case STARTUP:
				// if case is startup, check for permissions.
//				if (!checkStartup(player)) {
//					player.sendMessage("You need " + cmdStart + " permission.");
//					return true;
//				}
				// only got the startup command
				if (split.length == 1) {
					sender.sendMessage("Startup " + use + sl + cmd + " startup [" + opts + "]\n" + sl + cmd + " startup day -- automatically loop day when the server\nis " + sl + "reload 'ed or restarted.");
					return true;
				}
				// trying to get a string to match enum value, null if not matched
				SubCommand T2 = null;
				try {
					T2 = asSubCmd(split[1]);
					if (T2 == null) {
						// improper usage, displays full help
						return false;
					}
				} catch (NullPointerException ex) {
				}
				// self explanatory.
				// add multi-world-persistent-set.
				// already added for setSetting
				// now add for persistentWriter
				switch (T2) {
				case DAY:
					setPersist(0, sender, split);
					return true;
				case NIGHT:
					setPersist(13800, sender, split);
					return true;
				case STOP:
					setPersist(-1, sender, split);
					return true;
				case SUNRISE:
					setPersist(22000, sender, split);
					return true;
				case SUNSET:
					setPersist(12000, sender, split);
					return true;
				case RISESET:
				case SETRISE:
					setPersist(-2, sender, split);
					return true;
				case STARTUP:
					sender.sendMessage("Why would you even want to try that? Are you trying to make the universe implode?");
					return false;
				default:
					break;
				}
				break;
			default:
				break;
			}
			// this return should never be reached
			return false;
		}

		// } else {
		// // has neither permission
		// player.sendMessage("You don't have permission to use /" + cmd + ".");
		// return true;
		// }
		// never returned true, also shouldn't be reached. may be reached if the user changes the yml file.
		return false;
	}

	// should be renamed, along with the enum. Provides an enum value that matches a string, if possible. If not possible, returns null.
	// function must then check to see if it got null, in which case, string wasn't in enum.
	private static SubCommand asSubCmd(String str) {
		for (SubCommand t1 : SubCommand.values()) {
			if (t1.name().equalsIgnoreCase(str)) {
				return t1;
			}
		}
		return null;
	}

	// actually kinda TierAll. 2nd tier is startup, and I just parsed the extra.
	private enum SubCommand {
		DAY, NIGHT, STOP, STARTUP, SUNSET, SUNRISE, RISESET, SETRISE
	}

}
