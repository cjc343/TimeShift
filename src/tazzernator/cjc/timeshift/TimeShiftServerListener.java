package tazzernator.cjc.timeshift;
import org.bukkit.event.server.PluginEvent;
import org.bukkit.event.server.ServerListener;
import org.bukkit.plugin.Plugin;

import com.nijikokun.bukkit.Permissions.Permissions;
public class TimeShiftServerListener extends ServerListener {

    private TimeShift origin;

    public TimeShiftServerListener(TimeShift thisPlugin) {
        this.origin = thisPlugin;
    }

    @Override
    public void onPluginEnabled(PluginEvent event) {
        if (TimeShift.Permissions == null) {
            Plugin plugin = origin.getServer().getPluginManager().getPlugin("Permissions");

            if (plugin != null) {
                if (plugin.isEnabled()) {
                    TimeShift.Permissions = (Permissions)plugin;
                    System.out.println("[" + TimeShift.name + "] hooked into Permissions.");
                }
            }
        }
    }
    @Override
    public void onPluginDisabled(PluginEvent event) {
        if (TimeShift.Permissions != null) {
            String plugin = event.getPlugin().getDescription().getName();

            if (plugin.equals("Permissions")) {
                    TimeShift.Permissions = null;
                    System.out.println("[" + TimeShift.name + "] un-hooked from Permissions");
                }
            }
        }
}
