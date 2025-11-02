package xyz.hypnr.fbaddon;

import com.andrei1058.bedwars.api.BedWars;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import xyz.hypnr.fbaddon.commands.FBAddonCommand;

public class DirectionalFBAddon extends JavaPlugin {

    private static DirectionalFBAddon instance;
    private BedWars bedWarsAPI;
    private Listener fireballListener;
    private volatile boolean featureEnabled = true;

    @Override
    public void onEnable() {
        instance = this;

        setupBedWars();

        saveDefaultConfig();

        try {
            Class<?> cls = Class.forName("xyz.hypnr.fbaddon.listeners.DirectionalFireballListener");
            Object listenerInstance = null;
            try {
                Constructor<?> c1 = cls.getConstructor(JavaPlugin.class);
                listenerInstance = c1.newInstance(this);
            } catch (NoSuchMethodException ignored) {
                try {
                    Constructor<?> c2 = cls.getConstructor(this.getClass());
                    listenerInstance = c2.newInstance(this);
                } catch (NoSuchMethodException ex) {
                    throw new RuntimeException("No suitable constructor for DirectionalFireballListener", ex);
                }
            }
            if (!(listenerInstance instanceof Listener)) {
                throw new RuntimeException("DirectionalFireballListener does not implement Listener");
            }
            fireballListener = (Listener) listenerInstance;
            getServer().getPluginManager().registerEvents(fireballListener, this);
        } catch (Exception ex) {
            getLogger().severe("Failed to initialize DirectionalFireballListener: " + ex.getMessage());
            ex.printStackTrace();
        }

        if (getCommand("fbaddon") != null) {
            FBAddonCommand cmd = new FBAddonCommand(this);
            getCommand("fbaddon").setExecutor(cmd);
            getCommand("fbaddon").setTabCompleter(cmd);
        }

        getLogger().info("DirectionalFBAddon enabled. Fireball knockback is directional.");
    }

    @Override
    public void onDisable() {
        getLogger().info("DirectionalFBAddon disabled.");
        instance = null;
    }

    private boolean setupBedWars() {
        if (getServer().getPluginManager().getPlugin("BedWars1058") == null) {
            bedWarsAPI = null;
            return false;
        }
        bedWarsAPI = Bukkit.getServicesManager().getRegistration(BedWars.class) != null ? Bukkit.getServicesManager().getRegistration(BedWars.class).getProvider() : null;
        return bedWarsAPI != null;
    }

    public static DirectionalFBAddon getInstance() {
        return instance;
    }

    public BedWars getBedWarsAPI() {
        return bedWarsAPI;
    }

    public boolean isFeatureEnabled() {
        return featureEnabled;
    }

    public void setFeatureEnabled(boolean enabled) {
        this.featureEnabled = enabled;
    }

    public void reloadAddonConfig() {
        reloadConfig();
        if (fireballListener != null) {
            try {
                Method m = fireballListener.getClass().getMethod("refreshFromConfig");
                m.invoke(fireballListener);
            } catch (Throwable ignored) {
                // ignore if not present
            }
        }
    }
}
