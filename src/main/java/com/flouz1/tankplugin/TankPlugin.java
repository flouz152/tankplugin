package com.flouz1.tankplugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
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
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;

public class TankPlugin extends JavaPlugin implements Listener, TabCompleter {

    private static final String HELMET_NAME = ChatColor.GOLD + "ТАНК ШЛЕМ" + ChatColor.DARK_GRAY + " • " + ChatColor.YELLOW + "<М>";
    private static final String CHESTPLATE_NAME = ChatColor.GOLD + "ТАНК НАГРУДНИК" + ChatColor.DARK_GRAY + " • " + ChatColor.YELLOW + "<М>";
    private static final String LEGGINGS_NAME = ChatColor.GOLD + "ТАНК ПОНОЖИ" + ChatColor.DARK_GRAY + " • " + ChatColor.YELLOW + "<М>";
    private static final String BOOTS_NAME = ChatColor.GOLD + "ТАНК БОТИНКИ" + ChatColor.DARK_GRAY + " • " + ChatColor.YELLOW + "<М>";
    private static final String SWORD_NAME = ChatColor.DARK_AQUA + "Облучённая Мачете" + ChatColor.DARK_GRAY + " • " + ChatColor.RED + "<М>";
    private static final String MEDKIT_NAME = ChatColor.GREEN + "Медкомплект \"Аптека\"";
    private static final String RPD_NAME = ChatColor.DARK_RED + "РПД ПРЕМУМ" + ChatColor.DARK_GRAY + " • " + ChatColor.RED + "<100>";
    private static final String REMINGTON_NAME = ChatColor.GOLD + "Ремингтон 870" + ChatColor.DARK_GRAY + " • " + ChatColor.GREEN + "<6>";
    private static final String AMMO_LORE_PREFIX = ChatColor.GRAY + "Боезапас: ";
    private static final String AMMO_STRIPPED_PREFIX = "Боезапас:";

    private static final double RPD_DAMAGE = 9.0D;
    private static final double REMINGTON_DAMAGE = 14.0D;
    private static final double RPD_SPEED = 2.6D;
    private static final double REMINGTON_SPEED = 2.0D;
    private static final int RPD_COOLDOWN_TICKS = 4;
    private static final int REMINGTON_COOLDOWN_TICKS = 16;

