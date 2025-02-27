package com.direwolf20.buildinggadgets.common.entities;

import com.direwolf20.buildinggadgets.common.util.ref.Reference;
import com.direwolf20.buildinggadgets.common.util.ref.Reference.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = Reference.MODID, bus = Bus.MOD)
public class OurEntities {
    private OurEntities() {}

    /**
     * Our only Entity is the one used to show the animation of a block
     * being placed or removed.
     */
    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityType<?>> event) {
        event.getRegistry().register(
                EntityType.Builder.<ConstructionBlockEntity>of(ConstructionBlockEntity::new, MobCategory.MISC)
                        .setTrackingRange(64)
                        .setUpdateInterval(1)
                        .setShouldReceiveVelocityUpdates(false)
                        .setCustomClientFactory(((spawnEntity, world) -> new ConstructionBlockEntity(ConstructionBlockEntity.TYPE, world)))
                        .build("")
                        .setRegistryName(EntityReference.CONSTRUCTION_BLOCK_ENTITY_RL)
        );
    }

    @SubscribeEvent
    public static void registerModels(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ConstructionBlockEntity.TYPE, ConstructionBlockEntityRender::new);
    }
}
