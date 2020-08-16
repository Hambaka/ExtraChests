package com.technovision.extrachests.items;

import com.technovision.extrachests.ExtraChests;
import com.technovision.extrachests.blocks.ExtraChestTypes;
import com.technovision.extrachests.blocks.GenericExtraChestBlock;
import com.technovision.extrachests.blocks.blockentities.GenericExtraChestBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class UpgradeItem extends Item {

    ExtraChestUpgradeType type;

    public UpgradeItem(ExtraChestUpgradeType type) {
        super(new Item.Settings().group(ExtraChests.TAB));
        this.type = type;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {

        World world = context.getWorld();
        if (world.isClient) { return ActionResult.PASS; }

        PlayerEntity entityPlayer = context.getPlayer();
        if (entityPlayer == null) { return ActionResult.PASS; }

        BlockPos blockPos = context.getBlockPos();
        if (this.type.canUpgrade(ExtraChestTypes.WOOD)) {
            if (!(world.getBlockState(blockPos).getBlock() instanceof ChestBlock)) {
                return ActionResult.PASS;
            }
        }
        else {
            if (world.getBlockState(blockPos).getBlock().getDefaultState() != ExtraChestTypes.get(this.type.source).getDefaultState()) {
                return ActionResult.PASS;
            }
        }

        BlockEntity blockEntity = world.getBlockEntity(blockPos);

        if (this.type.canUpgrade(ExtraChestTypes.WOOD)) {
            if (!(blockEntity instanceof ChestBlockEntity)) {
                return ActionResult.PASS;
            }
        }

        ItemStack itemStack = context.getStack();
        GenericExtraChestBlockEntity newChest;
        Text customName = null;
        DefaultedList<ItemStack> chestContents = DefaultedList.ofSize(27, ItemStack.EMPTY);
        Direction chestFacing = Direction.NORTH;
        BlockState chestState = world.getBlockState(blockPos);

        if (blockEntity != null) {
            if (blockEntity instanceof GenericExtraChestBlockEntity) {
                GenericExtraChestBlockEntity chest = (GenericExtraChestBlockEntity) blockEntity;

                if (GenericExtraChestBlockEntity.countViewers(world, blockPos.getX(), blockPos.getY(), blockPos.getZ()) > 0) {
                    return ActionResult.PASS;
                }
                if (!chest.canPlayerUse(entityPlayer)) {
                    return ActionResult.PASS;
                }

                chestContents = chest.getInvStackList();
                chestFacing = chestState.get(GenericExtraChestBlock.FACING);
                customName = chest.getCustomName();
                newChest = this.type.target.makeEntity();

                if (newChest == null) {
                    return ActionResult.PASS;
                }
            }
            else if (blockEntity instanceof ChestBlockEntity) {
                ChestBlockEntity chest = (ChestBlockEntity) blockEntity;

                if (GenericExtraChestBlockEntity.countViewers(world, blockPos.getX(), blockPos.getY(), blockPos.getZ()) > 0) {
                    return ActionResult.PASS;
                }
                if (!chest.canPlayerUse(entityPlayer)) {
                    return ActionResult.PASS;
                }

                chestFacing = chestState.get(ChestBlock.FACING);
                chestContents = DefaultedList.ofSize(chest.size(), ItemStack.EMPTY);

                for (int slot = 0; slot < chestContents.size(); slot++) {
                    chestContents.set(slot, chest.getStack(slot));
                }

                customName = chest.getCustomName();
                newChest = this.type.target.makeEntity();
            }

            blockEntity.toUpdatePacket();
            world.removeBlockEntity(blockPos);
            world.removeBlock(blockPos, false);

            BlockState blockState = ExtraChestTypes.get(type.target).getDefaultState().with(GenericExtraChestBlock.FACING, chestFacing).with(GenericExtraChestBlock.WATERLOGGED, false);
            world.setBlockState(blockPos, blockState, 3);
            world.updateListeners(blockPos, blockState, blockState, 3);

            itemStack.decrement(1);

            BlockEntity blockEntity2 = world.getBlockEntity(blockPos);

            if (blockEntity2 instanceof GenericExtraChestBlockEntity) {
                if (customName != null) {
                    ((GenericExtraChestBlockEntity) blockEntity2).setCustomName(customName);
                }
                ((GenericExtraChestBlockEntity) blockEntity2).setInvStackList(chestContents);
            }
        }
        return ActionResult.PASS;
    }
}
