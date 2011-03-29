package tazzernator.cjc.timeshift;

import org.bukkit.event.server.PluginDisableEvent;
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

	@Override
	public void onPluginDisable(PluginDisableEvent event) {
		if (TimeShift.Permissions != null) {
			String plugin = event.getPlugin().getDescription().getName();

			if (plugin.equals(toHook)) {
				TimeShift.Permissions = null;
				System.out.println("[" + TimeShift.name + "] un-hooked from " + toHook);
			}
		}
	}
}