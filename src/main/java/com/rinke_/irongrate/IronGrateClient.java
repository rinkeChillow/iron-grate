package com.rinke_.irongrate;
import com.rinke_.irongrate.block.ModBlocks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.render.RenderLayer;

public class IronGrateClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(
                ModBlocks.IRON_GRATE,
                RenderLayer.getCutout()
        );

    }
}
