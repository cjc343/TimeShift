package tazzernator.cjc.timeshift;

import java.util.ArrayList;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Server;
import org.bukkit.World;

public class TimeShiftCommandParser  { //extends PlayerListener
	private Server server;
	private TimeShift instance;
	ArrayList<String> data = new ArrayList<String>();

	
	public TimeShiftCommandParser(TimeShift instance, Server server) {
		this.server = server;
		this.instance = instance;
	}

	
	// changes one of the servers in memory.
	private void setSetting(int setting, Player player) {
		World w = server.getWorld(player.getWorld().getName());
		//if the previous value at key was null, the world never got a timer.
		if (TimeShift.settings.put(w.getName(), setting) == null) {
			instance.scheduleTimer(w);
		}
	}

	
	//checks if a player has permission to use permission.
	public boolean checkPermissions(Player p, String permission) {
		if (TimeShift.Permissions != null) {
			if (!TimeShift.Permissions.has(p, permission)) {
				return false;
			}
		}		
		return true;
	}
	
	public boolean checkShift(Player player){
		if (!checkPermissions(player, "timeshift.shift")) {
			return false;	
		}
		return true;
	}
	
	private boolean checkShiftError(Player player) {
		if (!checkShift(player)) {
			player.sendMessage("You need timeshift.shift permission.");
			return true;	
		}
		return false;
	}
	
	public boolean checkStartup(Player player){
		if (!checkPermissions(player, "timeshift.startup")) {
			return false;	
		}
		return true;
	}
	//feb 17th and 18th
	//reworked to be 'handleCommand' and to not handle /time, only /shift commands, which are claimed in yml.
	//this has caused this class to part into a commandparser and a playerlistener.
	//the player listener handles only the /time command, and hopefully does so in a way that does not
	//interfere with other plugins. It may not be triggered still if another plugin claims /time.
	
	//also, general fixes.
	//moved permission checking into a function, simplified setSetting, simplified world-finding
	//changed from vectors to mapping world names to (int) values.
	
	// feb. 5 2011
	// mostly rewritten to do switch/case instead of if/else
	// also added new functionality:
	// --optional Permissions support
	// --Ability to set an on server-load/server-reload default
	
	public boolean handleCommand(CommandSender sender, Command command, String commandLabel, String[] split) throws NullPointerException,ArrayIndexOutOfBoundsException {
	//get basic info about 
        String commandName = command.getName().toLowerCase();
	//	String[] split = args;
        //cast sender to player, check if it 'worked' and return if it didn't.
		Player player = (Player) sender;
		if (player == null) {
			System.out.println("Do you want to be able to use " + TimeShift.name + " commands from the console? Ask cjc. Maybe.");
			return false;
		}

		//check that user has one of the permissions at least.
		if (!checkShift(player) && !checkStartup(player)) {
			//has neither
			player.sendMessage("You don't have permission to use /shift.");
			return true;
		}
		/*
		 * Depending on the player's command, the number in the temp file is
		 * changed. The number is checked by the Time class every second or so
		 * to determine what the time should be in the server.
		 */

		if (commandName.equals("shift")) {
			// shift command used, only checking for future ability to. right now, it'd be assumed.

			if (split.length == 0) {
				//if it didn't have args, it's a help request. check what permission level they have and display customized permissions.
				//if permissions isn't installed, checkPermissions returns true, so users will see "full help"
				if (checkStartup(player) && checkShift(player)) {
					//has both
					player.sendMessage("Usage: /shift day | night | stop | startup <x>");
				} else if (checkShift(player)) {
					//has shift only
					player.sendMessage("Usage: /shift day | night | stop");
				} else {
					//has startup only
					player.sendMessage("Usage: /shift startup [day | night | stop] -- sets startup and /reload behavior only.");
				}
				//command handled
				return true;
			}
			
			//length was at least 1.
			TierOne T1 = null;
			try {
				// try to match first argument to argument list
				T1 = asTierOne(split[0]);
				if (T1 == null) {
					//this means a false 1st argument was used.
					//returning false displays the "full" help from the uml file.
					//the /help command is not mentioned so that bukkit won't touch it.
					return false;
				}
			} catch (NullPointerException ex) {
			}
			switch (T1) {

			case DAY:
				if (checkShiftError(player)) return true;
				setSetting(0,player);			
				server.broadcastMessage("The time suddenly shifts!");
				break;
			case NIGHT:
				if (checkShiftError(player)) return true;
				setSetting(13800,player);
				server.broadcastMessage("The time suddenly shifts!");
				break;
			case STOP:
				if (checkShiftError(player)) return true;
				setSetting(-1,player);
				server.broadcastMessage("Time appears to be back to normal...");
				break;
			case STARTUP:
				// if case is startup, check for permissions.
				if (!checkStartup(player)) {
					player.sendMessage("You need timeshift.startup permission.");
					return true;	
				}
				if (split.length == 1) {
					player.sendMessage("Startup Usage: /shift startup ( day | night | stop )");
					return true;
				}
				TierOne T2 = null;
				try {
					T2 = asTierOne(split[1]);
					if (T2 == null) {
						//improper usage, displays full help
						return false;
					}
				} catch (NullPointerException ex) {
				}
				switch (T2) {
				case DAY:
					TimeShiftFileReaderWriter.persistentWriter(0, player);
					player.sendMessage("Server will loop day on startup");
					break;
				case NIGHT:
					TimeShiftFileReaderWriter.persistentWriter(13800, player);
					player.sendMessage("Server will loop night on startup");
					break;
				case STOP:
					TimeShiftFileReaderWriter.persistentWriter(-1, player);
					player.sendMessage("Server will not loop on startup");
					break;
				case STARTUP:
					player.sendMessage("Why would you even want to try that?");
					break;
				default:
					break;
				}
				break;
			default:
				break;
			}
			//catch shift commands
			return true;
		} 
		//never returned true
		return false;
	}

	private static TierOne asTierOne(String str) {
		for (TierOne t1 : TierOne.values()) {
			if (t1.name().equalsIgnoreCase(str)) {
				return t1;
			}
		}
		return null;
	}
	
	private enum TierOne {
		DAY, NIGHT, STOP, STARTUP
	}

}
