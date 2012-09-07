package tazzernator.cjc.timeshift;

import org.bukkit.World;

import tazzernator.cjc.timeshift.settings.LoopSetting;
//'classic', called repeatedly but still vastly different from before in order to accommodate commands of any positive range.
public class TimeShiftRunnable implements Runnable {
	protected World world = null;//world operated upon
	protected LoopSetting loop_setting;//loop setting being operated
	protected int currentIdx = 0;//current index in loop setting
	protected long lastTime;//last time checked
	protected int stop;//stop time for current index
	public void run() {
		long time = world.getTime();
		if (time > lastTime && time < stop) {
			lastTime = time;
		} else if (time >= stop || time < lastTime) {
			currentIdx = loop_setting.getNextIdx(currentIdx);
			world.setTime(loop_setting.getStartTime(currentIdx));
			stop = loop_setting.getStopTime(currentIdx);
			lastTime = world.getTime();
		}// else {
			//time is < stop and time = lastTime, all I had was a message, and it seems so unlikely that I see no point in that.
	}
}