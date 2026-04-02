package com.drppp.modelloader.example.item;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 示例：如何注册使用自定义模型的物品
 *
 * 关键步骤：
 *   1. 创建 Item，设置 TEISR
 *   2. 注册 Item 到 Registry
 *   3. 注册 Item 的模型（指向 builtin/entity JSON）
 *   4. 提供对应的 JSON 文件
 *
 * 所需的 JSON 模型文件 (assets/mymod/models/item/xxx.json):
 *
 *   对于 OBJ 剑:
 *   {
 *     "parent": "builtin/entity",
 *     "display": {
 *       "thirdperson_righthand": {
 *         "rotation": [0, 90, -35],
 *         "translation": [0, 4, 0.5],
 *         "scale": [0.85, 0.85, 0.85]
 *       },
 *       "firstperson_righthand": {
 *         "rotation": [0, -90, 25],
 *         "translation": [1.13, 3.2, 1.13],
 *         "scale": [0.68, 0.68, 0.68]
 *       },
 *       "gui": {
 *         "rotation": [30, 45, 0],
 *         "translation": [0, 3, 0],
 *         "scale": [0.625, 0.625, 0.625]
 *       },
 *       "ground": {
 *         "rotation": [0, 0, 0],
 *         "translation": [0, 2, 0],
 *         "scale": [0.25, 0.25, 0.25]
 *       },
 *       "fixed": {
 *         "rotation": [0, 0, 0],
 *         "translation": [0, 0, 0],
 *         "scale": [0.5, 0.5, 0.5]
 *       }
 *     }
 *   }
 */
@Mod.EventBusSubscriber(modid = "mymod")
public class ItemRegistration {

    // ===================== 定义物品 =====================

    /** OBJ 模型的剑 */
    public static final Item OBJ_SWORD = new Item()
            .setRegistryName("mymod", "obj_sword")
            .setTranslationKey("mymod.obj_sword")
            .setCreativeTab(CreativeTabs.COMBAT)
            .setMaxStackSize(1);

    /** GeckoLib 模型的魔法杖 */
    public static final Item GECKOLIB_STAFF = new Item()
            .setRegistryName("mymod", "magic_staff")
            .setTranslationKey("mymod.magic_staff")
            .setCreativeTab(CreativeTabs.COMBAT)
            .setMaxStackSize(1);

    /** B3D 模型的钻头 */
    public static final Item B3D_DRILL = new Item()
            .setRegistryName("mymod", "drill")
            .setTranslationKey("mymod.drill")
            .setCreativeTab(CreativeTabs.TOOLS)
            .setMaxStackSize(1);

    // ===================== 注册物品 =====================

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(
                OBJ_SWORD,
                GECKOLIB_STAFF,
                B3D_DRILL
        );
    }

    // ===================== 注册模型 + TEISR =====================

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void registerModels(ModelRegistryEvent event) {
        // 绑定 TEISR 渲染器
        OBJ_SWORD.setTileEntityItemStackRenderer(ObjItemTEISR.INSTANCE);
        GECKOLIB_STAFF.setTileEntityItemStackRenderer(GeckoLibItemTEISR.INSTANCE);
        B3D_DRILL.setTileEntityItemStackRenderer(B3dItemTEISR.INSTANCE);

        // 注册物品模型指向 builtin/entity
        // 对应的 JSON 文件必须存在且 parent 为 "builtin/entity"
        ModelLoader.setCustomModelResourceLocation(OBJ_SWORD, 0,
                new ModelResourceLocation("mymod:obj_sword", "inventory"));
        ModelLoader.setCustomModelResourceLocation(GECKOLIB_STAFF, 0,
                new ModelResourceLocation("mymod:magic_staff", "inventory"));
        ModelLoader.setCustomModelResourceLocation(B3D_DRILL, 0,
                new ModelResourceLocation("mymod:drill", "inventory"));
    }
}
