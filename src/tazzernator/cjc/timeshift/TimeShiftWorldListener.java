package tazzernator.cjc.timeshift;

import org.bukkit.event.world.WorldListener;
import org.bukkit.event.world.WorldLoadEvent;

public class TimeShiftWorldListener extends WorldListener {

	TimeShift plugin;
	public TimeShiftWorldListener(TimeShift instance) {
		plugin = instance;
	}

	@Override
	public void onWorldLoad(WorldLoadEvent event) {
		plugin.scheduleTimer(event.getWorld());
	}
}
