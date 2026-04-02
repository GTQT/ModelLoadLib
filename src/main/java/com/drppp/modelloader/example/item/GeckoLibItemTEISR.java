package com.drppp.modelloader.example.item;

import com.drppp.modelloader.anim.AnimationController;
import com.drppp.modelloader.api.ModelLoadException;
import com.drppp.modelloader.api.model.Animation;
import com.drppp.modelloader.api.model.UnifiedModel;
import com.drppp.modelloader.core.ModelLoaderRegistry;
import com.drppp.modelloader.loaders.GeckoLibAnimationLoader;
import com.drppp.modelloader.render.EnhancedModelRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.Map;

/**
 * 示例：使用 GeckoLib 模型渲染带动画的物品
 *
 * 适用场景：
 *   - 会动的武器（如脉动的魔法杖）
 *   - 有展开/收起动画的道具
 *   - GUI 中旋转展示的物品
 *
 * 注册方式同 ObjItemTEISR:
 *   item.setTileEntityItemStackRenderer(() -> GeckoLibItemTEISR.INSTANCE);
 *
 * 需要 builtin/entity JSON:
 *   assets/mymod/models/item/magic_staff.json:
 *   { "parent": "builtin/entity" }
 */
public class GeckoLibItemTEISR extends TileEntityItemStackRenderer {

    public static final GeckoLibItemTEISR INSTANCE = new GeckoLibItemTEISR();

    private static final ResourceLocation GEO_MODEL =
            new ResourceLocation("mymod", "models/item/magic_staff.geo.json");
    private static final ResourceLocation ANIM_FILE =
            new ResourceLocation("mymod", "animations/item/magic_staff.animation.json");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("mymod", "textures/item/magic_staff.png");

    private EnhancedModelRenderer renderer;
    private boolean initialized = false;
    private boolean loadFailed = false;

    /** 用于追踪动画时间 */
    private long lastRenderTime = 0;

    private void init() {
        if (initialized || loadFailed) return;
        try {
            UnifiedModel model = ModelLoaderRegistry.getInstance().load(GEO_MODEL);

            // 加载动画
            Map<String, Animation> animations = GeckoLibAnimationLoader.loadAnimations(ANIM_FILE);
            for (Animation anim : animations.values()) {
                model.addAnimation(anim);
            }

            renderer = new EnhancedModelRenderer();
            renderer.setModel(model);
            renderer.setDefaultTexture(TEXTURE);
            renderer.setGlobalScale(1f / 16f);

            // 播放 idle 动画（如魔法杖上的能量脉动）
            if (model.getAnimation("animation.magic_staff.idle") != null) {
                renderer.getAnimController().play("animation.magic_staff.idle", true);
            }

            initialized = true;
            lastRenderTime = System.currentTimeMillis();
        } catch (ModelLoadException e) {
            e.printStackTrace();
            loadFailed = true;
        }
    }

    @Override
    public void renderByItem(ItemStack stack) {
        init();
        if (!initialized || renderer == null) return;

        // 计算 deltaTime
        long now = System.currentTimeMillis();
        float deltaTime = (now - lastRenderTime) / 1000f;
        lastRenderTime = now;

        // 限制最大 delta，避免切换窗口后动画跳帧
        deltaTime = Math.min(deltaTime, 0.1f);

        // 更新动画
        renderer.getAnimController().update(deltaTime);

        // 根据 NBT 数据触发特殊动画（例如物品被"激活"时）
        if (stack.hasTagCompound() && stack.getTagCompound().getBoolean("Activated")) {
            AnimationController ctrl = renderer.getAnimController();
            if (!ctrl.isPlaying("animation.magic_staff.activate")) {
                ctrl.crossFade("animation.magic_staff.activate", 0.2f);
            }
        }

        GlStateManager.pushMatrix();

        // 物品渲染变换
        GlStateManager.translate(0.5f, 0.0f, 0.5f);

        // 渲染
        renderer.render(0f);

        GlStateManager.popMatrix();
    }
}
