package com.rinke_.irongrate.block;

import com.rinke_.irongrate.IronGrate;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.resources.Identifier;

public class ModBlocks {
    public static final Block IRON_GRATE = registerBlock("iron_grate",
            new IronGrateBlock(BlockBehaviour.Properties.of()
                            .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(IronGrate.MOD_ID, "iron_grate")))

                            .strength(3.0F, 6.0F)
                            .sound(SoundType.COPPER_GRATE)
                            .mapColor(MapColor.METAL)
                            .noOcclusion()
                            .requiresCorrectToolForDrops()
                            .isValidSpawn(Blocks::never)
                            .isRedstoneConductor(Blocks::never)
                            .isSuffocating(Blocks::never)
                            .isViewBlocking(Blocks::never)
            )
    );


    private static Block registerBlock(String name, Block block){
        registerBlockItem(name, block);
        return Registry.register(BuiltInRegistries.BLOCK, Identifier.fromNamespaceAndPath(IronGrate.MOD_ID, name), block);
    }

    private static void registerBlockItem(String name, Block block){
        Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(IronGrate.MOD_ID, name),
                new BlockItem(block, new net.minecraft.world.item.Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(IronGrate.MOD_ID, name))).useBlockDescriptionPrefix()));

    }

    public static void registerModBlocks(){
        IronGrate.LOGGER.info("blocks from " + IronGrate.MOD_ID + " is now loading...");

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(entries -> {
            entries.insertBefore(Items.IRON_CHAIN,ModBlocks.IRON_GRATE);
        });
    }
}