    private static final List<String> HELMET_LORE = createLore(
            ChatColor.GRAY + "Категория: " + ChatColor.YELLOW + "Шлем",
            ChatColor.DARK_PURPLE + "Сканер ночного видения"
    );
    private static final List<String> CHESTPLATE_LORE = createLore(
            ChatColor.GRAY + "Категория: " + ChatColor.YELLOW + "Броня",
            ChatColor.LIGHT_PURPLE + "Поддержка нанорегенерации"
    );
    private static final List<String> LEGGINGS_LORE = createLore(
            ChatColor.GRAY + "Категория: " + ChatColor.YELLOW + "Броня",
            ChatColor.AQUA + "Экранирование удара: " + ChatColor.GOLD + "+2 золотых сердца",
            ChatColor.DARK_GRAY + "Активируется при манёврах"
    );
    private static final List<String> BOOTS_LORE = createLore(
            ChatColor.GRAY + "Категория: " + ChatColor.YELLOW + "Броня",
            ChatColor.GREEN + "Усиленные сервоприводы скорости"
    );
    private static final List<String> SWORD_LORE = createLore(
            ChatColor.GRAY + "Тип: " + ChatColor.RED + "Тактическое мачете",
            ChatColor.GRAY + "Эффект: " + ChatColor.DARK_AQUA + "Сила II",
            ChatColor.DARK_GRAY + "Хранить при себе для активации"
    );
    private static final List<String> MEDKIT_LORE = createLore(
            ChatColor.GRAY + "Тип: " + ChatColor.GREEN + "Медицинский комплект",
            ChatColor.GRAY + "Применение: " + ChatColor.WHITE + "ПКМ",
            ChatColor.GRAY + "Требование: " + ChatColor.YELLOW + "Отсутствие полного HP",
            ChatColor.DARK_GREEN + "Запускает отсчёт и восстанавливает здоровье"
    );
    private static final List<String> RPD_LORE = createLore(
            ChatColor.GRAY + "C418 - stal",
            "",
            ChatColor.RED + "Урон: " + ChatColor.GOLD + "■■■■■■■■■■" + ChatColor.DARK_GRAY + "■■",
            ChatColor.RED + "Скорострельность: " + ChatColor.GOLD + "■■■■■■■■■■■■",
            ChatColor.RED + "Точность: " + ChatColor.GOLD + "■■■■■■■■" + ChatColor.DARK_GRAY + "■■■■",
            ChatColor.RED + "Дальность: " + ChatColor.GOLD + "■■■■■■■■■" + ChatColor.DARK_GRAY + "■■■",
            ChatColor.RED + "Мобильность: " + ChatColor.GOLD + "■■■■■■" + ChatColor.DARK_GRAY + "■■■■■",
            "",
            ChatColor.GRAY + "Тип: " + ChatColor.RED + "Пулемёт",
            ChatColor.GRAY + "Магазин: " + ChatColor.YELLOW + "100",
            ChatColor.GRAY + "Боеприпасы: " + ChatColor.YELLOW + "7,62×39 мм",
            ChatColor.GRAY + "Перезарядка: " + ChatColor.YELLOW + "4 сек.",
            "",
            ChatColor.GOLD + "Модификации:",
            ChatColor.YELLOW + "↑ " + ChatColor.GRAY + "УВЕЛИЧЕН УРОН",
            ChatColor.YELLOW + "↑ " + ChatColor.GRAY + "УВЕЛИЧЕННАЯ ТОЧНОСТЬ",
            ChatColor.YELLOW + "↑ " + ChatColor.GRAY + "УВЕЛИЧЕННАЯ СКОРОСТРЕЛЬНОСТЬ",
            ChatColor.YELLOW + "↓ " + ChatColor.GRAY + "УМЕНЬШЕНА ОТДАЧА"
    );
    private static final List<String> REMINGTON_LORE = createLore(
            ChatColor.GRAY + "C418 - blocks",
            "",
            ChatColor.RED + "Урон: " + ChatColor.GOLD + "■■■■■■■■■" + ChatColor.DARK_GRAY + "■■■",
            ChatColor.RED + "Скорострельность: " + ChatColor.GOLD + "■■■" + ChatColor.DARK_GRAY + "■■■■■■■",
            ChatColor.RED + "Точность: " + ChatColor.GOLD + "■■■■■■■" + ChatColor.DARK_GRAY + "■■■",
            ChatColor.RED + "Дальность: " + ChatColor.GOLD + "■■■■■" + ChatColor.DARK_GRAY + "■■■■",
            ChatColor.RED + "Мобильность: " + ChatColor.GOLD + "■■■■■■■■" + ChatColor.DARK_GRAY + "■■",
            "",
            ChatColor.GRAY + "Тип: " + ChatColor.RED + "Помповое ружьё",
            ChatColor.GRAY + "Магазин: " + ChatColor.YELLOW + "6",
            ChatColor.GRAY + "Боеприпасы: " + ChatColor.YELLOW + "12×70 мм",
            ChatColor.GRAY + "Перезарядка: " + ChatColor.YELLOW + "0.75 сек."
    );

