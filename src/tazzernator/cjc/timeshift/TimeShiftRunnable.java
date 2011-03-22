package tazzernator.cjc.timeshift;

//import java.util.TimerTask;
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
// it should now consist almost solely of the time logic from feverdream's Noon, and modified many times at that. <- comment modified march 8 to clarify

// ******************************** THIS CLASS IS AGPL LICENSED!!!1! **********************************************
//modified by cjc feb 27 to implement runnable instead of extending TimerTask
//modified by cjc mar 18 to potentially implement sunrise and sunset WITHOUT looking at Noon or ExtendDay code. Which means it's probably not right yet.
public class TimeShiftRunnable implements Runnable {
	public World world = null;
	public String index;

	public void run() {
		long time = world.getTime();
		long relativeTime = time % 24000;
		long startOfDay = time - relativeTime;

		// Number is checked, and if it applies, the time is set
		try {
			int setting = TimeShift.settings.get(index);
			if (relativeTime > 12000 && setting == 0) {//day
				world.setTime(startOfDay + 24000);
			} else if ((relativeTime > 22200 || relativeTime < 13700) && setting == 13800) {//night
				world.setTime(startOfDay + 37700);
			} else if ((relativeTime < 12000 || relativeTime > 13700) && setting == 12000) {//sunset
				world.setTime(startOfDay + 36000);
			} else if (relativeTime < 22000 && setting == 22000) {//sunrise
				world.setTime(startOfDay + 46000);
			} else if (setting == -2) {
				if (relativeTime < 12000) {
					world.setTime(startOfDay + 36000);
				} else if (relativeTime > 13700 && relativeTime < 22000) {
					world.setTime(startOfDay + 46000);
				}
			}
		} catch (Exception e) {
			TimeShift.settings.put(index, -1);
		}
	}
}
