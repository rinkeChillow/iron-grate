package com.rinke_.irongrate.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;
import net.minecraft.block.Waterloggable;
//? if >=1.21.3 {
import net.minecraft.util.math.random.Random;
//?}
import net.minecraft.world.BlockView;
import net.minecraft.util.math.BlockPos;
//? if >=1.21.3 {
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
//?}
//? if <1.21.3 {
/*import net.minecraft.world.WorldAccess;*/
//?}

public class IronGrateBlock extends Block implements Waterloggable {

    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

    public IronGrateBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(WATERLOGGED, false));
    }
    // Hide faces when touching another iron grate
    @Override
    public boolean isSideInvisible(BlockState state, BlockState neighborState, Direction direction) {
        return neighborState.isOf(this);
    }
    // registers the WATERLOGGED state
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }
    // tells Minecraft there is water inside the block
    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED)
                ? Fluids.WATER.getStill(false)
                : super.getFluidState(state);
    }
    // changes the block state to waterlogged when water is actually placed
    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        BlockState state = this.getDefaultState();

        return state.with(
                WATERLOGGED,
                context.getWorld()
                        .getFluidState(context.getBlockPos())
                        .getFluid() == Fluids.WATER
        );
    }
    // water fluid ticking
    @Override
    //? if >=1.21.3 {
    public BlockState getStateForNeighborUpdate(
            BlockState state,
            WorldView world,
            ScheduledTickView tickView,
            BlockPos pos,
            Direction direction,
            BlockPos neighborPos,
            BlockState neighborState,
            Random random
    ) {
        if (state.get(WATERLOGGED)) {
            tickView.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }
        return super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }
    //?}
    //? if <1.21.3 {
    /*public BlockState getStateForNeighborUpdate(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            WorldAccess world,
            BlockPos pos,
            BlockPos neighborPos
    ) {
        if (state.get(WATERLOGGED)) {
            world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }*/
    //?}
    //turnig off ambiant shading (the darkening effect that only solid blocks do)
    @Override
    public boolean hasSidedTransparency(BlockState state) {
        return true;
    }
    @Override
    public float getAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos) {
        return 1.0F;
    }
}