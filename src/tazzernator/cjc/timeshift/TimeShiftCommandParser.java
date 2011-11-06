package tazzernator.cjc.timeshift;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.World;

public class TimeShiftCommandParser {
	private TimeShift instance;
	private TimeShiftPersistentReaderWriter tsprw;
	// show up in a lot of places. Actual strings only shows up here.
	private final static String cmd = "shift";
	private final static String time = "time";
	public static final String cmdPerm = time + cmd + "." + cmd; // currently used in Player Listener
	public static final String cmdCancel = time + cmd + ".cancel"; // help uses for canceling shifts
	private final String cmdStart = time + cmd + ".startup"; // cannot cancel shifts via /shift stop commands,

	public TimeShiftCommandParser(TimeShift instance, TimeShiftPersistentReaderWriter tsprw) {
		this.instance = instance;
		this.tsprw = tsprw;
	}

	// ---------------------- setting persistent (in db) settings
	// this method parses single world or multi world commands that set the persistent startup state for a world
	private void setPersist(int setting, CommandSender sender, String[] split) {
		//System.out.println("TimeShift is parsing a persistent state command");
		if (split.length > 2) { // split length > 2, multi-world command (world names listed after command)
			for (int i = 2; i < split.length; i++) { // for each world listed
				World w = this.instance.getServer().getWorld(split[i]);// try to get a world for each worldname
				if (w != null) { // if a world by that name exists
					tsprw.persistentWriter(setting, w);// world exists, write persistent setting
					instance.tsm.sendMessage(sender, w, setting, true);// print result
				} else {
					instance.tsm.sendError(sender, 0, split[i]); // dne error
				}
			}
		} else {// command on current player world (no names listed)
			if (sender instanceof Player) {
				Player player = (Player) sender; // cast player in order to get player's current world
				tsprw.persistentWriter(setting, player.getWorld()); // write setting for player's world
				instance.tsm.sendMessage(sender, player.getWorld(), setting, true);// print result
			} else { // sender not a player (console) and must specify worldname(s)
				instance.tsm.sendError(sender, 3, "");// specify world from console error
			}
		}
	}

	// ------------------ setting temporary (in memory only) settings
	// this method parses single world or multi world commands that set the current state for a world
	// possible change: enum for states and remove all magic numbers, also, enum for errors+others in Messaging?
	private void setSetting(int setting, CommandSender sender, String[] split) {
		//System.out.println("TimeShift is parsing a current state command");
		if (split.length > 1) {// multi world
			for (int i = 1; i < split.length; i++) { // for each world listed
				World w = this.instance.getServer().getWorld(split[i]);
				if (w != null) { // if the world exists
					if (TimeShift.settings.put(w.getName(), setting) == null) { // set new setting, and if there was no previous setting:
						instance.scheduleTimer(w); // schedule a new timer if no previous setting
					}
					instance.tsm.sendMessage(sender, w, setting, false);// print result
				} else { // world doesn't exist
					instance.tsm.sendError(sender, 0, split[i]); // dne error
				}
			}
		} else {// single world, based on player info
			if (sender instanceof Player) {
				Player player = (Player) sender; // make sure we've got a player -- can't do single world from console\
				if (TimeShift.settings.put(player.getWorld().getName(), setting) == null) {// make change
					instance.scheduleTimer(player.getWorld());
				}
				instance.tsm.sendMessage(sender, player.getWorld(), setting, false);// print result
			} else { // console, not player
				instance.tsm.sendError(sender, 3, "");// specify world from console error
			}
		}
	}

	// checks if a player has permission to use a given permission node.
	// permission nodes are defined for player groups in the Permissions plugin configuration.
	// this method is designed to return true if Permissions is not installed on the server.
//removed when new permission methods made it unnecessary...

	// check if a player has permission to use shift commands
	public boolean checkShift(Player player) {
		if (player.hasPermission(cmdPerm))
			return true;
		return false;
	}

	// check if a player has permission to use startup commands
	public boolean checkStartup(Player player) {
		if (player.hasPermission(cmdStart))
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

	// this class parses shift commands and handles the different arguments possible for setting current behavior or startup behavior
	public boolean handleCommand(CommandSender sender, Command command, String commandLabel, String[] split) throws NullPointerException, ArrayIndexOutOfBoundsException {
		// get basic info needed
		String commandName = command.getName().toLowerCase(); // command used (currently always "shift")
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
					// if it didn't have args, it's a help request. check what permission level they have and display customized help.
					// if permissions isn't installed, checkPermissions returns true, so users will see "full help"
					// this can be optimized
					if (checkStartup(player) && checkShift(player)) {
						// has both
						instance.tsm.sendHelp(sender, 3); // send full help
					} else if (checkShift(player)) {
						// has shift only
						instance.tsm.sendHelp(sender, 1); // send shift help
					} else if (checkStartup(player)) {
						// has startup only
						instance.tsm.sendHelp(sender, 2); // send startup help
					} else { // has nothing
						instance.tsm.sendError(sender, 4, ""); // send no permissions error
					}
				} else { // not a player (console)
					instance.tsm.sendHelp(sender, 0); // send console help
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
					return false;
				}
			} catch (NullPointerException ex) {
			}

			// if player used command, check permissions
			if (isPlayer) {
				if (T1 == SubCommand.STARTUP) {// player wants to change startup settings, check startup permission
					if (!checkStartup(player)) {
						instance.tsm.sendError(sender, 2, ""); // send no startup permissions error
						return true;
					}
				} else {// player is not changing startup settings, check general permission
					if (!checkShift(player)) {
						instance.tsm.sendError(sender, 1, ""); // send no shift permissions error
						return true;
					}
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
				// permissions already checked
				// only got the startup command, send help
				if (split.length == 1) {
					instance.tsm.sendHelp(sender, 2); // send startup help
					return true;
				}
				// match 2nd argument to possible arguments enum
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
				case STARTUP: // startup is not a valid sub argument for the startup argument... 'shift startup startup'? Why would those silly users do that? :'( (Easter eggs are not localizable)
					sender.sendMessage("Why would you even want to try that? Are you trying to make the universe implode?");
					return false;
				default:
					break;
				}
				break;
			default:
				break;
			}
			// this return should never be reached. Help will be displayed if it is.
			return false;
		}

		// never returned true, also shouldn't be reached. may be reached if the user changes the plugin.yml file and removes the shift command.
		return false;
	}

	// Provides an enum value that matches a string, if possible. If not possible, returns null.
	// function must then check to see if it got null, in which case, string wasn't in enum.
	private static SubCommand asSubCmd(String str) {
		for (SubCommand t1 : SubCommand.values()) {// compare to values in enum
			if (t1.name().equalsIgnoreCase(str)) {
				return t1;
			}
		}
		return null;
	}

	// possible arguments to the shift command and sub arguments to the startup argument
	private enum SubCommand {
		DAY, NIGHT, STOP, STARTUP, SUNSET, SUNRISE, RISESET, SETRISE
	}

}
