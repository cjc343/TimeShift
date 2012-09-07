package tazzernator.cjc.timeshift;

import org.bukkit.World;

import tazzernator.cjc.timeshift.settings.LoopSetting;
// The way of the future!
public class TimeShiftShifter implements Runnable {
	protected World world = null;
	protected LoopSetting loop_setting;
	protected TimeShift plugin;
	protected int currentIdx = 0;
	protected int nextStartTime;
	protected int stopTime;
	public void run() {
		long time = world.getTime();
		if (time < stopTime && !(time < 20 && stopTime == 24000)) {
			plugin.scheduleTimer(world, loop_setting, currentIdx, (stopTime - time));
			return;
		}
		if (!( stopTime == 24000 && nextStartTime == 0)) {
			world.setTime(nextStartTime);//set the time and schedule a new timer for when it should end.
		}
		currentIdx = loop_setting.getNextIdx(currentIdx);//get next index
		plugin.scheduleTimer(world, loop_setting, currentIdx, loop_setting.getStopTime(currentIdx) - loop_setting.getStartTime(currentIdx));
	}
}