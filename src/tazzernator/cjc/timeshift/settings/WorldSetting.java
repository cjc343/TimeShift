package tazzernator.cjc.timeshift.settings;

public class WorldSetting {
	
	String worldName;//key for world_settings
	int tid = -1;//Current task handling world
	String loopName;//The name of loop being performed. This is equal to the key used in the config file for command definitions. It is the value returned from command_aliases for any synonym of a name. It is the key to loop_settings which defines how 
	public String getWorldName() {
		return worldName;
	}
	public void setWorldName(String worldName) {
		this.worldName = worldName;
	}
	public int getTid() {
		return tid;
	}
	public void setTid(int tid) {
		this.tid = tid;
	}
	public String getLoopName() {
		return loopName;
	}
	public void setLoopName(String loopName) {
		this.loopName = loopName;
	}
}
