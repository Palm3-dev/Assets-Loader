package com.palm3.assets_loader;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class AssetsInGameTest {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, LoaderMain.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, LoaderMain.MOD_ID);

    public static final DeferredHolder<Block, Block> TEST_BLOCK = BLOCKS
            .register("test_block", () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)));

    public static final DeferredHolder<Item, Item> TEST_ITEM = ITEMS
            .register("test_item", () -> new Item(new Item.Properties()));
}
