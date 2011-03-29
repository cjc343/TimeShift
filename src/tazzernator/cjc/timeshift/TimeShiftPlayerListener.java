package tazzernator.cjc.timeshift;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerListener;

public class TimeShiftPlayerListener extends PlayerListener {
	private TimeShift plugin;

	
	public TimeShiftPlayerListener(TimeShift instance) {
		this.plugin = instance;
	}
	
	private void setSetting(int setting, Player player) {
		World w = plugin.getServer().getWorld(player.getWorld().getName());
		TimeShift.settings.put(w.getName(), setting);
	}
	
	//handles /time command only in order to 'peacefully' use it?
	@Override
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		if (event.getMessage().startsWith("/time ")) {//does not catch just /time, catches /time [arg]
			if (event.getMessage().startsWith("help", 6)) {
				return; //doesn't catch /time help
			}
			Player player = event.getPlayer();
			// time command cancels an active shift only
			// check for permission to (cancel a) shift
			if (TimeShift.Permissions != null) {
				if (!TimeShift.Permissions.getHandler().has(player, TimeShiftCommandParser.cmdPerm) && !TimeShift.Permissions.getHandler().has(player, TimeShiftCommandParser.cmdCancel)) {
					return;
				}
			}		
			World w = player.getWorld();
			try {
				// TST should fix before it is ever an issue?
				if (TimeShift.settings.get(w.getName()) != -1) {
					setSetting(-1, player);
					plugin.getServer().broadcastMessage(TimeShiftCommandParser.norm + w.getName() + TimeShiftCommandParser.clbr);
				}
			} catch (ArrayIndexOutOfBoundsException e) {
				System.out.println(TimeShift.name + " had a minor error with the /time command. Please report.");
			}
			// don't catch time commands?
			return;
		}
	}
}
