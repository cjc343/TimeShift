package com.bukkit.tazzernator.timeshift;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Timer;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.Server;


public class TimeShiftPlayerListener extends PlayerListener {
	private final TimeShift plugin;
	private Server server;

    public TimeShiftPlayerListener(TimeShift instance, Server server) {
        plugin = instance;
        this.server = server;
    }
    
    static void fileWriter(String num){
    	//Method used to write to our temp file.
    	FileWriter fstream;
		try {
			fstream = new FileWriter("TimeShift.time");
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(num);
			out.close();
		} catch (IOException e) {
		}
    }
    public void onPlayerCommand(PlayerChatEvent event) {
        String[] split = event.getMessage().split(" ");
        Player player = event.getPlayer();
        
        /*
         * Depending on the player's command, the number in the temp file is changed.
         * The number is checked by the Time class every second or so to determine
         * what the time should be in the server.
         */
        if (split[0].equalsIgnoreCase("/shift")) {
        	if(split.length == 1){
        		player.sendMessage("Usage: /shift day | night | stop");
        	}
        	else if(split.length == 2){
        		if(split[1].equalsIgnoreCase("day")){
        			fileWriter("0");
        			server.broadcastMessage("The time suddenly shifts!");
        		}
        		else if(split[1].equalsIgnoreCase("night")){
        			fileWriter("13800");
        			server.broadcastMessage("The time suddenly shifts!");
        		}
        		else if(split[1].equalsIgnoreCase("stop")){
        			fileWriter("-1");
        			server.broadcastMessage("Time appears to be back to normal...");
        		}
        		else {
        			player.sendMessage("Usage: /shift day | night | stop");
        		}        		
        	}
        	event.setCancelled(true);		
        }
        else if (split[0].equalsIgnoreCase("/time")) {
        	fileWriter("-1");
			server.broadcastMessage("Time appears to be back to normal...");
			event.setCancelled(true);
        }
    }

}
