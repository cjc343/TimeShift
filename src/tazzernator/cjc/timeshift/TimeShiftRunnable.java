package tazzernator.cjc.timeshift;

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
	//public String index; // edited by cjc mar 29 to remove index variable.

	public void run() {
		long time = world.getTime();
		long relativeTime = time % 24000;
		long startOfDay = time - relativeTime;
		// modified by cjc march 22 (may have started late on the 21st, but I thought it was after midnight) to add sunrise, sunset, and a combo setting. After posting 1.5, added this comment to source and rearranged the order of checks in the if block below for (every so slightly) improved efficiency. Instead of finishing in O(1) time, it now finishes in (unnoticeably) faster O(1).
		// Number is checked, and if it applies, the time is set
		try {
			int setting = TimeShift.settings.get(world.getName());
			if (relativeTime > 12000 && setting == 0) {//day
				world.setTime(startOfDay + 24000);
			} else if (setting == 13800 && (relativeTime > 22200 || relativeTime < 13700)) {//night
				world.setTime(startOfDay + 37700);
			} else if (setting == 12000 && (relativeTime < 12000 || relativeTime > 13700)) {//sunset
				world.setTime(startOfDay + 36000);
			} else if (setting == 22000 && relativeTime < 22000) {//sunrise
				world.setTime(startOfDay + 46000);
			} else if (setting == -2) {//riseset/setrise
				if (relativeTime < 12000) {
					world.setTime(startOfDay + 36000);
				} else if (relativeTime > 13700 && relativeTime < 22000) {
					world.setTime(startOfDay + 46000);
				}
			}
		} catch (Exception e) {
			TimeShift.settings.put(world.getName(), -1);
		}
	}
}
