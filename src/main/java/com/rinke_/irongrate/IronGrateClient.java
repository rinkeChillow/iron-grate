package com.rinke_.irongrate;
import com.rinke_.irongrate.block.ModBlocks;
import net.fabricmc.api.ClientModInitializer;
//? if >=1.21.6 {
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.render.BlockRenderLayer;
//?}
//? if <1.21.6 {
/*import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.render.RenderLayer;
*///?}

public class IronGrateClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        //? if >=1.21.6 {
        BlockRenderLayerMap.putBlock(
                ModBlocks.IRON_GRATE,
                BlockRenderLayer.CUTOUT
        );
        //?}
        //? if <1.21.6 {
        /*BlockRenderLayerMap.INSTANCE.putBlock(
                ModBlocks.IRON_GRATE,
                RenderLayer.getCutout()
        );
        *///?}

    }
}
