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

	static void fileWriter(String num) {
		// Method used to write to our temp file.
		FileWriter fstream;
		try {
			fstream = new FileWriter("plugins/TimeShift/TimeShift.time");
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(num);
			out.close();
		} catch (IOException e) {
		}
	}

	static void persistentWriter(String num) {
		// Method used to write to our persistent file.
		FileWriter fstream;
		try {
			fstream = new FileWriter("plugins/TimeShift/TimeShift-Startup.time");
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(num);
			out.close();
		} catch (IOException e) {
		}
	}

	// feb. 5 2011
	// mostly rewritten to do switch/case instead of if/else
	// also added new functionality:
	// --optional Permissions support
	// --Ability to set an on server-load/server-reload default
	public void onPlayerCommand(PlayerChatEvent event) {
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

			TierOne T1 = null;
			try {
				// try to match argument to argument list
				T1 = TierOne.valueOf(split[1].toUpperCase());
			} catch (Exception ex) {
				// if there's no matching arg, display help.
				// if all have permission, display full help.
				// if permissions are set, display appropriate commands.
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
			}
			switch (T1) {

			case DAY:
				fileWriter("0");
				server.broadcastMessage("The time suddenly shifts!");
				event.setCancelled(true);
				break;
			case NIGHT:
				fileWriter("13800");
				server.broadcastMessage("The time suddenly shifts!");
				event.setCancelled(true);
				break;
			case STOP:
				fileWriter("-1");
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
				TierOne T2 = null;
				try {
					T2 = TierOne.valueOf(split[2].toUpperCase());
				} catch (Exception ex) {
					player.sendMessage("Startup Usage: /shift startup ( day | night | stop )");
				}
				switch (T2) {
				case DAY:
					persistentWriter("0");
					player.sendMessage("Server will loop day on startup");
					event.setCancelled(true);
					break;
				case NIGHT:
					persistentWriter("13800");
					player.sendMessage("Server will loop night on startup");
					event.setCancelled(true);
					break;
				case STOP:
					persistentWriter("-1");
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
			// check if there's an active shift
			try {
				readLines("plugins/TimeShift/TimeShift.time");
			} catch (IOException e) {
			}
			for (String d : data) {
				if (Integer.parseInt(d) != -1) {
					// if there is, cancel it.
					fileWriter("-1");
					server.broadcastMessage("Time appears to be back to normal...");
					event.setCancelled(true);
				}
			}
		}
	}

	private enum TierOne {
		DAY, NIGHT, STOP, STARTUP
	}

}
