package tazzernator.cjc.timeshift;

import com.avaje.ebean.validation.NotEmpty;
import com.avaje.ebean.validation.NotNull;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity()
@Table(name="ts_world")
public class TimeShiftWorldSetting {
	@Id
	private int id;
	
	@NotEmpty
	private String worldName;
	
	@NotNull
	private int setting;

	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public void setWorldName(String worldName) {
		this.worldName = worldName;
	}

	public String getWorldName() {
		return worldName;
	}

	public void setSetting(int setting) {
		this.setting = setting;
	}

	public int getSetting() {
		return setting;
	}

}
