package com.direwolf20.buildinggadgets.common.events;

import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class BreakEventHandler {
    @SubscribeEvent
    public static void GetDrops(BlockEvent.BreakEvent event) {
        //If you are holding an exchanger gadget and break a block, put it into your inventory
        //This allows us to use the BreakBlock event on our exchanger, to properly remove blocks from the world.
//        PlayerEntity player = event.getPlayer();
//        if (player == null)
//            return;
//
//        ItemStack heldItem = AbstractGadget.getGadget(player);
//        if (heldItem.isEmpty())
//            return;
//
//        List<ItemStack> drops = Block.getDrops(event.getState(), (ServerWorld) event.getWorld(), event.getPos(), event.getWorld().getTileEntity(event.getPos()));
//        drops.removeIf(item -> InventoryHelper.giveItem(item, player, event.getPlayer().world));
    }
}

