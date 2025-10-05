package com.flouz1.tankplugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
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

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import com.destroystokyo.paper.event.player.PlayerSteerVehicleEvent;

public class TankPlugin extends JavaPlugin implements Listener, TabCompleter {

    private static final String HELMET_NAME = "ТАНК ШЛЕМ - М";
    private static final String CHESTPLATE_NAME = "ТАНК НАГРУДНИК - М";
    private static final String LEGGINGS_NAME = "ТАНК ПОНОЖИ - М";
    private static final String BOOTS_NAME = "ТАНК БОТИНКИ - М";
    private static final String SWORD_NAME = "Облученная Мачете - М";
    private static final String MEDKIT_NAME = "Аптечка";

    private BukkitTask effectTask;
    private final Set<UUID> activeMedkits = new HashSet<>();
    private final Map<UUID, MedkitSession> medkitSessions = new HashMap<>();
    private final Map<UUID, Helicopter> helicopters = new HashMap<>();
    private final Map<UUID, UUID> seatOwners = new HashMap<>();
    private final Map<UUID, ControlState> controlStates = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        if (getCommand("flouz1") != null) {
            getCommand("flouz1").setTabCompleter(this);
        }
        effectTask = Bukkit.getScheduler().runTaskTimer(this, this::updateEffects, 1L, 1L);
    }

    @Override
    public void onDisable() {
        if (effectTask != null) {
            effectTask.cancel();
            effectTask = null;
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

        if (!args[0].equalsIgnoreCase("com") || args.length < 2) {
            player.sendMessage(ChatColor.RED + "Неизвестная команда.");
            return true;
        }

        if (args[1].equalsIgnoreCase("tank")) {
            if (args.length < 3) {
                player.sendMessage(ChatColor.RED + "Укажите предмет: helmet, chestplate, leggings, boots.");
                return true;
            }
            switch (args[2].toLowerCase()) {
                case "helmet":
                    giveItem(player, createNamedItem(Material.DIAMOND_HELMET, HELMET_NAME));
                    player.sendMessage(ChatColor.GREEN + "Вы получили " + HELMET_NAME + ChatColor.GREEN + ".");
                    return true;
                case "chestplate":
                    giveItem(player, createNamedItem(Material.DIAMOND_CHESTPLATE, CHESTPLATE_NAME));
                    player.sendMessage(ChatColor.GREEN + "Вы получили " + CHESTPLATE_NAME + ChatColor.GREEN + ".");
                    return true;
                case "leggings":
                    giveItem(player, createNamedItem(Material.DIAMOND_LEGGINGS, LEGGINGS_NAME));
                    player.sendMessage(ChatColor.GREEN + "Вы получили " + LEGGINGS_NAME + ChatColor.GREEN + ".");
                    return true;
                case "boots":
                    giveItem(player, createNamedItem(Material.DIAMOND_BOOTS, BOOTS_NAME));
                    player.sendMessage(ChatColor.GREEN + "Вы получили " + BOOTS_NAME + ChatColor.GREEN + ".");
                    return true;
                default:
                    player.sendMessage(ChatColor.RED + "Неизвестный предмет танка.");
                    return true;
            }
        }

        if (args[1].equalsIgnoreCase("luckydayz")) {
            if (args.length < 3) {
                player.sendMessage(ChatColor.RED + "Укажите предмет: machete, medkit, helicopter.");
                return true;
            }

            switch (args[2].toLowerCase()) {
                case "machete":
                    giveItem(player, createNamedItem(Material.DIAMOND_SWORD, SWORD_NAME));
                    player.sendMessage(ChatColor.GREEN + "Вы получили " + SWORD_NAME + ChatColor.GREEN + ".");
                    return true;
                case "medkit":
                    giveItem(player, createNamedItem(Material.GLOWSTONE_DUST, MEDKIT_NAME));
                    player.sendMessage(ChatColor.GREEN + "Вы получили аптечку.");
                    return true;
                case "helicopter":
                    if (args.length >= 4 && args[3].equalsIgnoreCase("speed")) {
                        if (args.length == 5) {
                            try {
                                double speed = Double.parseDouble(args[4]);
                                if (!Double.isFinite(speed) || speed <= 0 || speed > 2) {
                                    player.sendMessage(ChatColor.RED + "Скорость должна быть в диапазоне (0; 2].");
                                    return true;
                                }
                                Helicopter helicopter = helicopters.get(player.getUniqueId());
                                if (helicopter == null) {
                                    player.sendMessage(ChatColor.RED + "Сначала вызовите вертолет.");
                                    return true;
                                }
                                helicopter.setSpeed(speed);
                                player.sendMessage(ChatColor.GREEN + "Скорость вертолета установлена на " + speed + ".");
                                return true;
                            } catch (NumberFormatException ex) {
                                player.sendMessage(ChatColor.RED + "Скорость должна быть числом.");
                                return true;
                            }
                        }
                        player.sendMessage(ChatColor.YELLOW + "Использование: /flouz1 com luckydayz helicopter speed <значение>.");
                        return true;
                    }
                    spawnHelicopter(player);
                    return true;
                default:
                    player.sendMessage(ChatColor.RED + "Неизвестная команда luckydayz.");
                    return true;
            }
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
            stack.setItemMeta(meta);
        }
        return stack;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(this, () -> updateEffectsFor(event.getPlayer()), 1L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        activeMedkits.remove(event.getPlayer().getUniqueId());
        removeHelicopter(event.getPlayer().getUniqueId());
        MedkitSession session = medkitSessions.remove(event.getPlayer().getUniqueId());
        if (session != null) {
            session.restore(event.getPlayer());
        }
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
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Bukkit.getScheduler().runTaskLater(this, () -> updateEffectsFor((Player) event.getWhoClicked()), 1L);
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
    public void onArmorChange(PlayerArmorChangeEvent event) {
        Bukkit.getScheduler().runTaskLater(this, () -> updateEffectsFor(event.getPlayer()), 1L);
    }

    @EventHandler
    public void onSwapItems(PlayerSwapHandItemsEvent event) {
        Bukkit.getScheduler().runTaskLater(this, () -> updateEffectsFor(event.getPlayer()), 1L);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        EquipmentSlot handUsed = event.getHand();
        if (handUsed != EquipmentSlot.HAND && handUsed != EquipmentSlot.OFF_HAND) {
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

        AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double maxHealth = attribute != null ? attribute.getValue() : player.getMaxHealth();
        if (player.getHealth() >= maxHealth) {
            player.sendMessage(ChatColor.YELLOW + "У вас полное здоровье. Аптечка не требуется.");
            activeMedkits.remove(uuid);
            return;
        }

        PlayerInventory inventory = player.getInventory();
        ItemStack hand = event.getItem();
        if (hand != null) {
            int amount = hand.getAmount();
            if (amount > 1) {
                hand.setAmount(amount - 1);
            } else {
                if (handUsed == EquipmentSlot.HAND) {
                    inventory.setItemInMainHand(null);
                } else {
                    inventory.setItemInOffHand(null);
                }
            }
        }

        MedkitSession session = new MedkitSession(player.getLevel(), player.getExp());
        medkitSessions.put(uuid, session);

        new BukkitRunnable() {
            int counter = 8;
            int progressStep = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    cleanupMedkit(uuid, player);
                    return;
                }

                progressStep = Math.min(progressStep + 1, 10);
                session.applyProgress(player, progressStep);
                if (counter >= 0) {
                    player.sendMessage(ChatColor.WHITE.toString() + ChatColor.BOLD + "Аптека отхилит вас через " + counter);
                }

                if (counter == 0) {
                    player.setHealth(maxHealth);
                    session.applyProgress(player, 10);
                    Bukkit.getScheduler().runTaskLater(TankPlugin.this, () -> cleanupMedkit(uuid, player), 2L);
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
        Helicopter helicopter = helicopters.get(ownerId);
        if (helicopter != null) {
            helicopter.start();
        }
    }

    @EventHandler
    public void onPlayerSteer(PlayerSteerVehicleEvent event) {
        Player player = event.getPlayer();
        Helicopter helicopter = helicopters.get(player.getUniqueId());
        if (helicopter == null || !helicopter.isDriver(player)) {
            return;
        }

        ControlState state = controlStates.computeIfAbsent(player.getUniqueId(), ignored -> new ControlState());
        float forward = event.getForward();
        float sideways = event.getSideways();
        state.forward = forward > 0.01F;
        state.turnLeft = sideways < -0.01F;
        state.turnRight = sideways > 0.01F;
        state.up = event.isJumping();
        state.down = forward < -0.01F || event.isSneaking();
        event.setCancelled(true);
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

        Helicopter helicopter = new Helicopter(this, seat, parts, player.getUniqueId());
        helicopters.put(uuid, helicopter);
        seatOwners.put(seat.getUniqueId(), uuid);
        controlStates.put(uuid, new ControlState());
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
        helicopter.stop();
        ArmorStand seat = helicopter.getSeat();
        seatOwners.remove(seat.getUniqueId());
        seat.remove();
        for (ArmorStand part : helicopter.getParts()) {
            part.remove();
        }
        controlStates.remove(owner);
    }

    private void cleanupMedkit(UUID uuid, Player player) {
        activeMedkits.remove(uuid);
        MedkitSession session = medkitSessions.remove(uuid);
        if (session != null && player.isOnline()) {
            session.restore(player);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("flouz1")) {
            return null;
        }

        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            suggestions.add("com");
            return filterPrefix(suggestions, args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("com")) {
            suggestions.add("tank");
            suggestions.add("luckydayz");
            return filterPrefix(suggestions, args[1]);
        }

        if (args[0].equalsIgnoreCase("com") && args[1].equalsIgnoreCase("tank")) {
            if (args.length == 3) {
                suggestions.add("helmet");
                suggestions.add("chestplate");
                suggestions.add("leggings");
                suggestions.add("boots");
                return filterPrefix(suggestions, args[2]);
            }
            return suggestions;
        }

        if (args[0].equalsIgnoreCase("com") && args[1].equalsIgnoreCase("luckydayz")) {
            if (args.length == 3) {
                suggestions.add("machete");
                suggestions.add("medkit");
                suggestions.add("helicopter");
                return filterPrefix(suggestions, args[2]);
            }
            if (args.length == 4 && args[2].equalsIgnoreCase("helicopter")) {
                suggestions.add("speed");
                return filterPrefix(suggestions, args[3]);
            }
            if (args.length == 5 && args[2].equalsIgnoreCase("helicopter") && args[3].equalsIgnoreCase("speed")) {
                suggestions.add("0.4");
                suggestions.add("0.8");
                suggestions.add("1.0");
                suggestions.add("1.5");
                suggestions.add("2.0");
                return filterPrefix(suggestions, args[4]);
            }
        }
        return suggestions;
    }

    private List<String> filterPrefix(List<String> options, String current) {
        String lower = current == null ? "" : current.toLowerCase();
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }

    private static class Helicopter {
        private static final float TURN_STEP = 4.0F;

        private final TankPlugin plugin;
        private final ArmorStand seat;
        private final List<ArmorStand> parts;
        private final UUID owner;
        private double speed = 0.4D;
        private BukkitRunnable task;

        Helicopter(TankPlugin plugin, ArmorStand seat, List<ArmorStand> parts, UUID owner) {
            this.plugin = plugin;
            this.seat = seat;
            this.parts = parts;
            this.owner = owner;
        }

        ArmorStand getSeat() {
            return seat;
        }

        List<ArmorStand> getParts() {
            return parts;
        }

        boolean isDriver(Player player) {
            return seat.getPassengers().contains(player);
        }

        void start() {
            if (task != null) {
                return;
            }
            task = new BukkitRunnable() {
                @Override
                public void run() {
                    Player player = Bukkit.getPlayer(owner);
                    if (player == null || !player.isOnline()) {
                        stop();
                        plugin.removeHelicopter(owner);
                        return;
                    }
                    if (!seat.getPassengers().contains(player)) {
                        return;
                    }
                    ControlState state = plugin.controlStates.computeIfAbsent(owner, ignored -> new ControlState());
                    Location seatLocation = seat.getLocation();
                    float yaw = seatLocation.getYaw();
                    if (state.turnLeft) {
                        yaw -= TURN_STEP;
                    }
                    if (state.turnRight) {
                        yaw += TURN_STEP;
                    }
                    if (yaw > 180.0F) {
                        yaw -= 360.0F;
                    } else if (yaw < -180.0F) {
                        yaw += 360.0F;
                    }
                    seatLocation.setYaw(yaw);
                    seat.teleport(seatLocation);

                    Location playerLocation = player.getLocation();
                    player.setRotation(yaw, playerLocation.getPitch());

                    double radians = Math.toRadians(yaw);
                    Vector move = new Vector(0, 0, 0);
                    Vector forward = new Vector(-Math.sin(radians), 0, Math.cos(radians));

                    if (state.forward) {
                        move.add(forward);
                    }
                    if (state.up) {
                        move.setY(move.getY() + 1);
                    }
                    if (state.down) {
                        move.setY(move.getY() - 1);
                    }

                    if (move.lengthSquared() > 0) {
                        move.normalize().multiply(speed / 5.0D);
                        Location newLocation = seat.getLocation().add(move);
                        seat.teleport(newLocation);
                        syncParts(move);
                    }
                    state.clear();
                }
            };
            task.runTaskTimer(plugin, 0L, 1L);
        }

        void stop() {
            if (task != null) {
                task.cancel();
                task = null;
            }
        }

        void syncParts(Vector delta) {
            for (ArmorStand part : parts) {
                part.teleport(part.getLocation().add(delta));
            }
        }

        void setSpeed(double speed) {
            this.speed = speed;
        }
    }

    private static class ControlState {
        boolean forward;
        boolean turnLeft;
        boolean turnRight;
        boolean up;
        boolean down;

        void clear() {
            forward = false;
            turnLeft = false;
            turnRight = false;
            up = false;
            down = false;
        }
    }

    private static class MedkitSession {
        private final int level;
        private final float exp;

        MedkitSession(int level, float exp) {
            this.level = level;
            this.exp = exp;
        }

        void applyProgress(Player player, int step) {
            player.setLevel(0);
            float progress = Math.min(1.0F, step / 10.0F);
            player.setExp(progress);
        }

        void restore(Player player) {
            player.setLevel(level);
            player.setExp(exp);
        }
    }
}
