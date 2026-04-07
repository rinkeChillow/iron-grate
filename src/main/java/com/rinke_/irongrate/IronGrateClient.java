package com.rinke_.irongrate;
import com.rinke_.irongrate.block.ModBlocks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.RenderLayer;

public class IronGrateClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.putBlock(
                ModBlocks.IRON_GRATE,
                BlockRenderLayer.CUTOUT
        );

    }
}
