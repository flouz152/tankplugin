package com.flouz1.tankplugin;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class TankPlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private static final String HELMET_NAME = "ТАНК ШЛЕМ - М";
    private static final String CHESTPLATE_NAME = "ТАНК НАГРУДНИК - М";
    private static final String LEGGINGS_NAME = "ТАНК ПОНОЖИ - М";
    private static final String BOOTS_NAME = "ТАНК БОТИНКИ - М";
    private static final String SWORD_NAME = "Облученная Мачете - М";
    private static final String MEDKIT_NAME = "Аптека";

    private NamespacedKey itemKey;
    private BukkitTask effectTask;
    private final Map<UUID, BukkitTask> medkitTasks = new HashMap<>();
    private final Map<ArmorPiece, PotionEffectType> armorEffects = new EnumMap<>(ArmorPiece.class);

    @Override
    public void onEnable() {
        this.itemKey = new NamespacedKey(this, "custom-item");

        armorEffects.put(ArmorPiece.HELMET, PotionEffectType.NIGHT_VISION);
        armorEffects.put(ArmorPiece.CHESTPLATE, PotionEffectType.REGENERATION);
        armorEffects.put(ArmorPiece.BOOTS, PotionEffectType.SPEED);

        Bukkit.getPluginManager().registerEvents(this, this);

        if (getCommand("flouz1") != null) {
            getCommand("flouz1").setExecutor(this);
            getCommand("flouz1").setTabCompleter(this);
        }

        this.effectTask = new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.getOnlinePlayers().forEach(TankPlugin.this::updatePlayerEffects);
            }
        }.runTaskTimer(this, 20L, 40L);
    }

    @Override
    public void onDisable() {
        if (effectTask != null) {
            effectTask.cancel();
        }
        medkitTasks.values().forEach(BukkitTask::cancel);
        medkitTasks.clear();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        updatePlayerEffects(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        BukkitTask task = medkitTasks.remove(event.getPlayer().getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            if (hasCustomItem(player.getInventory().getLeggings(), CustomItemType.TANK_LEGGINGS)) {
                applyAbsorption(player);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            if (hasCustomItem(player.getInventory().getLeggings(), CustomItemType.TANK_LEGGINGS)) {
                applyAbsorption(player);
            }
        }
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        int previousSlot = event.getPreviousSlot();
        int newSlot = event.getNewSlot();
        if (hasCustomItem(player.getInventory().getLeggings(), CustomItemType.TANK_LEGGINGS)) {
            if (isSlotOneThree(previousSlot) || isSlotOneThree(newSlot)) {
                applyAbsorption(player);
            }
        }
        Bukkit.getScheduler().runTaskLater(this, () -> updatePlayerEffects(player), 1L);
    }

    @EventHandler
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(this, () -> updatePlayerEffects(player), 1L);
    }

    @EventHandler
    public void onMedkitUse(PlayerInteractEvent event) {
        if (!(event.getAction().name().contains("RIGHT_CLICK"))) {
            return;
        }
        ItemStack item = event.getItem();
        if (!hasCustomItem(item, CustomItemType.MEDKIT)) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (medkitTasks.containsKey(uuid)) {
            return;
        }

        consumeItem(event.getHand(), player);
        startMedkitCountdown(player);
    }

    private void startMedkitCountdown(Player player) {
        UUID uuid = player.getUniqueId();
        BukkitRunnable runnable = new BukkitRunnable() {
            private int seconds = 8;

            @Override
            public void run() {
                player.sendMessage(ChatColor.WHITE.toString() + ChatColor.BOLD + "Аптека отхилит вас через " + seconds);
                if (seconds == 0) {
                    double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                    player.setHealth(Math.min(maxHealth, maxHealth));
                    cancel();
                    return;
                }
                seconds--;
            }

            @Override
            public synchronized void cancel() throws IllegalStateException {
                super.cancel();
                medkitTasks.remove(uuid);
            }
        };

        BukkitTask task = runnable.runTaskTimer(this, 0L, 20L);
        medkitTasks.put(uuid, task);
    }

    private void updatePlayerEffects(Player player) {
        PlayerInventory inventory = player.getInventory();

        for (Map.Entry<ArmorPiece, PotionEffectType> entry : armorEffects.entrySet()) {
            ArmorPiece piece = entry.getKey();
            PotionEffectType effect = entry.getValue();
            ItemStack item = piece.getItem(inventory);
            boolean hasItem = hasCustomItem(item, piece.getItemType());
            if (hasItem) {
                applyEffect(player, effect, piece.getAmplifier());
            } else {
                player.removePotionEffect(effect);
            }
        }

        boolean hasLeggings = hasCustomItem(inventory.getLeggings(), CustomItemType.TANK_LEGGINGS);
        if (!hasLeggings) {
            player.removePotionEffect(PotionEffectType.ABSORPTION);
        }

        boolean holdingSword = hasCustomItem(inventory.getItemInMainHand(), CustomItemType.MACHETE)
                || hasCustomItem(inventory.getItemInOffHand(), CustomItemType.MACHETE);
        if (holdingSword) {
            applyEffect(player, PotionEffectType.INCREASE_DAMAGE, 1);
        } else {
            player.removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
        }
    }

    private void applyEffect(Player player, PotionEffectType type, int amplifier) {
        PotionEffect current = player.getPotionEffect(type);
        int duration = 20 * 15;
        if (current == null || current.getDuration() <= 20 || current.getAmplifier() != amplifier) {
            player.addPotionEffect(new PotionEffect(type, duration, amplifier, true, false, false));
        }
    }

    private void applyAbsorption(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 20 * 60, 0, true, false, false));
    }

    private boolean isSlotOneThree(int slot) {
        return slot == 0 || slot == 2;
    }

    private void consumeItem(EquipmentSlot hand, Player player) {
        if (hand == null) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        ItemStack stack = hand == EquipmentSlot.HAND ? inventory.getItemInMainHand()
                : inventory.getItemInOffHand();
        if (stack == null || stack.getType() == Material.AIR) {
            return;
        }
        if (stack.getAmount() <= 1) {
            if (hand == EquipmentSlot.HAND) {
                inventory.setItemInMainHand(null);
            } else {
                inventory.setItemInOffHand(null);
            }
        } else {
            stack.setAmount(stack.getAmount() - 1);
        }
    }

    private boolean hasCustomItem(ItemStack stack, CustomItemType type) {
        if (stack == null || stack.getType() == Material.AIR) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String value = container.get(itemKey, PersistentDataType.STRING);
        return type.getKey().equals(value);
    }

    private ItemStack createItem(CustomItemType type) {
        ItemStack stack = new ItemStack(type.getMaterial());
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(type.getDisplayName());
            meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, type.getKey());
            if (type == CustomItemType.MACHETE) {
                meta.addEnchant(Enchantment.DAMAGE_ALL, 3, true);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Команда доступна только игрокам.");
            return true;
        }
        Player player = (Player) sender;
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Использование: /flouz1 com tank|luckydayz <тип>");
            return true;
        }

        String root = args[0].toLowerCase(Locale.ROOT);
        if (!"com".equals(root)) {
            player.sendMessage(ChatColor.RED + "Неизвестная подкоманда.");
            return true;
        }

        String category = args[1].toLowerCase(Locale.ROOT);
        String itemId = args[2].toLowerCase(Locale.ROOT);

        CustomItemType type = null;
        if ("tank".equals(category)) {
            switch (itemId) {
                case "helmet":
                    type = CustomItemType.TANK_HELMET;
                    break;
                case "bodyarmor":
                    type = CustomItemType.TANK_CHESTPLATE;
                    break;
                case "lenghtarmor":
                    type = CustomItemType.TANK_LEGGINGS;
                    break;
                case "boottarmor":
                    type = CustomItemType.TANK_BOOTS;
                    break;
                default:
                    break;
            }
        } else if ("luckydayz".equals(category)) {
            switch (itemId) {
                case "obla":
                    type = CustomItemType.MACHETE;
                    break;
                case "apteka":
                    type = CustomItemType.MEDKIT;
                    break;
                default:
                    break;
            }
        }

        if (type == null) {
            player.sendMessage(ChatColor.RED + "Неизвестный предмет.");
            return true;
        }

        ItemStack item = createItem(type);
        player.getInventory().addItem(item);
        player.sendMessage(ChatColor.GREEN + "Вы получили предмет: " + type.getDisplayName());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            result.add("com");
        } else if (args.length == 2) {
            if ("com".equalsIgnoreCase(args[0])) {
                result.add("tank");
                result.add("luckydayz");
            }
        } else if (args.length == 3) {
            if ("com".equalsIgnoreCase(args[0])) {
                if ("tank".equalsIgnoreCase(args[1])) {
                    result.add("helmet");
                    result.add("bodyarmor");
                    result.add("lenghtarmor");
                    result.add("boottarmor");
                } else if ("luckydayz".equalsIgnoreCase(args[1])) {
                    result.add("obla");
                    result.add("apteka");
                }
            }
        }
        return result;
    }

    private enum CustomItemType {
        TANK_HELMET("tank-helmet", Material.DIAMOND_HELMET, HELMET_NAME),
        TANK_CHESTPLATE("tank-chestplate", Material.DIAMOND_CHESTPLATE, CHESTPLATE_NAME),
        TANK_LEGGINGS("tank-leggings", Material.DIAMOND_LEGGINGS, LEGGINGS_NAME),
        TANK_BOOTS("tank-boots", Material.DIAMOND_BOOTS, BOOTS_NAME),
        MACHETE("irradiated-machete", Material.DIAMOND_SWORD, SWORD_NAME),
        MEDKIT("medkit", Material.GLOWSTONE_DUST, MEDKIT_NAME);

        private final String key;
        private final Material material;
        private final String displayName;

        CustomItemType(String key, Material material, String displayName) {
            this.key = key;
            this.material = material;
            this.displayName = displayName;
        }

        public String getKey() {
            return key;
        }

        public Material getMaterial() {
            return material;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private enum ArmorPiece {
        HELMET(CustomItemType.TANK_HELMET, EquipmentSlot.HEAD, 0),
        CHESTPLATE(CustomItemType.TANK_CHESTPLATE, EquipmentSlot.CHEST, 0),
        BOOTS(CustomItemType.TANK_BOOTS, EquipmentSlot.FEET, 0);

        private final CustomItemType itemType;
        private final EquipmentSlot slot;
        private final int amplifier;

        ArmorPiece(CustomItemType itemType, EquipmentSlot slot, int amplifier) {
            this.itemType = itemType;
            this.slot = slot;
            this.amplifier = amplifier;
        }

        public CustomItemType getItemType() {
            return itemType;
        }

        public int getAmplifier() {
            return amplifier;
        }

        public ItemStack getItem(PlayerInventory inventory) {
            switch (slot) {
                case HEAD:
                    return inventory.getHelmet();
                case CHEST:
                    return inventory.getChestplate();
                case FEET:
                    return inventory.getBoots();
                default:
                    return null;
            }
        }
    }
}
