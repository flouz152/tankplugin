package com.flouz.tank;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

public class EquipmentEffectManager implements Listener {

    public static final String HELMET_NAME = "ТАНК ШЛЕМ - М";
    public static final String CHESTPLATE_NAME = "ТАНК НАГРУДНИК - М";
    public static final String LEGGINGS_NAME = "ТАНК ПОНОЖИ - М";
    public static final String BOOTS_NAME = "ТАНК БОТИНКИ - М";
    public static final String MACHETE_NAME = "Облученная Мачете - М";

    private final TankPlugin plugin;
    private BukkitTask repeatingTask;

    public EquipmentEffectManager(TankPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (repeatingTask != null) {
            repeatingTask.cancel();
        }
        this.repeatingTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAllPlayers, 20L, 20L);
    }

    public void stop() {
        if (repeatingTask != null) {
            repeatingTask.cancel();
            repeatingTask = null;
        }
    }

    private void updateAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyEquipmentEffects(player);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        applyEquipmentEffects(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        clearEffects(event.getPlayer());
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            if (isWearingTankLeggings(player)) {
                applyLeggingsEffect(player);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            if (isWearingTankLeggings(player)) {
                applyLeggingsEffect(player);
            }
        }
    }

    @EventHandler
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        int slot = event.getNewSlot();
        if (slot == 0 || slot == 2) {
            Player player = event.getPlayer();
            if (isWearingTankLeggings(player)) {
                applyLeggingsEffect(player);
            }
        }
    }

    private void applyEquipmentEffects(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack helmet = inventory.getHelmet();
        ItemStack chestplate = inventory.getChestplate();
        ItemStack leggings = inventory.getLeggings();
        ItemStack boots = inventory.getBoots();
        ItemStack mainHand = inventory.getItemInMainHand();
        ItemStack offHand = inventory.getItemInOffHand();

        boolean helmetEquipped = isNamedItem(helmet, Material.DIAMOND_HELMET, HELMET_NAME);
        boolean chestplateEquipped = isNamedItem(chestplate, Material.DIAMOND_CHESTPLATE, CHESTPLATE_NAME);
        boolean leggingsEquipped = isNamedItem(leggings, Material.DIAMOND_LEGGINGS, LEGGINGS_NAME);
        boolean bootsEquipped = isNamedItem(boots, Material.DIAMOND_BOOTS, BOOTS_NAME);
        boolean macheteEquipped = isNamedItem(mainHand, Material.DIAMOND_SWORD, MACHETE_NAME)
                || isNamedItem(offHand, Material.DIAMOND_SWORD, MACHETE_NAME);

        handleEffect(player, PotionEffectType.NIGHT_VISION, helmetEquipped, 0);
        handleEffect(player, PotionEffectType.REGENERATION, chestplateEquipped, 0);
        handleEffect(player, PotionEffectType.SPEED, bootsEquipped, 0);
        handleEffect(player, PotionEffectType.INCREASE_DAMAGE, macheteEquipped, 1);

        if (leggingsEquipped) {
            applyLeggingsEffect(player);
        } else {
            player.removePotionEffect(PotionEffectType.ABSORPTION);
        }
    }

    private void clearEffects(Player player) {
        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        player.removePotionEffect(PotionEffectType.REGENERATION);
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
        player.removePotionEffect(PotionEffectType.ABSORPTION);
    }

    private void applyLeggingsEffect(Player player) {
        PotionEffect effect = new PotionEffect(PotionEffectType.ABSORPTION, Integer.MAX_VALUE, 0, false, false, true);
        player.addPotionEffect(effect);
    }

    private void handleEffect(Player player, PotionEffectType type, boolean shouldHave, int amplifier) {
        if (shouldHave) {
            PotionEffect effect = new PotionEffect(type, Integer.MAX_VALUE, amplifier, false, false, true);
            player.addPotionEffect(effect);
        } else {
            player.removePotionEffect(type);
        }
    }

    private boolean isWearingTankLeggings(Player player) {
        return isNamedItem(player.getInventory().getLeggings(), Material.DIAMOND_LEGGINGS, LEGGINGS_NAME);
    }

    public static boolean isNamedItem(ItemStack stack, Material material, String name) {
        if (stack == null || stack.getType() != material || !stack.hasItemMeta()) {
            return false;
        }
        String displayName = stack.getItemMeta().getDisplayName();
        return displayName != null && displayName.equals(name);
    }
}
