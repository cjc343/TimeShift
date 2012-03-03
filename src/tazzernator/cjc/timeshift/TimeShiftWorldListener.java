package tazzernator.cjc.timeshift;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.bukkit.event.world.WorldLoadEvent;

public class TimeShiftWorldListener implements Listener {

	TimeShift plugin;
	public TimeShiftWorldListener(TimeShift instance) {
		plugin = instance;
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onWorldLoad(WorldLoadEvent event) {
		plugin.scheduleTimer(event.getWorld());
	}
}
