package tazzernator.cjc.timeshift;

import java.util.TimerTask;
import org.bukkit.World;

/*
 * Credit for the concept of this class goes to feverdream
 * https://github.com/feverdream/
 */

//modified by cjc on feb 17 to switch to a string index, and new settings format and feb 18 to add this comment.

//this class has been modified by cjc and Tazzernator. Dates of modification are unknown, but currently prior(or equal) to Feb. 06, 2011, 
//the date this was added, and certainly later than Jan. 16, 2011, the date Noon('s source) was first publicly available.
//as such, this class, as a work, and as specified by the AGPL license Noon was released under, is also AGPL licensed.

// in order to make that a little more 'prominent', I'm just going to repeat that this class is AGPL licensed.

// this class is AGPL licensed.

//this class was modified again by cjc on feb 7 in order to shorten it even further and add multi-world support.
// it should now consist almost solely of the logic from feverdream's Noon, and modified many times at that.

// ******************************** THIS CLASS IS AGPL LICENSED!!!1! **********************************************

public class TimeShiftTimer extends TimerTask {
	public World world = null;
	public String index;

	public void run() {
		long time = world.getTime();
		long relativeTime = time % 24000;
		long startOfDay = time - relativeTime;
		// Number is checked, and if it applies, the time is set
		try {
			if (relativeTime > 12000 && TimeShift.settings.get(index) == 0) {
				world.setTime(startOfDay + 24000);
			} else if ((relativeTime > 22200 || relativeTime < 13700) && TimeShift.settings.get(index) == 13800) {
				world.setTime(startOfDay + 37700);
			}
		} catch (Exception e) {
			TimeShift.settings.put(index, -1);
		}
	}
}
