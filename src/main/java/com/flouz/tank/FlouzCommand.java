package com.flouz.tank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class FlouzCommand implements CommandExecutor, TabCompleter {

    private final TankPlugin plugin;

    public FlouzCommand(TankPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Команду может использовать только игрок.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        String first = args[0].toLowerCase();
        switch (first) {
            case "com":
                return handleComCommand(player, args);
            case "tank":
                return handleTankCommand(player, args, 1);
            default:
                sendUsage(player);
                return true;
        }
    }

    private boolean handleComCommand(Player player, String[] args) {
        if (args.length < 2) {
            sendUsage(player);
            return true;
        }

        String second = args[1].toLowerCase();
        switch (second) {
            case "tank":
                return handleTankCommand(player, args, 2);
            case "luckydayz":
                return handleLuckyDayzCommand(player, args);
            default:
                sendUsage(player);
                return true;
        }
    }

    private boolean handleTankCommand(Player player, String[] args, int index) {
        if (args.length <= index) {
            sendUsage(player);
            return true;
        }

        String target = args[index].toLowerCase();
        switch (target) {
            case "helmet":
                giveItem(player, createArmor(Material.DIAMOND_HELMET, EquipmentEffectManager.HELMET_NAME));
                player.sendMessage(ChatColor.GREEN + "Вы получили " + EquipmentEffectManager.HELMET_NAME + ChatColor.GREEN + ".");
                return true;
            case "bodyarmor":
                giveItem(player, createArmor(Material.DIAMOND_CHESTPLATE, EquipmentEffectManager.CHESTPLATE_NAME));
                player.sendMessage(ChatColor.GREEN + "Вы получили " + EquipmentEffectManager.CHESTPLATE_NAME + ChatColor.GREEN + ".");
                return true;
            case "lenghtarmor":
                giveItem(player, createArmor(Material.DIAMOND_LEGGINGS, EquipmentEffectManager.LEGGINGS_NAME));
                player.sendMessage(ChatColor.GREEN + "Вы получили " + EquipmentEffectManager.LEGGINGS_NAME + ChatColor.GREEN + ".");
                return true;
            case "boottarmor":
                giveItem(player, createArmor(Material.DIAMOND_BOOTS, EquipmentEffectManager.BOOTS_NAME));
                player.sendMessage(ChatColor.GREEN + "Вы получили " + EquipmentEffectManager.BOOTS_NAME + ChatColor.GREEN + ".");
                return true;
            default:
                sendUsage(player);
                return true;
        }
    }

    private boolean handleLuckyDayzCommand(Player player, String[] args) {
        if (args.length < 3) {
            sendUsage(player);
            return true;
        }

        String third = args[2].toLowerCase();
        switch (third) {
            case "obla":
                giveItem(player, createWeapon());
                player.sendMessage(ChatColor.GREEN + "Вы получили " + EquipmentEffectManager.MACHETE_NAME + ChatColor.GREEN + ".");
                return true;
            case "apteka":
                giveItem(player, createMedkit());
                player.sendMessage(ChatColor.GREEN + "Вы получили аптечку.");
                return true;
            case "vert":
                plugin.getHelicopterManager().spawnHelicopter(player);
                return true;
            default:
                sendUsage(player);
                return true;
        }
    }

    private ItemStack createArmor(Material material, String displayName) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack createWeapon() {
        ItemStack stack = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(EquipmentEffectManager.MACHETE_NAME);
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack createMedkit() {
        ItemStack stack = new ItemStack(Material.GLOWSTONE_DUST);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MedkitManager.MEDKIT_NAME);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private void giveItem(Player player, ItemStack item) {
        player.getInventory().addItem(item).values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

    private void sendUsage(Player player) {
        player.sendMessage(ChatColor.RED + "Использование: /flouz1 com tank <helmet|bodyarmor|lenghtarmor|boottarmor>");
        player.sendMessage(ChatColor.RED + "Использование: /flouz1 com luckydayz <obla|apteka|vert>");
        player.sendMessage(ChatColor.RED + "Использование: /flouz1 tank <boottarmor>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return partial(Arrays.asList("com", "tank"), args[0]);
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("com")) {
                return partial(Arrays.asList("tank", "luckydayz"), args[1]);
            } else if (args[0].equalsIgnoreCase("tank")) {
                return partial(Collections.singletonList("boottarmor"), args[1]);
            }
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("com") && args[1].equalsIgnoreCase("tank")) {
                return partial(Arrays.asList("helmet", "bodyarmor", "lenghtarmor", "boottarmor"), args[2]);
            }
            if (args[0].equalsIgnoreCase("com") && args[1].equalsIgnoreCase("luckydayz")) {
                return partial(Arrays.asList("obla", "apteka", "vert"), args[2]);
            }
        }
        return Collections.emptyList();
    }

    private List<String> partial(List<String> options, String token) {
        String lower = token == null ? "" : token.toLowerCase();
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }
}