    private BukkitTask effectTask;
    private NamespacedKey ammoKey;
    private Class<? extends Event> steerEventClass;
    private Method steerGetPlayer;
    private Method steerGetForward;
    private Method steerGetSideways;
    private Method steerIsJumping;
    private Method steerIsSneaking;
    private Method steerSetCancelled;
    private final Set<UUID> activeMedkits = new HashSet<>();
    private final Map<UUID, MedkitSession> medkitSessions = new HashMap<>();
    private final Map<UUID, Helicopter> helicopters = new HashMap<>();
    private final Map<UUID, UUID> seatOwners = new HashMap<>();
    private final Map<UUID, ControlState> controlStates = new ConcurrentHashMap<>();
    private final Map<UUID, WeaponType> projectileTypes = new HashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        if (getCommand("flouz1") != null) {
            getCommand("flouz1").setTabCompleter(this);
        }
        ammoKey = new NamespacedKey(this, "ammo");
        setupSteerReflection();
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
        clearSteerReflection();
    }

    @SuppressWarnings("unchecked")
    private void setupSteerReflection() {
        try {
            Class<?> clazz = Class.forName("com.destroystokyo.paper.event.player.PlayerSteerVehicleEvent");
            if (!Event.class.isAssignableFrom(clazz)) {
                return;
            }
            steerEventClass = (Class<? extends Event>) clazz;
            steerGetPlayer = clazz.getMethod("getPlayer");
            steerGetForward = clazz.getMethod("getForward");
            steerGetSideways = clazz.getMethod("getSideways");
            steerIsJumping = clazz.getMethod("isJumping");
            steerIsSneaking = clazz.getMethod("isSneaking");
            steerSetCancelled = clazz.getMethod("setCancelled", boolean.class);

            Bukkit.getPluginManager().registerEvent(steerEventClass, this, EventPriority.NORMAL,
                    (listener, event) -> handleSteerVehicle(event), this);
        } catch (ClassNotFoundException ex) {
            getLogger().warning("PlayerSteerVehicleEvent недоступен. Вертолёт будет ограничен в управлении.");
        } catch (NoSuchMethodException ex) {
            clearSteerReflection();
            getLogger().log(Level.SEVERE, "Не удалось инициализировать управление вертолётом", ex);
        } catch (Throwable throwable) {
            clearSteerReflection();
            getLogger().log(Level.SEVERE, "Не удалось подключить управление вертолётом", throwable);
        }
    }

    private void clearSteerReflection() {
        steerEventClass = null;
        steerGetPlayer = null;
        steerGetForward = null;
        steerGetSideways = null;
        steerIsJumping = null;
        steerIsSneaking = null;
        steerSetCancelled = null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Команду может выполнять только игрок.");
            return true;
        }

        Player player = (Player) sender;
        if (args.length == 0) {
            sendCommandOverview(player);
            return true;
        }

        if (!args[0].equalsIgnoreCase("com") || args.length < 2) {
            sendCommandOverview(player);
            return true;
        }

        if (args[1].equalsIgnoreCase("tank")) {
            if (args.length < 3) {
                player.sendMessage(ChatColor.YELLOW + "Укажите элемент сета: helmet, chestplate, leggings, boots.");
                return true;
            }
            switch (args[2].toLowerCase()) {
                case "helmet":
                    giveItem(player, createStyledItem(Material.DIAMOND_HELMET, HELMET_NAME, HELMET_LORE));
                    player.sendMessage(ChatColor.GREEN + "Вы получили " + HELMET_NAME + ChatColor.GREEN + ".");
                    return true;
                case "chestplate":
                    giveItem(player, createStyledItem(Material.DIAMOND_CHESTPLATE, CHESTPLATE_NAME, CHESTPLATE_LORE));
                    player.sendMessage(ChatColor.GREEN + "Вы получили " + CHESTPLATE_NAME + ChatColor.GREEN + ".");
                    return true;
                case "leggings":
                    giveItem(player, createStyledItem(Material.DIAMOND_LEGGINGS, LEGGINGS_NAME, LEGGINGS_LORE));
                    player.sendMessage(ChatColor.GREEN + "Вы получили " + LEGGINGS_NAME + ChatColor.GREEN + ".");
                    return true;
                case "boots":
                    giveItem(player, createStyledItem(Material.DIAMOND_BOOTS, BOOTS_NAME, BOOTS_LORE));
                    player.sendMessage(ChatColor.GREEN + "Вы получили " + BOOTS_NAME + ChatColor.GREEN + ".");
                    return true;
                default:
                    player.sendMessage(ChatColor.RED + "Неизвестный предмет танка.");
                    return true;
            }
        }

        if (args[1].equalsIgnoreCase("luckydayz")) {
            if (args.length < 3) {
                player.sendMessage(ChatColor.YELLOW + "Укажите предмет: machete, medkit, rpd, remington, helicopter.");
                return true;
            }

            switch (args[2].toLowerCase()) {
                case "machete":
                    giveItem(player, createStyledItem(Material.DIAMOND_SWORD, SWORD_NAME, SWORD_LORE));
                    player.sendMessage(ChatColor.GREEN + "Вы получили " + SWORD_NAME + ChatColor.GREEN + ".");
                    return true;
                case "medkit":
                    giveItem(player, createStyledItem(Material.GLOWSTONE_DUST, MEDKIT_NAME, MEDKIT_LORE));
                    player.sendMessage(ChatColor.GREEN + "Вы получили " + MEDKIT_NAME + ChatColor.GREEN + ".");
                    return true;
                case "rpd":
                    ItemStack rpd = createStyledItem(Material.MUSIC_DISC_STAL, RPD_NAME, RPD_LORE);
                    setWeaponAmmo(rpd, WeaponType.RPD, WeaponType.RPD.getMaxAmmo());
                    giveItem(player, rpd);
                    player.sendMessage(ChatColor.GREEN + "Вы получили " + RPD_NAME + ChatColor.GREEN + ".");
                    return true;
                case "remington":
                    ItemStack remington = createStyledItem(Material.MUSIC_DISC_BLOCKS, REMINGTON_NAME, REMINGTON_LORE);
                    setWeaponAmmo(remington, WeaponType.REMINGTON, WeaponType.REMINGTON.getMaxAmmo());
                    giveItem(player, remington);
                    player.sendMessage(ChatColor.GREEN + "Вы получили " + REMINGTON_NAME + ChatColor.GREEN + ".");
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

        sendCommandOverview(player);
        return true;
    }

    private void giveItem(Player player, ItemStack item) {
        player.getInventory().addItem(item);
    }

    private ItemStack createStyledItem(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) {
                meta.setLore(new ArrayList<>(lore));
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static List<String> createLore(String... lines) {
        List<String> lore = new ArrayList<>();
        for (String line : lines) {
            lore.add(line);
        }
        return lore;
    }

    private void sendCommandOverview(Player player) {
        player.sendMessage(ChatColor.AQUA + "Доступные команды:");
        player.sendMessage(ChatColor.GRAY + "/flouz1 com tank helmet" + ChatColor.DARK_GRAY + " — " + ChatColor.YELLOW + ChatColor.stripColor(HELMET_NAME));
        player.sendMessage(ChatColor.GRAY + "/flouz1 com tank chestplate" + ChatColor.DARK_GRAY + " — " + ChatColor.YELLOW + ChatColor.stripColor(CHESTPLATE_NAME));
        player.sendMessage(ChatColor.GRAY + "/flouz1 com tank leggings" + ChatColor.DARK_GRAY + " — " + ChatColor.YELLOW + ChatColor.stripColor(LEGGINGS_NAME));
        player.sendMessage(ChatColor.GRAY + "/flouz1 com tank boots" + ChatColor.DARK_GRAY + " — " + ChatColor.YELLOW + ChatColor.stripColor(BOOTS_NAME));
        player.sendMessage(ChatColor.GRAY + "/flouz1 com luckydayz machete" + ChatColor.DARK_GRAY + " — " + ChatColor.YELLOW + ChatColor.stripColor(SWORD_NAME));
        player.sendMessage(ChatColor.GRAY + "/flouz1 com luckydayz medkit" + ChatColor.DARK_GRAY + " — " + ChatColor.YELLOW + ChatColor.stripColor(MEDKIT_NAME));
        player.sendMessage(ChatColor.GRAY + "/flouz1 com luckydayz rpd" + ChatColor.DARK_GRAY + " — " + ChatColor.YELLOW + ChatColor.stripColor(RPD_NAME));
        player.sendMessage(ChatColor.GRAY + "/flouz1 com luckydayz remington" + ChatColor.DARK_GRAY + " — " + ChatColor.YELLOW + ChatColor.stripColor(REMINGTON_NAME));
        player.sendMessage(ChatColor.GRAY + "/flouz1 com luckydayz helicopter" + ChatColor.DARK_GRAY + " — " + ChatColor.YELLOW + "личный вертолёт");
        player.sendMessage(ChatColor.GRAY + "/flouz1 com luckydayz helicopter speed <0.1-2.0>" + ChatColor.DARK_GRAY + " — " + ChatColor.YELLOW + "настройка скорости вертолёта");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        updateEffectsFor(player);
        Bukkit.getScheduler().runTask(this, () -> updateEffectsFor(player));
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
        updateEffectsFor(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Bukkit.getScheduler().runTask(this, () -> updateEffectsFor((Player) event.getWhoClicked()));
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            updateEffectsFor(player);
            if (isWearingTankLeggings(player)) {
                applyAbsorption(player);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            updateEffectsFor(player);
            if (isWearingTankLeggings(player)) {
                applyAbsorption(player);
            }
        }
    }

    @EventHandler
    public void onArmorChange(PlayerArmorChangeEvent event) {
        updateEffectsFor(event.getPlayer());
    }

    @EventHandler
    public void onSwapItems(PlayerSwapHandItemsEvent event) {
        updateEffectsFor(event.getPlayer());
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
        Player player = event.getPlayer();

        if (isNamedItem(item, Material.GLOWSTONE_DUST, MEDKIT_NAME)) {
            handleMedkitUse(event, player, handUsed);
            return;
        }

        if (isNamedItem(item, Material.MUSIC_DISC_STAL, RPD_NAME)) {
            handleWeaponFire(event, player, item, WeaponType.RPD);
            return;
        }

        if (isNamedItem(item, Material.MUSIC_DISC_BLOCKS, REMINGTON_NAME)) {
            handleWeaponFire(event, player, item, WeaponType.REMINGTON);
        }
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

    private void handleSteerVehicle(Event event) {
        if (steerEventClass == null || !steerEventClass.isInstance(event)) {
            return;
        }
        try {
            Object playerObject = steerGetPlayer.invoke(event);
            if (!(playerObject instanceof Player)) {
                return;
            }
            Player player = (Player) playerObject;
            Helicopter helicopter = helicopters.get(player.getUniqueId());
            if (helicopter == null || !helicopter.isDriver(player)) {
                return;
            }

            ControlState state = controlStates.computeIfAbsent(player.getUniqueId(), ignored -> new ControlState());
            float forward = ((Number) steerGetForward.invoke(event)).floatValue();
            float sideways = ((Number) steerGetSideways.invoke(event)).floatValue();
            boolean jumping = (Boolean) steerIsJumping.invoke(event);
            boolean sneaking = (Boolean) steerIsSneaking.invoke(event);

            state.forward = forward > 0.01F;
            state.turnLeft = sideways < -0.01F;
            state.turnRight = sideways > 0.01F;
            state.up = jumping;
            state.down = forward < -0.01F || sneaking;

            steerSetCancelled.invoke(event, true);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            getLogger().log(Level.SEVERE, "Ошибка обработки управления вертолётом", ex);
        }
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

        if (isWearingTankLeggings(player)) {
            applyAbsorption(player);
        } else {
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
        parts.add(createPart(base.clone().add(0, -0.2, -0.9), Material.GRAY_CONCRETE, new Vector(0, 0, 0)));
        parts.add(createPart(base.clone().add(0.9, -0.2, -0.1), Material.GRAY_CONCRETE, new Vector(0, 0, 0)));
        parts.add(createPart(base.clone().add(-0.9, -0.2, -0.1), Material.GRAY_CONCRETE, new Vector(0, 0, 0)));
        parts.add(createPart(base.clone().add(0, 0.9, 0), Material.BLACK_CONCRETE, new Vector(0, 0, 0)));
        parts.add(createPart(base.clone().add(0, 1.2, 0), Material.IRON_BARS, new Vector(0, 0, 0)));

        Helicopter helicopter = new Helicopter(this, seat, parts, player.getUniqueId());
        helicopters.put(uuid, helicopter);
        seatOwners.put(seat.getUniqueId(), uuid);
        controlStates.put(uuid, new ControlState());
        helicopter.start();
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

    @EventHandler
    public void onProjectileDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (!(damager instanceof Projectile)) {
            return;
        }
        Projectile projectile = (Projectile) damager;
        WeaponType type = projectileTypes.remove(projectile.getUniqueId());
        if (type == null) {
            return;
        }
        if (!(projectile.getShooter() instanceof Player)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }
        event.setDamage(type.getDamage());
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        if (projectileTypes.remove(projectile.getUniqueId()) != null) {
            projectile.remove();
        }
    }

    private void setWeaponAmmo(ItemStack item, WeaponType type, int ammo) {
        if (item == null || type == null) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        int clamped = Math.max(0, Math.min(ammo, type.getMaxAmmo()));
        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (ammoKey != null) {
            container.set(ammoKey, PersistentDataType.INTEGER, clamped);
        }
        List<String> lore = meta.getLore();
        List<String> updatedLore = new ArrayList<>();
        if (lore != null) {
            for (String line : lore) {
                if (line != null && ChatColor.stripColor(line).startsWith(AMMO_STRIPPED_PREFIX)) {
                    continue;
                }
                updatedLore.add(line);
            }
        }
        updatedLore.add(AMMO_LORE_PREFIX + ChatColor.YELLOW + clamped + ChatColor.GRAY + " / " + ChatColor.YELLOW + type.getMaxAmmo());
        meta.setLore(updatedLore);
        item.setItemMeta(meta);
    }

    private int getWeaponAmmo(ItemStack item, WeaponType type) {
        if (item == null || type == null) {
            return 0;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (ammoKey == null || !container.has(ammoKey, PersistentDataType.INTEGER)) {
            return type.getMaxAmmo();
        }
        Integer stored = container.get(ammoKey, PersistentDataType.INTEGER);
        if (stored == null) {
            return type.getMaxAmmo();
        }
        return Math.max(0, Math.min(stored, type.getMaxAmmo()));
    }

    private void updateWeaponStack(Player player, EquipmentSlot slot, ItemStack item) {
        if (slot == EquipmentSlot.OFF_HAND) {
            player.getInventory().setItemInOffHand(item);
        } else {
            player.getInventory().setItemInMainHand(item);
        }
    }

    private void handleMedkitUse(PlayerInteractEvent event, Player player, EquipmentSlot handUsed) {
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
            } else if (handUsed == EquipmentSlot.HAND) {
                inventory.setItemInMainHand(null);
            } else {
                inventory.setItemInOffHand(null);
            }
        }

        MedkitSession session = new MedkitSession(player.getLevel(), player.getExp(), player.getTotalExperience());
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
                player.sendMessage(ChatColor.WHITE.toString() + ChatColor.BOLD + "Аптека отхилит вас через " + counter);

                if (counter <= 0) {
                    double healedMax = attribute != null ? attribute.getValue() : player.getMaxHealth();
                    player.setHealth(healedMax);
                    cancel();
                    Bukkit.getScheduler().runTask(TankPlugin.this, () -> cleanupMedkit(uuid, player));
                    return;
                }

                counter--;
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    private void handleWeaponFire(PlayerInteractEvent event, Player player, ItemStack item, WeaponType type) {
        EquipmentSlot slot = event.getHand();
        if (slot != EquipmentSlot.HAND && slot != EquipmentSlot.OFF_HAND) {
            return;
        }

        if (player.getCooldown(item.getType()) > 0) {
            return;
        }

        int ammo = getWeaponAmmo(item, type);
        if (ammo <= 0) {
            player.sendMessage(ChatColor.RED + "Боезапас исчерпан. Требуется пополнение патронов.");
            return;
        }

        event.setCancelled(true);
        Snowball projectile = player.launchProjectile(Snowball.class);
        projectile.setGravity(false);
        projectile.setInvulnerable(true);
        projectile.setVelocity(player.getLocation().getDirection().normalize().multiply(type.getSpeed()));
        projectileTypes.put(projectile.getUniqueId(), type);
        setWeaponAmmo(item, type, ammo - 1);
        updateWeaponStack(player, slot, item);
        player.setCooldown(item.getType(), type.getCooldownTicks());

        Bukkit.getScheduler().runTaskLater(this, () -> projectileTypes.remove(projectile.getUniqueId()), 200L);
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
                suggestions.add("rpd");
                suggestions.add("remington");
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

    private enum WeaponType {
        RPD(RPD_DAMAGE, RPD_SPEED, RPD_COOLDOWN_TICKS, 100),
        REMINGTON(REMINGTON_DAMAGE, REMINGTON_SPEED, REMINGTON_COOLDOWN_TICKS, 6);

        private final double damage;
        private final double speed;
        private final int cooldownTicks;
        private final int maxAmmo;

        WeaponType(double damage, double speed, int cooldownTicks, int maxAmmo) {
            this.damage = damage;
            this.speed = speed;
            this.cooldownTicks = cooldownTicks;
            this.maxAmmo = maxAmmo;
        }

        double getDamage() {
            return damage;
        }

        double getSpeed() {
            return speed;
        }

        int getCooldownTicks() {
            return cooldownTicks;
        }

        int getMaxAmmo() {
            return maxAmmo;
        }
    }

    private static class MedkitSession {
        private final int level;
        private final float exp;
        private final int totalExperience;

        MedkitSession(int level, float exp, int totalExperience) {
            this.level = level;
            this.exp = exp;
            this.totalExperience = totalExperience;
        }

        void applyProgress(Player player, int step) {
            if (step <= 0) {
                player.setLevel(0);
                player.setExp(0.0F);
                player.setTotalExperience(0);
                return;
            }
            player.setLevel(step);
            float progress = Math.min(1.0F, step / 10.0F);
            player.setExp(progress);
            player.setTotalExperience(step * 10);
        }

        void restore(Player player) {
            player.setTotalExperience(totalExperience);
            player.setLevel(level);
            player.setExp(exp);
        }
    }
}
