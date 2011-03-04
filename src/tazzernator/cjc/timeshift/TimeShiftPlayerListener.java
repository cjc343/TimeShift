package com.bukkit.tazzernator.timeshift;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList; //import java.util.Timer;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.Server;
import org.bukkit.World;

public class TimeShiftPlayerListener extends PlayerListener {
	// private final TimeShift plugin;
	private Server server;
	ArrayList<String> data = new ArrayList<String>();

	public TimeShiftPlayerListener(TimeShift instance, Server server) {
		// plugin = instance;
		this.server = server;
	}

	private ArrayList<String> readLines(String filename) throws IOException {
		// Method to read our number in the temp file
		data.clear();
		FileReader fileReader = new FileReader(filename);
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		String line = null;
		while ((line = bufferedReader.readLine()) != null) {
			data.add(line.toLowerCase());
		}
		bufferedReader.close();
		return data;
	}

//	static void fileWriter(String num) {
//		// Method used to write to our temp file.
//		FileWriter fstream;
//		try {
//			fstream = new FileWriter("plugins/TimeShift/TimeShift.time");
//			BufferedWriter out = new BufferedWriter(fstream);
//			out.write(num);
//			out.close();
//		} catch (IOException e) {
//		}
//	}

	//build and write string to file for persistent settings.
	void persistentWriter(int setting, Player player) {
	//	System.out.println("persistent state attempted: " + setting + "  in world : " + player.getWorld().hashCode());
		String output = "";
		
		//read in file
		//modify correct setting
		//output to file
		try {
			readLines(TimeShift.path);
		} catch (Exception e) {
		}
		
		int i = 0;
		for (World w : server.getWorlds()) {
			if (!(w.hashCode() == player.getWorld().hashCode())) {
				i++;
			}
		}
		
		int j = 0;
		for (String d : data) {
			String[] sets = d.split(",");
			for (String e : sets) {
				if (j == i) {
	//				System.out.println("setting : " + setting);
					output = output + setting + ",";
					j++;
				} else {
	//				System.out.println("e : " + e);
					output = output + e + ",";
					j++;
				}
			}
		}
		
		//if we haven't made it to i yet, get there.
		while (j <= i) {
			if (j < i) {
				output = output + "-1,";
				j++;
			} else if (j == i) {
				output = output + setting + ",";
				j++;
			}
		}
		
		//write out the output.
		FileWriter fstream;
		try {
			fstream = new FileWriter(TimeShift.path);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(output);
			out.close();
		} catch (IOException e) {
		}
	}
	
	//locally changes one of the servers.
	public void setSetting(int setting, Player player) throws ArrayIndexOutOfBoundsException {
		int i = 0;
		for (World w : server.getWorlds()) {
			if (w.hashCode() == player.getWorld().hashCode()) {
				try {
					TimeShift.settings.set(i, setting);
				} catch (ArrayIndexOutOfBoundsException ex) {
					TimeShift.settings.add(i, setting);
				}
			}
			i++;
		}
	}

