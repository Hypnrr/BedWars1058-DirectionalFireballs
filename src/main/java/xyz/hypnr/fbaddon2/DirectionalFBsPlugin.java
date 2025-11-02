package xyz.hypnr.fbaddon2;

import com.andrei1058.bedwars.api.BedWars;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.hypnr.fbaddon.listeners.DirectionalFireballListener;
import java.lang.reflect.Method;

public class DirectionalFBsPlugin extends JavaPlugin {

    private BedWars bedWarsAPI;
    private Listener fireballListener;
    private volatile boolean featureEnabled = true;

    @Override
    public void onEnable() {
        setupBedWars();
        saveDefaultConfig();

        fireballListener = new DirectionalFireballListener(this);
        getServer().getPluginManager().registerEvents(fireballListener, this);

        FBAddonsCommand cmd = new FBAddonsCommand(this);
        if (getCommand("fbaddons") != null) {
            getCommand("fbaddons").setExecutor(cmd);
            getCommand("fbaddons").setTabCompleter(cmd);
        }
        if (getCommand("fbaddon") != null) {
            getCommand("fbaddon").setExecutor(cmd);
            getCommand("fbaddon").setTabCompleter(cmd);
        }

        getLogger().info("DirectionalFBsPlugin enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("DirectionalFBsPlugin disabled.");
    }

    private boolean setupBedWars() {
        if (getServer().getPluginManager().getPlugin("BedWars1058") == null) {
            bedWarsAPI = null;
            return false;
        }
        bedWarsAPI = Bukkit.getServicesManager().getRegistration(BedWars.class) != null ? Bukkit.getServicesManager().getRegistration(BedWars.class).getProvider() : null;
        return bedWarsAPI != null;
    }

    public BedWars getBedWarsAPI() {
        return bedWarsAPI;
    }

    public boolean isFeatureEnabled() {
        return featureEnabled;
    }

    public void setFeatureEnabled(boolean featureEnabled) {
        this.featureEnabled = featureEnabled;
    }

    public void refreshListenerFromConfig() {
        try {
            if (fireballListener != null) {
                Method m = fireballListener.getClass().getMethod("refreshFromConfig");
                m.invoke(fireballListener);
            }
        } catch (Throwable ignored) {}
    }
}
