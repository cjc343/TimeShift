package tazzernator.cjc.timeshift;

import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerListener;
import org.bukkit.plugin.Plugin;

import com.nijikokun.bukkit.Permissions.Permissions;

public class TimeShiftServerListener extends ServerListener {

	private TimeShift origin;
	private String toHook = "Permissions";

	public TimeShiftServerListener(TimeShift thisPlugin) {
		this.origin = thisPlugin;
	}

	// listens for Permissions plugin to be enabled, then hooks in. Only necessary if Permissions gets loaded after TimeShift, otherwise this is done in onEnable.
	// plugin load order is not guaranteed between releases, so this is just a safety net.
	@Override
	public void onPluginEnable(PluginEnableEvent event) {
		if (TimeShift.Permissions == null) {
			Plugin plugin = origin.getServer().getPluginManager().getPlugin(toHook);

			if (plugin != null) {
				if (plugin.isEnabled()) {
					TimeShift.Permissions = (Permissions) plugin;
					System.out.println("[" + TimeShift.name + "] hooked into " + toHook);
				}
			}
		}
	}
}
