package com.direwolf20.buildinggadgets.common.blocks.templatemanager;

import com.direwolf20.buildinggadgets.client.gui.GuiMod;
import com.direwolf20.buildinggadgets.common.items.ITemplate;
import com.direwolf20.buildinggadgets.common.network.PacketHandler;
import com.direwolf20.buildinggadgets.common.network.packets.PacketBlockMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import PlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;

import javax.annotation.Nullable;

public class TemplateManager extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public TemplateManager(Properties builder) {
        super(builder);
        setDefaultState(getStateContainer().getBaseState().with(FACING, Direction.NORTH));
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return getDefaultState().with(FACING, context.getPlacementHorizontalFacing().getOpposite());
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new TemplateManagerTileEntity();
    }

    @Override
    public boolean onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult blockRayTraceResult) {
        // Only execute on the server
        if (worldIn.isRemote) {
            return true;
        }
        TileEntity te = worldIn.getTileEntity(pos);
        if (!(te instanceof TemplateManagerTileEntity)) {
            return false;
        }
        TemplateManagerContainer container = ((TemplateManagerTileEntity) te).getContainer((PlayerEntity) player);
        for (int i = 0; i <= 1; i++) {
            ItemStack itemStack = container.getSlot(i).getStack();
            if (!(itemStack.getItem() instanceof ITemplate)) continue;

            ITemplate template = (ITemplate) itemStack.getItem();
            String UUID = template.getUUID(itemStack);
            if (UUID == null) continue;

            CompoundNBT tagCompound = template.getWorldSave(worldIn).getCompoundFromUUID(UUID);
            if (tagCompound != null) {
                PacketHandler.sendTo(new PacketBlockMap(tagCompound), (ServerPlayerEntity) player);
            }
        }
        GuiMod.TEMPLATE_MANAGER.openContainer(player, worldIn, pos);
        return true;
    }

    @Override
    public void onReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            TileEntity tileEntity = world.getTileEntity(pos);
            if (tileEntity instanceof TemplateManagerTileEntity) {
                tileEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(iItemHandler -> {
                    for (int i = 0; i < iItemHandler.getSlots(); i++) {
                        ItemStack stack = iItemHandler.getStackInSlot(i);
                        if (!stack.isEmpty())
                            InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), stack);
                    }
                });
            }
        }
        super.onReplaced(state, world, pos, newState, isMoving);
    }
}
