package tazzernator.cjc.timeshift;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;


public class TimeShiftPlayerListener implements Listener {
	 private TimeShift instance;

	public TimeShiftPlayerListener(TimeShift instance) {
		 this.instance = instance;
	}

	// handles /time command only in order to 'peacefully' use it
	// ignores 'time help' and 'time' commands. The time command alone may display the time.
	// Help for time should not cancel a shift. Other cases may exist which should not cancel, but
	// it isn't possible to check every plugin's implementation of the time command since it is quite common
	// and varies in usage. Shifts must be canceled for the time command to be effective in another plugin.
	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		if (event.getMessage().startsWith("/time ")) {// does not catch just /time, catches /time [arg]
			System.out.println("TimeShift has received a /time command which will probably cancel a shift");
			if (event.getMessage().startsWith("help", 6)) {
				return; // doesn't catch /time help
			}
			Player player = event.getPlayer();
			// time command cancels an active shift only
			// check for permission to (cancel a) shift
//			if (TimeShift.Permissions != null) {
				if (!player.hasPermission(TimeShiftCommandParser.cmdPerm) && !player.hasPermission(TimeShiftCommandParser.cmdCancel)) {
					return;
				}
//			}
		
			World w = player.getWorld();
			try {
				// TST should fix before it is ever an issue?
				if (TimeShift.settings.get(w.getName()) != -1) { // check if a 'shift' is active
					TimeShift.settings.put(player.getWorld().getName(), -1); // if so, cancel it
					instance.tsm.sendMessage(player, player.getWorld(), -1, false);// print result
				}
			} catch (ArrayIndexOutOfBoundsException e) {
				System.out.println(TimeShift.name + " had a minor error with the /time command. Please report.");
			}
			// don't catch time commands?
			return;
		}
	}
}
