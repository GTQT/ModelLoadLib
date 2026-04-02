package com.drppp.modelloader.example.block;

import com.drppp.modelloader.Tags;
import com.drppp.modelloader.example.item.BotariumItemTEISR;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Botarium 方块注册
 *
 * 如果你想把 Botarium 加到已有的 BlockRegistration 中，
 * 把下面的内容合并过去即可。这里单独写方便理解。
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public class BotariumRegistration {



    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {

    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {

    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void registerModels(ModelRegistryEvent event) {

    }
}
