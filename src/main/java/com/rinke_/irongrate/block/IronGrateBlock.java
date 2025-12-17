package com.rinke_.irongrate.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;


public class IronGrateBlock extends Block {

    public IronGrateBlock(Settings settings) {
        super(settings);
    }

    @Override
    public boolean isSideInvisible(BlockState state, BlockState neighborState, Direction direction) {
        // Hide faces when touching another iron grate
        return neighborState.isOf(this);
    }
}