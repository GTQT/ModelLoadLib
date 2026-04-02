package com.drppp.modelloader.example.item;

import com.drppp.modelloader.Tags;
import com.drppp.modelloader.api.ModelLoadException;
import com.drppp.modelloader.api.model.UnifiedModel;
import com.drppp.modelloader.core.ModelLoaderRegistry;
import com.drppp.modelloader.render.VBOModelRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**
 * tree 方块的物品形态 3D 渲染器
 *
 * 让物品在手持、GUI、掉落等所有场景都显示 OBJ 3D 模型
 * 配合 models/item/tree.json 中 "parent": "builtin/entity" 使用
 */
public class TreeItemTEISR extends TileEntityItemStackRenderer {

    public static final TreeItemTEISR INSTANCE = new TreeItemTEISR();

    private static final ResourceLocation MODEL =
            new ResourceLocation(Tags.MOD_ID, "models/obj/turbofan.obj");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Tags.MOD_ID, "textures/block/turbofan.png");

    private VBOModelRenderer vboRenderer;
    private boolean initialized = false;
    private boolean loadFailed = false;

    private void init() {
        if (initialized || loadFailed) return;
        try {
            UnifiedModel model = ModelLoaderRegistry.getInstance().load(MODEL);

            vboRenderer = new VBOModelRenderer();
            vboRenderer.upload(model);
            initialized = true;

            System.out.println("[TreeItemTEISR] Loaded successfully!");
        } catch (ModelLoadException e) {
            System.err.println("[TreeItemTEISR] FAILED: " + e.getMessage());
            e.printStackTrace();
            loadFailed = true;
        }
    }

    @Override
    public void renderByItem(ItemStack stack) {
        init();
        if (!initialized || vboRenderer == null) return;

        Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);

        GlStateManager.pushMatrix();

        // 将模型中心移到渲染原点
        // builtin/entity 的原点在 (0, 0, 0)，display 的 transform 会在外层处理
        GlStateManager.translate(0.5f, 0.375f, 0.5f);
        float scale = 0.7f;
        GlStateManager.scale(scale, scale, scale);
        // 不需要额外缩放，display 的 scale 已经在 JSON 中配置
        vboRenderer.render();

        GlStateManager.popMatrix();
    }
}
