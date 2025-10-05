package com.flouz1.tankplugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public class TankPlugin extends JavaPlugin implements Listener {

    private static final String HELMET_NAME = "ТАНК ШЛЕМ - М";
    private static final String CHESTPLATE_NAME = "ТАНК НАГРУДНИК - М";
    private static final String LEGGINGS_NAME = "ТАНК ПОНОЖИ - М";
    private static final String BOOTS_NAME = "ТАНК БОТИНКИ - М";
    private static final String SWORD_NAME = "Облученная Мачете - М";
    private static final String MEDKIT_NAME = "Аптечка";

    private BukkitTask effectTask;
    private final Set<UUID> activeMedkits = new HashSet<>();
    private final Map<UUID, Helicopter> helicopters = new HashMap<>();
    private final Map<UUID, UUID> seatOwners = new HashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        effectTask = Bukkit.getScheduler().runTaskTimer(this, this::updateEffects, 20L, 40L);
    }

    @Override
    public void onDisable() {
        if (effectTask != null) {
            effectTask.cancel();
        }
        new HashSet<>(helicopters.keySet()).forEach(this::removeHelicopter);
        seatOwners.clear();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Команду может выполнять только игрок.");
            return true;
        }

        Player player = (Player) sender;
        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Использование: /flouz1 ...");
            return true;
        }

        if (args[0].equalsIgnoreCase("com")) {
            if (args.length < 3) {
                player.sendMessage(ChatColor.RED + "Недостаточно аргументов.");
                return true;
            }

            if (args[1].equalsIgnoreCase("tank")) {
                String sub = args[2];
                if (sub.equalsIgnoreCase("helmet")) {
                    giveItem(player, createNamedItem(Material.DIAMOND_HELMET, HELMET_NAME));
                    player.sendMessage(ChatColor.GREEN + "Вы получили " + HELMET_NAME + ChatColor.GREEN + ".");
                    return true;
                }
                if (sub.equalsIgnoreCase("Bodyarmor")) {
                    giveItem(player, createNamedItem(Material.DIAMOND_CHESTPLATE, CHESTPLATE_NAME));
                    player.sendMessage(ChatColor.GREEN + "Вы получили " + CHESTPLATE_NAME + ChatColor.GREEN + ".");
                    return true;
                }
                if (sub.equalsIgnoreCase("Lenghtarmor")) {
                    giveItem(player, createNamedItem(Material.DIAMOND_LEGGINGS, LEGGINGS_NAME));
                    player.sendMessage(ChatColor.GREEN + "Вы получили " + LEGGINGS_NAME + ChatColor.GREEN + ".");
                    return true;
                }
            }

            if (args[1].equalsIgnoreCase("luckydayz")) {
                String sub = args[2];
                if (sub.equalsIgnoreCase("obla")) {
                    giveItem(player, createNamedItem(Material.DIAMOND_SWORD, SWORD_NAME));
                    player.sendMessage(ChatColor.GREEN + "Вы получили " + SWORD_NAME + ChatColor.GREEN + ".");
                    return true;
                }
                if (sub.equalsIgnoreCase("apteka")) {
                    ItemStack medkit = createNamedItem(Material.GLOWSTONE_DUST, MEDKIT_NAME);
                    giveItem(player, medkit);
                    player.sendMessage(ChatColor.GREEN + "Вы получили аптечку.");
                    return true;
                }
                if (sub.equalsIgnoreCase("vert")) {
                    spawnHelicopter(player);
                    return true;
                }
            }
        }

        if (args[0].equalsIgnoreCase("tank") && args.length >= 2 && args[1].equalsIgnoreCase("Boottarmor")) {
            giveItem(player, createNamedItem(Material.DIAMOND_BOOTS, BOOTS_NAME));
            player.sendMessage(ChatColor.GREEN + "Вы получили " + BOOTS_NAME + ChatColor.GREEN + ".");
            return true;
        }

        player.sendMessage(ChatColor.RED + "Неизвестная команда.");
        return true;
    }

    private void giveItem(Player player, ItemStack item) {
        player.getInventory().addItem(item);
    }

    private ItemStack createNamedItem(Material material, String name) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setUnbreakable(true);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(this, () -> updateEffectsFor(event.getPlayer()), 20L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        activeMedkits.remove(event.getPlayer().getUniqueId());
        removeHelicopter(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        int newSlot = event.getNewSlot();
        if (newSlot == 0 || newSlot == 2) {
            Player player = event.getPlayer();
            if (isWearingTankLeggings(player)) {
                applyAbsorption(player);
            }
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            if (isWearingTankLeggings(player)) {
                applyAbsorption(player);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            if (isWearingTankLeggings(player)) {
                applyAbsorption(player);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (!isNamedItem(item, Material.GLOWSTONE_DUST, MEDKIT_NAME)) {
            return;
        }
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!activeMedkits.add(uuid)) {
            return;
        }
        event.setCancelled(true);
        new BukkitRunnable() {
            int counter = 8;
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    activeMedkits.remove(uuid);
                    return;
                }
                if (counter >= 0) {
                    player.sendMessage(ChatColor.WHITE.toString() + ChatColor.BOLD + "Аптека отхилит вас через " + counter);
                }
                if (counter == 0) {
                    AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                    double maxHealth = attribute != null ? attribute.getValue() : player.getMaxHealth();
                    player.setHealth(maxHealth);
                    activeMedkits.remove(uuid);
                    cancel();
                    return;
                }
                counter--;
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractAtEntityEvent event) {
        Entity entity = event.getRightClicked();
        UUID ownerId = seatOwners.get(entity.getUniqueId());
        if (ownerId == null) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!player.getUniqueId().equals(ownerId)) {
            return;
        }
        entity.addPassenger(player);
    }

    private void updateEffects() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateEffectsFor(player);
        }
    }

    private void updateEffectsFor(Player player) {
        PlayerInventory inventory = player.getInventory();

        if (isNamedItem(inventory.getHelmet(), Material.DIAMOND_HELMET, HELMET_NAME)) {
            applyEffect(player, PotionEffectType.NIGHT_VISION, 0);
        } else {
            removeEffect(player, PotionEffectType.NIGHT_VISION);
        }

        if (isNamedItem(inventory.getChestplate(), Material.DIAMOND_CHESTPLATE, CHESTPLATE_NAME)) {
            applyEffect(player, PotionEffectType.REGENERATION, 0);
        } else {
            removeEffect(player, PotionEffectType.REGENERATION);
        }

        if (!isWearingTankLeggings(player)) {
            removeEffect(player, PotionEffectType.ABSORPTION);
        }

        if (isNamedItem(inventory.getBoots(), Material.DIAMOND_BOOTS, BOOTS_NAME)) {
            applyEffect(player, PotionEffectType.SPEED, 0);
        } else {
            removeEffect(player, PotionEffectType.SPEED);
        }

        ItemStack main = inventory.getItemInMainHand();
        ItemStack off = inventory.getItemInOffHand();
        if (isNamedItem(main, Material.DIAMOND_SWORD, SWORD_NAME) || isNamedItem(off, Material.DIAMOND_SWORD, SWORD_NAME)) {
            applyEffect(player, PotionEffectType.INCREASE_DAMAGE, 1);
        } else {
            removeEffect(player, PotionEffectType.INCREASE_DAMAGE);
        }
    }

    private boolean isWearingTankLeggings(Player player) {
        return isNamedItem(player.getInventory().getLeggings(), Material.DIAMOND_LEGGINGS, LEGGINGS_NAME);
    }

    private void applyEffect(Player player, PotionEffectType type, int amplifier) {
        PotionEffect effect = new PotionEffect(type, Integer.MAX_VALUE, amplifier, true, false, false);
        player.addPotionEffect(effect, true);
    }

    private void applyAbsorption(Player player) {
        PotionEffect effect = new PotionEffect(PotionEffectType.ABSORPTION, Integer.MAX_VALUE, 0, true, false, false);
        player.addPotionEffect(effect, true);
    }

    private void removeEffect(Player player, PotionEffectType type) {
        if (player.hasPotionEffect(type)) {
            player.removePotionEffect(type);
        }
    }

    private boolean isNamedItem(ItemStack stack, Material material, String name) {
        if (stack == null || stack.getType() != material) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        return meta != null && name.equals(meta.getDisplayName());
    }

    private void spawnHelicopter(Player player) {
        UUID uuid = player.getUniqueId();
        removeHelicopter(uuid);

        Location base = player.getLocation().add(player.getLocation().getDirection().setY(0).normalize().multiply(2));
        base.setY(base.getY() + 0.5);

        ArmorStand seat = (ArmorStand) player.getWorld().spawnEntity(base.clone(), EntityType.ARMOR_STAND);
        seat.setVisible(false);
        seat.setGravity(false);
        seat.setInvulnerable(true);
        seat.setSmall(false);
        seat.setCustomNameVisible(false);
        seat.getScoreboardTags().add("tank-helicopter-seat");

        List<ArmorStand> parts = new ArrayList<>();
        parts.add(createPart(base.clone(), Material.GREEN_CONCRETE, new Vector(0, -0.5, 0)));
        parts.add(createPart(base.clone().add(0, 0.6, 0), Material.IRON_TRAPDOOR, new Vector(0, 0, 0)));
        parts.add(createPart(base.clone().add(0, -0.2, 0.8), Material.BLACK_CONCRETE, new Vector(0, 0, 0)));

        helicopters.put(uuid, new Helicopter(seat, parts));
        seatOwners.put(seat.getUniqueId(), uuid);
        player.sendMessage(ChatColor.GREEN + "Вы вызвали вертолет.");
    }

    private ArmorStand createPart(Location location, Material helmetMaterial, Vector offset) {
        Location spawnLoc = location.clone().add(offset);
        ArmorStand stand = (ArmorStand) location.getWorld().spawnEntity(spawnLoc, EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setMarker(true);
        stand.setGravity(false);
        stand.setInvulnerable(true);
        stand.setCustomNameVisible(false);
        stand.setHelmet(new ItemStack(helmetMaterial));
        stand.getScoreboardTags().add("tank-helicopter-part");
        return stand;
    }

    private void removeHelicopter(UUID owner) {
        Helicopter helicopter = helicopters.remove(owner);
        if (helicopter == null) {
            return;
        }
        ArmorStand seat = helicopter.getSeat();
        seatOwners.remove(seat.getUniqueId());
        seat.remove();
        for (ArmorStand part : helicopter.getParts()) {
            part.remove();
        }
    }

    private static class Helicopter {
        private final ArmorStand seat;
        private final List<ArmorStand> parts;

        Helicopter(ArmorStand seat, List<ArmorStand> parts) {
            this.seat = seat;
            this.parts = parts;
        }

        ArmorStand getSeat() {
            return seat;
        }

        List<ArmorStand> getParts() {
            return parts;
        }
    }
}
