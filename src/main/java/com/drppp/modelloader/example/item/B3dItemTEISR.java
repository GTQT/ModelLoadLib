package com.drppp.modelloader.example.item;

import com.drppp.modelloader.api.ModelLoadException;
import com.drppp.modelloader.api.model.UnifiedModel;
import com.drppp.modelloader.core.ModelLoaderRegistry;
import com.drppp.modelloader.render.EnhancedModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**
 * 示例：使用 B3D 模型渲染物品
 *
 * B3D 模型自带动画数据，无需额外加载动画文件。
 *
 * 注册方式同上:
 *   item.setTileEntityItemStackRenderer(() -> B3dItemTEISR.INSTANCE);
 */
public class B3dItemTEISR extends TileEntityItemStackRenderer {

    public static final B3dItemTEISR INSTANCE = new B3dItemTEISR();

    private static final ResourceLocation MODEL =
            new ResourceLocation("mymod", "models/item/drill.b3d");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("mymod", "textures/item/drill.png");

    private EnhancedModelRenderer renderer;
    private boolean initialized = false;
    private long lastRenderTime = 0;

    private void init() {
        if (initialized) return;
        try {
            UnifiedModel model = ModelLoaderRegistry.getInstance().load(MODEL);

            renderer = new EnhancedModelRenderer();
            renderer.setModel(model);
            renderer.setDefaultTexture(TEXTURE);
            renderer.setGlobalScale(1f / 16f);

            // B3D 自带动画，直接播放
            if (model.hasAnimations()) {
                String animName = model.getAnimations().keySet().iterator().next();
                renderer.getAnimController().play(animName, true);
            }

            initialized = true;
            lastRenderTime = System.currentTimeMillis();
        } catch (ModelLoadException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void renderByItem(ItemStack stack) {
        init();
        if (renderer == null) return;

        long now = System.currentTimeMillis();
        float dt = Math.min((now - lastRenderTime) / 1000f, 0.1f);
        lastRenderTime = now;

        // 仅在使用时播放动画（例如钻头旋转）
        boolean inUse = stack.hasTagCompound()
                && stack.getTagCompound().getBoolean("InUse");
        if (inUse) {
            renderer.getAnimController().setGlobalSpeed(1.0f);
        } else {
            renderer.getAnimController().setGlobalSpeed(0f); // 暂停
        }

        renderer.getAnimController().update(dt);

        GlStateManager.pushMatrix();
        GlStateManager.translate(0.5f, 0.0f, 0.5f);
        renderer.render(0f);
        GlStateManager.popMatrix();
    }
}
