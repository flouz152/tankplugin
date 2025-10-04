package com.flouz.tank;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class TankPlugin extends JavaPlugin {

    private EquipmentEffectManager effectManager;
    private MedkitManager medkitManager;
    private HelicopterManager helicopterManager;

    @Override
    public void onEnable() {
        this.effectManager = new EquipmentEffectManager(this);
        this.medkitManager = new MedkitManager(this);
        this.helicopterManager = new HelicopterManager(this);

        Bukkit.getPluginManager().registerEvents(effectManager, this);
        Bukkit.getPluginManager().registerEvents(medkitManager, this);
        Bukkit.getPluginManager().registerEvents(helicopterManager, this);

        effectManager.start();

        PluginCommand command = getCommand("flouz1");
        if (command != null) {
            FlouzCommand executor = new FlouzCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
    }

    @Override
    public void onDisable() {
        if (effectManager != null) {
            effectManager.stop();
        }
        if (medkitManager != null) {
            medkitManager.shutdown();
        }
        if (helicopterManager != null) {
            helicopterManager.shutdown();
        }
    }

    public EquipmentEffectManager getEffectManager() {
        return effectManager;
    }

    public MedkitManager getMedkitManager() {
        return medkitManager;
    }

    public HelicopterManager getHelicopterManager() {
        return helicopterManager;
    }
}
