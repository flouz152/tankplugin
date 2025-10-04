package com.flouz.tank;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class MedkitManager implements Listener {

    public static final String MEDKIT_NAME = ChatColor.WHITE + "" + ChatColor.BOLD + "Аптечка";
    private static final String COUNTDOWN_PREFIX = ChatColor.WHITE + "" + ChatColor.BOLD + "Аптека отхилит вас через ";

    private final TankPlugin plugin;
    private final Map<UUID, BukkitTask> activeMedkits = new HashMap<>();

    public MedkitManager(TankPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerUseMedkit(PlayerInteractEvent event) {
        if (!event.getAction().name().contains("RIGHT_CLICK")) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || !isMedkit(item)) {
            return;
        }

        Player player = event.getPlayer();
        if (activeMedkits.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Аптечка уже активирована.");
            event.setCancelled(true);
            return;
        }

        consumeItem(player, event.getHand());
        event.setCancelled(true);
        startCountdown(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        BukkitTask task = activeMedkits.remove(event.getPlayer().getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    public void shutdown() {
        for (BukkitTask task : activeMedkits.values()) {
            task.cancel();
        }
        activeMedkits.clear();
    }

    private void startCountdown(Player player) {
        BukkitRunnable runnable = new BukkitRunnable() {
            private int seconds = 8;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancelMedkit(player.getUniqueId());
                    cancel();
                    return;
                }

                player.sendMessage(COUNTDOWN_PREFIX + seconds);

                if (seconds <= 0) {
                    if (player.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                        player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
                    } else {
                        player.setHealth(player.getMaxHealth());
                    }
                    cancelMedkit(player.getUniqueId());
                    cancel();
                    return;
                }

                seconds--;
            }
        };

        BukkitTask task = runnable.runTaskTimer(plugin, 0L, 20L);
        activeMedkits.put(player.getUniqueId(), task);
    }

    private void cancelMedkit(UUID uuid) {
        BukkitTask task = activeMedkits.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    private boolean isMedkit(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return false;
        }
        String displayName = stack.getItemMeta().getDisplayName();
        return displayName != null && displayName.equals(MEDKIT_NAME);
    }

    private void consumeItem(Player player, EquipmentSlot slot) {
        if (slot == null) {
            slot = EquipmentSlot.HAND;
        }

        ItemStack stack = slot == EquipmentSlot.HAND ? player.getInventory().getItemInMainHand()
                : player.getInventory().getItemInOffHand();
        if (stack == null) {
            return;
        }

        int amount = stack.getAmount();
        if (amount <= 1) {
            stack.setAmount(0);
            if (slot == EquipmentSlot.HAND) {
                player.getInventory().setItemInMainHand(null);
            } else {
                player.getInventory().setItemInOffHand(null);
            }
        } else {
            stack.setAmount(amount - 1);
            if (slot == EquipmentSlot.HAND) {
                player.getInventory().setItemInMainHand(stack);
            } else {
                player.getInventory().setItemInOffHand(stack);
            }
        }
    }
}
