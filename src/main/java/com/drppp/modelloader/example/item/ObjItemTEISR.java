package com.drppp.modelloader.example.item;

import com.drppp.modelloader.api.ModelLoadException;
import com.drppp.modelloader.api.model.Material;
import com.drppp.modelloader.api.model.UnifiedModel;
import com.drppp.modelloader.core.ModelLoaderRegistry;
import com.drppp.modelloader.render.VBOModelRenderer;
import com.drppp.modelloader.util.ModelUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**
 * OBJ 物品渲染 (修复版)
 */
public class ObjItemTEISR extends TileEntityItemStackRenderer {

    public static final ObjItemTEISR INSTANCE = new ObjItemTEISR();

    private static final ResourceLocation MODEL_LOC =
            new ResourceLocation("modelloader", "models/obj/tree.obj");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("modelloader", "textures/block/tree.png");

    private VBOModelRenderer vboRenderer;
    private boolean initialized = false;
    private boolean loadFailed = false;

    private void init() {
        if (initialized || loadFailed) return;
        try {
            UnifiedModel model = ModelLoaderRegistry.getInstance().load(MODEL_LOC);

            // 强制覆盖材质纹理
            for (Material mat : model.getMaterials().values()) {
                mat.setDiffuseTexture(TEXTURE);
            }

            vboRenderer = new VBOModelRenderer();
            vboRenderer.upload(model);
            initialized = true;
        } catch (ModelLoadException e) {
            e.printStackTrace();
            loadFailed = true;
        }
    }

    @Override
    public void renderByItem(ItemStack stack) {
        init();
        if (!initialized || vboRenderer == null) return;

        // ★ 手动绑定纹理
        Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);

        GlStateManager.pushMatrix();
        GlStateManager.translate(0.5f, 0.375f, 0.5f);
        vboRenderer.render();
        GlStateManager.popMatrix();
    }
}
