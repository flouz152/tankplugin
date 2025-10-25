package net.flouz.tank;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
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
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class TankPlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private NamespacedKey tankKey;
    private NamespacedKey medkitKey;
    private final Map<UUID, BukkitTask> activeMedkits = new HashMap<>();
    private final Random random = new Random();

    @Override
    public void onEnable() {
        this.tankKey = new NamespacedKey(this, "tank-item");
        this.medkitKey = new NamespacedKey(this, "medkit-item");

        PluginCommand pluginCommand = Objects.requireNonNull(getCommand("flouz1"),
                "Command flouz1 not defined in plugin.yml");
        pluginCommand.setExecutor(this);
        pluginCommand.setTabCompleter(this);

        Bukkit.getPluginManager().registerEvents(this, this);

        Bukkit.getScheduler().runTask(this, () ->
                Bukkit.getOnlinePlayers().forEach(this::updateEffects));
    }

    @Override
    public void onDisable() {
        activeMedkits.values().forEach(BukkitTask::cancel);
        activeMedkits.clear();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Эту команду может использовать только игрок.");
            return true;
        }

        Player player = (Player) sender;
        if (args.length < 1) {
            sendUsage(player);
            return true;
        }

        if (!args[0].equalsIgnoreCase("com") && !args[0].equalsIgnoreCase("tank")) {
            sendUsage(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("tank") && args.length >= 1) {
            if (args.length >= 2 && args[1].equalsIgnoreCase("Boottarmor")) {
                giveItem(player, EquipmentSlot.FEET);
                return true;
            }
            sendUsage(player);
            return true;
        }

        if (args.length < 2) {
            sendUsage(player);
            return true;
        }

        if (args[1].equalsIgnoreCase("tank")) {
            if (args.length < 3) {
                sendUsage(player);
                return true;
            }
            switch (args[2].toLowerCase(Locale.ROOT)) {
                case "helmet":
                    giveItem(player, EquipmentSlot.HEAD);
                    return true;
                case "bodyarmor":
                    giveItem(player, EquipmentSlot.CHEST);
                    return true;
                case "lenghtarmor":
                    giveItem(player, EquipmentSlot.LEGS);
                    return true;
                default:
                    sendUsage(player);
                    return true;
            }
        }

        if (args[1].equalsIgnoreCase("luckydayz")) {
            if (args.length < 3) {
                sendUsage(player);
                return true;
            }
            switch (args[2].toLowerCase(Locale.ROOT)) {
                case "obla":
                    giveSword(player);
                    return true;
                case "apteka":
                    giveMedkit(player);
                    return true;
                default:
                    sendUsage(player);
                    return true;
            }
        }

        sendUsage(player);
        return true;
    }

    private void sendUsage(Player player) {
        player.sendMessage(ChatColor.GRAY + "Использование:" );
        player.sendMessage(ChatColor.GRAY + "/flouz1 com tank helmet");
        player.sendMessage(ChatColor.GRAY + "/flouz1 com tank Bodyarmor");
        player.sendMessage(ChatColor.GRAY + "/flouz1 com tank Lenghtarmor");
        player.sendMessage(ChatColor.GRAY + "/flouz1 tank Boottarmor");
        player.sendMessage(ChatColor.GRAY + "/flouz1 com luckydayz obla");
        player.sendMessage(ChatColor.GRAY + "/flouz1 com luckydayz apteka");
    }

    private void giveItem(Player player, EquipmentSlot slot) {
        ItemStack item;
        switch (slot) {
            case HEAD:
                item = createArmor(Material.DIAMOND_HELMET, "ТАНК ШЛЕМ - М", "helmet");
                break;
            case CHEST:
                item = createArmor(Material.DIAMOND_CHESTPLATE, "ТАНК НАГРУДНИК - М", "chest");
                break;
            case LEGS:
                item = createArmor(Material.DIAMOND_LEGGINGS, "ТАНК ПОНОЖИ - М", "legs");
                break;
            case FEET:
                item = createArmor(Material.DIAMOND_BOOTS, "ТАНК БОТИНКИ - М", "boots");
                break;
            default:
                return;
        }
        player.getInventory().addItem(item);
    }

    private void giveSword(Player player) {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = sword.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.WHITE + "Облученная Мачете - М");
            meta.getPersistentDataContainer().set(tankKey, PersistentDataType.STRING, "sword");
            sword.setItemMeta(meta);
        }
        player.getInventory().addItem(sword);
    }

    private void giveMedkit(Player player) {
        ItemStack medkit = new ItemStack(Material.GLOWSTONE_DUST);
        ItemMeta meta = medkit.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.WHITE + "Аптечка");
            meta.getPersistentDataContainer().set(medkitKey, PersistentDataType.BYTE, (byte) 1);
            medkit.setItemMeta(meta);
        }
        player.getInventory().addItem(medkit);
    }

    private ItemStack createArmor(Material material, String name, String id) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.WHITE + name);
            meta.getPersistentDataContainer().set(tankKey, PersistentDataType.STRING, id);
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isTankItem(ItemStack stack, String id) {
        if (stack == null || stack.getType() == Material.AIR) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return id.equals(container.get(tankKey, PersistentDataType.STRING));
    }

    private boolean isMedkit(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(medkitKey, PersistentDataType.BYTE);
    }

    private void updateEffects(Player player) {
        PlayerInventory inv = player.getInventory();
        if (isTankItem(inv.getHelmet(), "helmet")) {
            ensureEffect(player, PotionEffectType.NIGHT_VISION, 0);
        } else {
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        }

        if (isTankItem(inv.getChestplate(), "chest")) {
            ensureEffect(player, PotionEffectType.REGENERATION, 0);
        } else {
            player.removePotionEffect(PotionEffectType.REGENERATION);
        }

        if (isTankItem(inv.getLeggings(), "legs")) {
            ensureEffect(player, PotionEffectType.ABSORPTION, 0);
        } else {
            player.removePotionEffect(PotionEffectType.ABSORPTION);
        }

        if (isTankItem(inv.getBoots(), "boots")) {
            ensureEffect(player, PotionEffectType.SPEED, 0);
        } else {
            player.removePotionEffect(PotionEffectType.SPEED);
        }

        if (hasTankSwordInHand(inv)) {
            ensureEffect(player, PotionEffectType.INCREASE_DAMAGE, 1);
        } else {
            player.removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
        }
    }

    private boolean hasTankSwordInHand(PlayerInventory inv) {
        return isTankItem(inv.getItemInMainHand(), "sword") || isTankItem(inv.getItemInOffHand(), "sword");
    }

    private void ensureEffect(Player player, PotionEffectType type, int amplifier) {
        PotionEffect current = player.getPotionEffect(type);
        if (current != null && current.getAmplifier() == amplifier && current.getDuration() > 200) {
            return;
        }
        PotionEffect effect = new PotionEffect(type, Integer.MAX_VALUE, amplifier, true, false, false);
        player.addPotionEffect(effect);
    }

    @EventHandler
    public void onArmorChange(PlayerArmorChangeEvent event) {
        Bukkit.getScheduler().runTask(this, () -> updateEffects(event.getPlayer()));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            Bukkit.getScheduler().runTask(this, () -> updateEffects(player));
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            if (isTankItem(player.getInventory().getLeggings(), "legs")) {
                ensureEffect(player, PotionEffectType.ABSORPTION, 0);
            }
            Bukkit.getScheduler().runTask(this, () -> updateEffects(player));
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            if (isTankItem(player.getInventory().getLeggings(), "legs")) {
                ensureEffect(player, PotionEffectType.ABSORPTION, 0);
            }
            Bukkit.getScheduler().runTask(this, () -> updateEffects(player));
        }
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (event.getNewSlot() == 0 || event.getNewSlot() == 2) {
            if (isTankItem(player.getInventory().getLeggings(), "legs")) {
                ensureEffect(player, PotionEffectType.ABSORPTION, 0);
            }
        }
        Bukkit.getScheduler().runTask(this, () -> updateEffects(player));
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Bukkit.getScheduler().runTask(this, () -> updateEffects(event.getPlayer()));
    }

    @EventHandler
    public void onItemBreak(PlayerItemBreakEvent event) {
        Bukkit.getScheduler().runTask(this, () -> updateEffects(event.getPlayer()));
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Bukkit.getScheduler().runTask(this, () -> updateEffects(event.getPlayer()));
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            Bukkit.getScheduler().runTask(this, () -> updateEffects(player));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(this, () -> updateEffects(event.getPlayer()), 1L);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (event.getHand() != EquipmentSlot.HAND && event.getHand() != EquipmentSlot.OFF_HAND) {
            return;
        }
        ItemStack item = event.getItem();
        if (!isMedkit(item)) {
            return;
        }

        event.setCancelled(true);

        Player player = event.getPlayer();
        if (activeMedkits.containsKey(player.getUniqueId())) {
            return;
        }

        BukkitRunnable runnable = new BukkitRunnable() {
            int seconds = 8;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    activeMedkits.remove(player.getUniqueId());
                    return;
                }
                player.sendMessage(ChatColor.WHITE.toString() + ChatColor.BOLD + "Аптека отхилит вас через " + seconds);
                if (seconds == 0) {
                    double max = Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getValue();
                    player.setHealth(max);
                    cancel();
                    activeMedkits.remove(player.getUniqueId());
                    return;
                }
                seconds--;
            }
        };

        BukkitTask task = runnable.runTaskTimer(this, 0L, 20L);
        activeMedkits.put(player.getUniqueId(), task);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getDamager();
        if (!hasTankSwordInHand(player.getInventory())) {
            return;
        }

        if (!isCriticalHit(player)) {
            return;
        }

        if (random.nextDouble() < 0.15d) {
            event.setDamage(event.getDamage() * 2.0d);
        }
    }

    private boolean isCriticalHit(Player player) {
        if (player.isOnGround()) {
            return false;
        }
        if (player.getFallDistance() <= 0.0F) {
            return false;
        }
        if (player.getVelocity().getY() >= 0.0D) {
            return false;
        }
        if (player.isSprinting()) {
            return false;
        }
        if (player.isInsideVehicle()) {
            return false;
        }
        if (player.hasPotionEffect(PotionEffectType.BLINDNESS)) {
            return false;
        }
        Material feetBlock = player.getLocation().getBlock().getType();
        if (feetBlock == Material.LADDER || feetBlock == Material.VINE) {
            return false;
        }
        if (player.getLocation().getBlock().isLiquid() || player.getEyeLocation().getBlock().isLiquid()) {
            return false;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.add("com");
            options.add("tank");
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("com")) {
                options.add("tank");
                options.add("luckydayz");
            } else if (args[0].equalsIgnoreCase("tank")) {
                options.add("Boottarmor");
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("com") && args[1].equalsIgnoreCase("tank")) {
                options.add("helmet");
                options.add("Bodyarmor");
                options.add("Lenghtarmor");
            } else if (args[0].equalsIgnoreCase("com") && args[1].equalsIgnoreCase("luckydayz")) {
                options.add("obla");
                options.add("apteka");
            }
        }

        String current = args.length == 0 ? "" : args[args.length - 1];
        return options.stream()
                .filter(opt -> opt.toLowerCase(Locale.ROOT).startsWith(current.toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());
    }
}
