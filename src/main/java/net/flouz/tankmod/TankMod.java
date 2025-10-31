package net.flouz.tankmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.network.play.client.CPacketCloseWindow;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Client-side mod that automates quick inventory toggling on damage and
 * extinguishes the player when set on fire.
 */
@Mod(modid = TankMod.MODID, name = TankMod.NAME, version = TankMod.VERSION, clientSideOnly = true)
public class TankMod {
    public static final String MODID = "nodamageanimation";
    public static final String NAME = "NoDamageAnimation";
    public static final String VERSION = "1.0.0";

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new ClientEvents());
    }

    @SideOnly(Side.CLIENT)
    private static class ClientEvents {
        private boolean wasBurning;
        private boolean wasArmorLow;

        @SubscribeEvent
        public void onLivingHurt(LivingHurtEvent event) {
            if (!(event.getEntityLiving() instanceof EntityPlayerSP)) {
                return;
            }

            Minecraft minecraft = Minecraft.getMinecraft();
            EntityPlayerSP player = minecraft.player;
            if (player == null || player != event.getEntityLiving() || player.connection == null) {
                return;
            }

            minecraft.addScheduledTask(() -> {
                if (player.connection != null) {
                    player.connection.sendPacket(new CPacketEntityAction(player, CPacketEntityAction.Action.OPEN_INVENTORY));
                    player.connection.sendPacket(new CPacketCloseWindow(player.openContainer.windowId));
                }
            });
        }

        @SubscribeEvent
        public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
            if (!(event.getEntityLiving() instanceof EntityPlayerSP)) {
                return;
            }

            Minecraft minecraft = Minecraft.getMinecraft();
            EntityPlayerSP player = minecraft.player;
            if (player == null || player != event.getEntityLiving()) {
                return;
            }

            boolean burning = player.isBurning();
            if (burning && !wasBurning) {
                player.sendChatMessage("/ext");
            }
            wasBurning = burning;

            boolean armorLow = isArmorDurabilityLow(player);
            if (armorLow && !wasArmorLow) {
                player.sendChatMessage("/fix all");
            }
            wasArmorLow = armorLow;
        }

        private boolean isArmorDurabilityLow(EntityPlayerSP player) {
            for (ItemStack stack : player.inventory.armorInventory) {
                if (stack.isEmpty()) {
                    continue;
                }

                int maxDamage = stack.getMaxDamage();
                if (maxDamage <= 0) {
                    continue;
                }

                int remaining = maxDamage - stack.getItemDamage();
                if ((float) remaining / (float) maxDamage < 0.5F) {
                    return true;
                }
            }
            return false;
        }
    }
}
