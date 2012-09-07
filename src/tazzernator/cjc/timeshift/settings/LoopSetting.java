package tazzernator.cjc.timeshift.settings;

public class LoopSetting {
	int[][] settings;//The array of settings also defined in config
	String loopName;//key, loop name, defined as command in config
//	public LoopSetting() {
//		settings = null;
//		loopName = null;
//	}
	public LoopSetting(String l, int[][] s) {// Constructor with setting and name provided
		settings = s;
		loopName = l;
	}
	public String getLoopName() {
		return loopName;
	}
	public void setLoopName(String loopName) {
		this.loopName = loopName;
	}
	public int[][] getSettings() {
		return settings;
	}
	public void setSettings(int[][] settings) {//n x 2 array of settings, often 1x2 by default
		this.settings = settings;
	}
//	public int[] getPair(int idx) {
//		return settings[idx];
//	}
	public int getStartTime(int idx) {
		return settings[idx][0];
	}
	public int getStopTime(int idx) {
		return settings[idx][1];
	}
	// Provides the index of the next setting in the loop.
	public int getNextIdx(int idx) {
		if (idx == settings.length - 1) {
			return 0;
		}
		return ++idx;
	}
}