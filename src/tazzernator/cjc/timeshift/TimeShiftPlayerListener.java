package tazzernator.cjc.timeshift;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import tazzernator.cjc.timeshift.settings.WorldSetting;

public class TimeShiftPlayerListener implements Listener {
	private TimeShift plugin;

	public TimeShiftPlayerListener(TimeShift instance) {
		this.plugin = instance;
	}

	// handles /time command only in order to 'peacefully' use it
	// ignores 'time help' and 'time' commands. The time command alone may display the time.
	// Help for time should not cancel a shift. Other cases may exist which should not cancel, but
	// it isn't possible to check every plugin's implementation of the time command since it is quite common
	// and varies in usage. Shifts must be canceled for the time command to be effective in another plugin.
	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		if (event.getMessage().startsWith("/time ")) {// does not catch just /time, catches /time [arg]
			if (event.getMessage().startsWith("help", 6)) {
				return; // doesn't catch /time help
			}
			Player player = event.getPlayer();
			// time command cancels an active shift only
			// check for permission to (cancel a) shift
			if (!player.hasPermission("timeshift.cancel")) {
				return;
			}

			World w = player.getWorld();

			// TST should fix before it is ever an issue?
			WorldSetting setting = TimeShift.world_settings.get(w.getName());
			if (setting != null && setting.getTid() != -1) { // check if a 'shift' is active
				plugin.cancelShift(setting);
				String wname = player.getWorld().getName();
				TimeShift.world_settings.put(wname, setting);
				plugin.tsm.sendMessage(player, wname, "", false);// print result
			}
			return;
		}
	}
}