	// feb. 5 2011
	// mostly rewritten to do switch/case instead of if/else
	// also added new functionality:
	// --optional Permissions support
	// --Ability to set an on server-load/server-reload default
	public void onPlayerCommand(PlayerChatEvent event) throws NullPointerException,ArrayIndexOutOfBoundsException {
		String[] split = event.getMessage().split(" ");
		Player player = event.getPlayer();

		/*
		 * Depending on the player's command, the number in the temp file is
		 * changed. The number is checked by the Time class every second or so
		 * to determine what the time should be in the server.
		 */

		if (split[0].equalsIgnoreCase("/shift")) {
			// permission check -- if permissions isn't installed, should
			// default to allow.
			if (TimeShift.Permissions != null) {
				if (!TimeShift.Permissions.has(player, "timeshift.shift")) {
					return;
				}
			}

			if (split.length == 1) {
				if (TimeShift.Permissions != null) {
					if (!TimeShift.Permissions.has(player, "timeshift.startup")) {
						// not able to change startup
						player.sendMessage("Usage: /shift day | night | stop");
					} else {
						// able to change startup
						player.sendMessage("Usage: /shift day | night | stop | startup <x>");
					}
				} else {
					// no permissions plugin
					player.sendMessage("Usage: /shift day | night | stop | startup <x>");
				}
				event.setCancelled(true);
				return;
			}
			
			TierOne T1 = null;
			try {
				// try to match argument to argument list
				T1 = asTierOne(split[1]);
				if (T1 == null) {
					return;
				}
			} catch (NullPointerException ex) {
				//ex.
				// if there's no matching arg, display help.
				// if all have permission, display full help.
				// if permissions are set, display appropriate commands.
//				
//				if (TimeShift.Permissions != null) {
//					if (!TimeShift.Permissions.has(player, "timeshift.startup")) {
//						// not able to change startup
//						player.sendMessage("Usage: /shift day | night | stop");
//					} else {
//						// able to change startup
//						player.sendMessage("Usage: /shift day | night | stop | startup <x>");
//					}
//				} else {
//					// no permissions plugin
//					player.sendMessage("Usage: /shift day | night | stop | startup <x>");
//				}
//				event.setCancelled(true);
			}
			switch (T1) {

			case DAY:
				setSetting(0,player);			
				server.broadcastMessage("The time suddenly shifts!");
				event.setCancelled(true);
				break;
			case NIGHT:
				setSetting(13800,player);
				server.broadcastMessage("The time suddenly shifts!");
				event.setCancelled(true);
				break;
			case STOP:
				setSetting(-1,player);
				server.broadcastMessage("Time appears to be back to normal...");
				event.setCancelled(true);
				break;
			case STARTUP:
				// if case is startup, check for permissions.
				if (TimeShift.Permissions != null) {
					if (!TimeShift.Permissions.has(player, "timeshift.startup")) {
						player.sendMessage("You need timeshift.startup permission.");
						return;
					}
				}
				if (split.length == 2) {
					player.sendMessage("Startup Usage: /shift startup ( day | night | stop )");
					event.setCancelled(true);
					return;
				}
				TierOne T2 = null;
				try {
					T2 = asTierOne(split[2]);
					if (T2 == null) {
						return;
					}
				} catch (NullPointerException ex) {
//					player.sendMessage("Startup Usage: /shift startup ( day | night | stop )");
//					event.setCancelled(true);
				}
				switch (T2) {
				case DAY:
					persistentWriter(0, player);
					player.sendMessage("Server will loop day on startup");
					event.setCancelled(true);
					break;
				case NIGHT:
					persistentWriter(13800, player);
					player.sendMessage("Server will loop night on startup");
					event.setCancelled(true);
					break;
				case STOP:
					persistentWriter(-1, player);
					player.sendMessage("Server will not loop on startup");
					event.setCancelled(true);
					break;
				case STARTUP:
					player.sendMessage("Why would you even want to try that?");
					event.setCancelled(true);
					break;
				default:
					break;
				}
				break;
			default:
				break;
			}
		} else if (split[0].equalsIgnoreCase("/time")) {
			// time command cancels an active shift only
			// check for permission to (cancel a) shift
			if (TimeShift.Permissions != null) {
				if (!TimeShift.Permissions.has(player, "timeshift.shift")) {
					return;
				}
			}
			
			int i = 0;
			for (World w : server.getWorlds()) {
				if (w.hashCode() != player.getWorld().hashCode()) {
					i++;
				}
			}
			try {
			//TST should fix before it is ever an issue?
			if (TimeShift.settings.get(i) != -1) {
				setSetting(-1,player);
				server.broadcastMessage("Time appears to be back to normal...");
				event.setCancelled(true);
			}
			} catch (ArrayIndexOutOfBoundsException e) {
				System.out.println(TimeShift.name + " had a minor error with the /time command. Please report.");
			}
			

		}

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
