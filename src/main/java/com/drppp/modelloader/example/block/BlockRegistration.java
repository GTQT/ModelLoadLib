package com.drppp.modelloader.example.block;

import com.drppp.modelloader.Tags;
import com.drppp.modelloader.example.item.TreeItemTEISR;
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

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public class BlockRegistration {

    // ===================== 定义方块 =====================

    public static final Block OBJ_MACHINE =
            new ObjBlockExample.BlockCustomMachine();

    public static final Block GECKOLIB_CHEST =
            new GeckoLibBlockExample.BlockAnimatedChest();

    public static final Block B3D_WINDMILL =
            new B3dBlockExample.BlockWindmill();

    // ===================== 对应的 ItemBlock =====================

    public static final Item OBJ_MACHINE_ITEM =
            new ItemBlock(OBJ_MACHINE).setRegistryName(OBJ_MACHINE.getRegistryName());

    public static final Item GECKOLIB_CHEST_ITEM =
            new ItemBlock(GECKOLIB_CHEST).setRegistryName(GECKOLIB_CHEST.getRegistryName());

    public static final Item B3D_WINDMILL_ITEM =
            new ItemBlock(B3D_WINDMILL).setRegistryName(B3D_WINDMILL.getRegistryName());

    // ===================== 注册方块 =====================

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().registerAll(
                OBJ_MACHINE,
                GECKOLIB_CHEST,
                B3D_WINDMILL
        );

        GameRegistry.registerTileEntity(ObjBlockExample.TileCustomMachine.class,
                new ResourceLocation(Tags.MOD_ID, "custom_machine"));
        GameRegistry.registerTileEntity(GeckoLibBlockExample.TileAnimatedChest.class,
                new ResourceLocation(Tags.MOD_ID, "animated_chest"));
        GameRegistry.registerTileEntity(B3dBlockExample.TileWindmill.class,
                new ResourceLocation(Tags.MOD_ID, "windmill"));
    }

    // ===================== 注册物品形态 =====================

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(
                OBJ_MACHINE_ITEM,
                GECKOLIB_CHEST_ITEM,
                B3D_WINDMILL_ITEM
        );
    }

    // ===================== 客户端注册 =====================

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void registerModels(ModelRegistryEvent event) {
        // 绑定 TESR（方块放置后的渲染）
        ClientRegistry.bindTileEntitySpecialRenderer(
                ObjBlockExample.TileCustomMachine.class,
                new ObjBlockExample.TESRCustomMachine());

        ClientRegistry.bindTileEntitySpecialRenderer(
                GeckoLibBlockExample.TileAnimatedChest.class,
                new GeckoLibBlockExample.TESRAnimatedChest());

        ClientRegistry.bindTileEntitySpecialRenderer(
                B3dBlockExample.TileWindmill.class,
                new B3dBlockExample.TESRWindmill());

        // ★★★ 绑定 TEISR（物品在手中/GUI/掉落的渲染）★★★
        OBJ_MACHINE_ITEM.setTileEntityItemStackRenderer(TreeItemTEISR.INSTANCE);

        // 注册 ItemBlock 模型
        ModelLoader.setCustomModelResourceLocation(OBJ_MACHINE_ITEM, 0,
                new ModelResourceLocation(Tags.MOD_ID + ":tree", "inventory"));
        ModelLoader.setCustomModelResourceLocation(GECKOLIB_CHEST_ITEM, 0,
                new ModelResourceLocation(Tags.MOD_ID + ":animated_chest", "inventory"));
        ModelLoader.setCustomModelResourceLocation(B3D_WINDMILL_ITEM, 0,
                new ModelResourceLocation(Tags.MOD_ID + ":windmill", "inventory"));
    }
}
