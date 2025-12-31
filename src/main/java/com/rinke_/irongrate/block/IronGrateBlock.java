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


public class IronGrateBlock extends Block{

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
}