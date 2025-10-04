package com.flouz.tank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class HelicopterManager implements Listener {

    private final TankPlugin plugin;
    private final Map<UUID, Helicopter> helicopters = new HashMap<>();
    private final Map<UUID, Helicopter> standIndex = new HashMap<>();

    public HelicopterManager(TankPlugin plugin) {
        this.plugin = plugin;
    }

    public void spawnHelicopter(Player player) {
        Helicopter existing = helicopters.remove(player.getUniqueId());
        if (existing != null) {
            unregister(existing);
            existing.remove();
            player.sendMessage(ChatColor.YELLOW + "Предыдущий вертолет был удален.");
        }

        Helicopter helicopter = new Helicopter(plugin, player);
        helicopter.spawn();
        register(helicopter);
        player.sendMessage(ChatColor.GREEN + "Вертолет призван! Только вы можете в него сесть.");
        new BukkitRunnable() {
            @Override
            public void run() {
                helicopter.mountOwner();
            }
        }.runTask(plugin);
    }

    @EventHandler
    public void onEntityMount(EntityMountEvent event) {
        Entity mount = event.getMount();
        if (!(mount instanceof ArmorStand)) {
            return;
        }

        Helicopter helicopter = standIndex.get(mount.getUniqueId());
        if (helicopter == null) {
            return;
        }

        if (!(event.getEntity() instanceof Player)) {
            event.setCancelled(true);
            return;
        }

        Player player = (Player) event.getEntity();
        if (!player.getUniqueId().equals(helicopter.getOwner().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onStandDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof ArmorStand && standIndex.containsKey(entity.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Helicopter helicopter = helicopters.remove(event.getPlayer().getUniqueId());
        if (helicopter != null) {
            unregister(helicopter);
            helicopter.remove();
        }
    }

    public void shutdown() {
        for (Helicopter helicopter : new ArrayList<>(helicopters.values())) {
            unregister(helicopter);
            helicopter.remove();
        }
        helicopters.clear();
        standIndex.clear();
    }

    private void register(Helicopter helicopter) {
        helicopters.put(helicopter.getOwner().getUniqueId(), helicopter);
        for (ArmorStand stand : helicopter.getParts()) {
            standIndex.put(stand.getUniqueId(), helicopter);
        }
    }

    private void unregister(Helicopter helicopter) {
        for (ArmorStand stand : helicopter.getParts()) {
            standIndex.remove(stand.getUniqueId());
        }
    }

    private static class Helicopter {
        private final TankPlugin plugin;
        private final Player owner;
        private final List<ArmorStand> parts = new ArrayList<>();
        private ArmorStand seat;

        private Helicopter(TankPlugin plugin, Player owner) {
            this.plugin = plugin;
            this.owner = owner;
        }

        public void spawn() {
            Location base = owner.getLocation().clone();
            base.setPitch(0f);

            seat = spawnSeat(base);
            parts.add(seat);

            parts.add(spawnPart(base.clone().add(0, 0.4, 0), new ItemStack(Material.GRAY_CONCRETE), false));
            parts.add(spawnPart(base.clone().add(0, 1.0, 0), new ItemStack(Material.IRON_TRAPDOOR), true));
            parts.add(spawnPart(base.clone().add(0, 0.8, -0.7), new ItemStack(Material.LIGHT_GRAY_CONCRETE), false));
            parts.add(spawnPart(base.clone().add(0.5, 0.6, 0), new ItemStack(Material.IRON_BARS), false));
            parts.add(spawnPart(base.clone().add(-0.5, 0.6, 0), new ItemStack(Material.IRON_BARS), false));
        }

        private ArmorStand spawnSeat(Location location) {
            ArmorStand stand = location.getWorld().spawn(location, ArmorStand.class, armorStand -> {
                armorStand.setVisible(false);
                armorStand.setGravity(false);
                armorStand.setMarker(false);
                armorStand.setSmall(false);
                armorStand.setBasePlate(false);
                armorStand.setArms(false);
                armorStand.setCollidable(false);
                armorStand.setInvulnerable(true);
                armorStand.setCustomNameVisible(false);
            });
            return stand;
        }

        private ArmorStand spawnPart(Location location, ItemStack helmet, boolean spin) {
            ArmorStand stand = location.getWorld().spawn(location, ArmorStand.class, armorStand -> {
                armorStand.getEquipment().setHelmet(helmet);
                armorStand.setMarker(true);
                armorStand.setVisible(false);
                armorStand.setGravity(false);
                armorStand.setSmall(true);
                armorStand.setBasePlate(false);
                armorStand.setArms(false);
                armorStand.setCollidable(false);
                armorStand.setInvulnerable(true);
            });

            if (spin) {
                new BukkitRunnable() {
                    float yaw = 0f;

                    @Override
                    public void run() {
                        if (!stand.isValid()) {
                            cancel();
                            return;
                        }
                        yaw += 15f;
                        stand.setRotation(yaw, 0f);
                    }
                }.runTaskTimer(plugin, 0L, 2L);
            }

            return stand;
        }

        public Player getOwner() {
            return owner;
        }

        public List<ArmorStand> getParts() {
            return parts;
        }

        public void mountOwner() {
            if (seat != null && seat.isValid() && owner.isOnline()) {
                seat.addPassenger(owner);
            }
        }

        public void remove() {
            for (ArmorStand stand : parts) {
                if (stand != null && stand.isValid()) {
                    stand.remove();
                }
            }
            parts.clear();
        }
    }
}
