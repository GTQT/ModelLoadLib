package com.drppp.modelloader.example.entity;

import com.drppp.modelloader.anim.AnimationController;
import com.drppp.modelloader.api.ModelLoadException;
import com.drppp.modelloader.api.model.UnifiedModel;
import com.drppp.modelloader.core.ModelLoaderRegistry;
import com.drppp.modelloader.render.EnhancedModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

/**
 * 示例：B3D 动画模型绑定到实体
 *
 * B3D 的特点是模型和动画打包在同一个文件中，
 * 加载后 UnifiedModel 直接包含动画数据，无需额外加载。
 *
 * 注册：
 *   RenderingRegistry.registerEntityRenderingHandler(
 *       EntityRobot.class,
 *       manager -> new B3dEntityRenderer<>(manager));
 */
public class B3dEntityRenderer<T extends EntityLiving> extends Render<T> {

    private static final ResourceLocation MODEL =
            new ResourceLocation("mymod", "models/entity/robot.b3d");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("mymod", "textures/entity/robot.png");

    private UnifiedModel model;
    private EnhancedModelRenderer renderer;
    private boolean initialized = false;
    private long lastRenderTime = 0;

    public B3dEntityRenderer(RenderManager renderManager) {
        super(renderManager);
    }

    private void init() {
        if (initialized) return;
        try {
            model = ModelLoaderRegistry.getInstance().load(MODEL);

            renderer = new EnhancedModelRenderer();
            renderer.setModel(model);
            renderer.setDefaultTexture(TEXTURE);
            renderer.setGlobalScale(1f / 16f);

            // B3D 自带动画，直接播放
            if (model.hasAnimations()) {
                String defaultAnim = model.getAnimations().keySet().iterator().next();
                renderer.getAnimController().play(defaultAnim, true);
            }

            initialized = true;
            lastRenderTime = System.currentTimeMillis();
        } catch (ModelLoadException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void doRender(T entity, double x, double y, double z,
                         float entityYaw, float partialTicks) {
        if (!initialized) init();
        if (renderer == null) return;

        // 更新动画
        long now = System.currentTimeMillis();
        float dt = Math.min((now - lastRenderTime) / 1000f, 0.1f);
        lastRenderTime = now;

        AnimationController ctrl = renderer.getAnimController();

        // 根据实体运动状态调整动画速度
        double speed = entity.motionX * entity.motionX + entity.motionZ * entity.motionZ;
        boolean isMoving = speed > 0.001;
        ctrl.setGlobalSpeed(isMoving ? 1.0f : 0.3f); // 静止时慢速播放

        ctrl.update(dt);

        // 受伤效果
        if (entity.hurtTime > 0) {
            float hurt = (float) entity.hurtTime / entity.maxHurtTime;
            renderer.setColorOverlay(1f, 0f, 0f, hurt * 0.5f);
        } else {
            renderer.clearColorOverlay();
        }

        // 渲染
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(180f - entityYaw, 0, 1, 0);

        renderer.render(partialTicks);

        GlStateManager.popMatrix();

        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(T entity) {
        return TEXTURE;
    }
}
