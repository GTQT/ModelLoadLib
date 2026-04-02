package com.drppp.modelloader.example.item;

import com.drppp.modelloader.Tags;
import com.drppp.modelloader.api.ModelLoadException;
import com.drppp.modelloader.api.model.UnifiedModel;
import com.drppp.modelloader.core.ModelLoaderRegistry;
import com.drppp.modelloader.render.EnhancedModelRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**
 * Botarium 物品形态的 3D 渲染器
 * 物品显示 idle 状态（已展开的静态模型）
 */
public class BotariumItemTEISR extends TileEntityItemStackRenderer {

    public static final BotariumItemTEISR INSTANCE = new BotariumItemTEISR();

    private static final ResourceLocation GEO_MODEL =
            new ResourceLocation(Tags.MOD_ID, "models/geo/botarium.geo.json");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Tags.MOD_ID, "textures/block/botarium.png");

    private EnhancedModelRenderer renderer;
    private boolean initialized = false;
    private boolean loadFailed = false;

    private void init() {
        if (initialized || loadFailed) return;
        try {
            UnifiedModel model = ModelLoaderRegistry.getInstance().load(GEO_MODEL);

            // 物品形态加载动画让 idle 状态下 Box 缩放为 0（隐藏包装箱）
            java.util.Map<String, com.drppp.modelloader.api.model.Animation> anims =
                    com.drppp.modelloader.loaders.GeckoLibAnimationLoader.loadAnimations(
                            new ResourceLocation(Tags.MOD_ID, "animations/botarium.animation.json"));
            for (com.drppp.modelloader.api.model.Animation a : anims.values()) {
                model.addAnimation(a);
            }

            renderer = new EnhancedModelRenderer();
            renderer.setModel(model);
            renderer.setDefaultTexture(TEXTURE);
            renderer.setGlobalScale(1f / 16f);

            // 播放 idle（Box 缩放为 0，只显示机器本体）
            renderer.getAnimController().play("Botarium.anim.idle", true);

            initialized = true;
        } catch (ModelLoadException e) {
            e.printStackTrace();
            loadFailed = true;
        }
    }

    @Override
    public void renderByItem(ItemStack stack) {
        init();
        if (!initialized || renderer == null) return;

        Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);

        GlStateManager.pushMatrix();
        GlStateManager.translate(0.5f, 0f, 0.5f);

        // 更新动画（idle 是静态的，但需要让 Box scale=0 生效）
        renderer.getAnimController().update(0.016f);
        renderer.render(0f);

        GlStateManager.popMatrix();
    }
}
