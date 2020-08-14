package com.technovision.extrachests.blocks;

import com.technovision.extrachests.ExtraChests;
import com.technovision.extrachests.blocks.blockentities.IronChestBlockEntity;
import it.unimi.dsi.fastutil.floats.Float2FloatFunction;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.tool.attribute.v1.FabricToolTags;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.block.ChestAnimationProgress;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.mob.PiglinBrain;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.stat.Stat;
import net.minecraft.stat.Stats;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.function.BiPredicate;
import java.util.function.Supplier;

public class IronChestBlock extends BlockWithEntity implements Waterloggable {

    public static final Identifier ID = new Identifier(ExtraChests.MOD_ID, "iron_chest");
    public static final DirectionProperty FACING;
    public static final EnumProperty<ChestType> CHEST_TYPE;
    public static final BooleanProperty WATERLOGGED;
    protected static final VoxelShape DOUBLE_NORTH_SHAPE;
    protected static final VoxelShape DOUBLE_SOUTH_SHAPE;
    protected static final VoxelShape DOUBLE_WEST_SHAPE;
    protected static final VoxelShape DOUBLE_EAST_SHAPE;
    protected static final VoxelShape SINGLE_SHAPE;

    private final Supplier<BlockEntityType<? extends IronChestBlockEntity>> entityTypeRetriever;

    static {
        FACING = HorizontalFacingBlock.FACING;
        CHEST_TYPE = Properties.CHEST_TYPE;
        WATERLOGGED = Properties.WATERLOGGED;
        DOUBLE_NORTH_SHAPE = Block.createCuboidShape(1.0D, 0.0D, 0.0D, 15.0D, 14.0D, 15.0D);
        DOUBLE_SOUTH_SHAPE = Block.createCuboidShape(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 16.0D);
        DOUBLE_WEST_SHAPE = Block.createCuboidShape(0.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D);
        DOUBLE_EAST_SHAPE = Block.createCuboidShape(1.0D, 0.0D, 1.0D, 16.0D, 14.0D, 15.0D);
        SINGLE_SHAPE = Block.createCuboidShape(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D);
    }

    public IronChestBlock(Supplier<BlockEntityType<? extends IronChestBlockEntity>> entityTypeRetriever) {
        super(FabricBlockSettings.of(Material.METAL)
                .hardness(3.5F)
                .resistance(4.0F)
                .sounds(BlockSoundGroup.METAL)
                .breakByTool(FabricToolTags.PICKAXES, 0)
                .requiresTool());
        this.entityTypeRetriever = entityTypeRetriever;
        this.stateManager.getDefaultState().with(FACING, Direction.NORTH).with(WATERLOGGED, Boolean.FALSE);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction direction = ctx.getPlayerFacing().getOpposite();
        FluidState fluidState = ctx.getWorld().getFluidState(ctx.getBlockPos());

        return this.getDefaultState().with(FACING, direction).with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER);
    }

    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        return ScreenHandler.calculateComparatorOutput((Inventory) world.getBlockEntity(pos));
    }

    @Override
    public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos, NavigationType type) {
        return false;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (state.get(CHEST_TYPE) == ChestType.SINGLE) {
            return SINGLE_SHAPE;
        } else {
            switch(getFacing(state)) {
                case NORTH:
                default:
                    return DOUBLE_NORTH_SHAPE;
                case SOUTH:
                    return DOUBLE_SOUTH_SHAPE;
                case WEST:
                    return DOUBLE_WEST_SHAPE;
                case EAST:
                    return DOUBLE_EAST_SHAPE;
            }
        }
    }

    public static Direction getFacing(BlockState state) {
        Direction direction = (Direction)state.get(FACING);
        return state.get(CHEST_TYPE) == ChestType.LEFT ? direction.rotateYClockwise() : direction.rotateYCounterclockwise();
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return (BlockState)state.with(FACING, rotation.rotate((Direction)state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation((Direction)state.get(FACING)));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, CHEST_TYPE, WATERLOGGED);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        } else {
            NamedScreenHandlerFactory namedScreenHandlerFactory = state.createScreenHandlerFactory(world, pos);
            if (namedScreenHandlerFactory != null) {
                player.openHandledScreen(namedScreenHandlerFactory);
                player.incrementStat(this.getOpenStat());
                PiglinBrain.onGuardedBlockBroken(player, true);
            }

            return ActionResult.CONSUME;
        }
    }

    protected Stat<Identifier> getOpenStat() {
        return Stats.CUSTOM.getOrCreateStat(Stats.OPEN_CHEST);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof Inventory) {
                ItemScatterer.spawn(world, pos, (Inventory)blockEntity);
                world.updateComparators(pos, this);
            }
            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }

    @Override
    public BlockEntity createBlockEntity(BlockView world) {
        return new IronChestBlockEntity();
    }

    public DoubleBlockProperties.PropertySource<? extends IronChestBlockEntity> getBlockEntitySource(BlockState state, World world, BlockPos pos, boolean ignoreBlocked) {
        BiPredicate biPredicate;
        if (ignoreBlocked) {
            biPredicate = (worldAccess, blockPos) -> {
                return false;
            };
        } else {
            biPredicate = (worldAccess, blockPos) -> {
                return false;
            };
        }
        return DoubleBlockProperties.toPropertySource((BlockEntityType)this.entityTypeRetriever.get(), IronChestBlock::getMergerType, IronChestBlock::getDirectionToAttached, FACING, state, world, pos, biPredicate);
    }

    public static DoubleBlockProperties.Type getMergerType(BlockState blockState) {
        return DoubleBlockProperties.Type.SINGLE;
    }

    public static Direction getDirectionToAttached(BlockState state) {
        Direction direction = state.get(FACING);
        return direction.rotateYCounterclockwise();
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        if (itemStack.hasCustomName()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof ChestBlockEntity) {
                ((ChestBlockEntity)blockEntity).setCustomName(itemStack.getName());
            }
        }
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return (Boolean)state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Environment(EnvType.CLIENT)
    public static DoubleBlockProperties.PropertyRetriever<IronChestBlockEntity, Float2FloatFunction> getAnimationProgressRetriever(final ChestAnimationProgress chestAnimationProgress) {
        return new DoubleBlockProperties.PropertyRetriever<IronChestBlockEntity, Float2FloatFunction>() {
            public Float2FloatFunction getFromBoth(IronChestBlockEntity chestBlockEntity, IronChestBlockEntity chestBlockEntity2) {
                return (f) -> {
                    return Math.max(chestBlockEntity.getAnimationProgress(f), chestBlockEntity2.getAnimationProgress(f));
                };
            }

            public Float2FloatFunction getFrom(IronChestBlockEntity chestBlockEntity) {
                return chestBlockEntity::getAnimationProgress;
            }

            public Float2FloatFunction getFallback() {
                ChestAnimationProgress var10000 = chestAnimationProgress;
                return var10000::getAnimationProgress;
            }
        };
    }
}