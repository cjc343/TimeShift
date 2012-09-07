package tazzernator.cjc.timeshift;

import com.avaje.ebean.validation.NotEmpty;
import com.avaje.ebean.validation.NotNull;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

// This is being @deprecated but still exists to transfer old user data first.
@Deprecated
@Entity()
@Table(name = "ts_world")
public class TimeShiftWorldSetting {
	@Id
	private int id;

	@NotEmpty
	private String worldName;

	@NotNull
	private int setting;

	// db entry id
	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	// manipulate the stored world name for a world
	@Deprecated
	public void setWorldName(String worldName) {
		this.worldName = worldName;
	}
	@Deprecated
	public String getWorldName() {
		return worldName;
	}

	// manipulate the stored settings for a world
	@Deprecated
	public void setSetting(int setting) {
		this.setting = setting;
	}
	@Deprecated
	public int getSetting() {
		return setting;
	}

}
