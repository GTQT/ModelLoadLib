package com.drppp.modelloader.example.entity;

import com.drppp.modelloader.anim.AnimationController;
import com.drppp.modelloader.api.ModelLoadException;
import com.drppp.modelloader.api.model.Animation;
import com.drppp.modelloader.api.model.Transform;
import com.drppp.modelloader.api.model.UnifiedModel;
import com.drppp.modelloader.core.ModelLoaderRegistry;
import com.drppp.modelloader.loaders.GeckoLibAnimationLoader;
import com.drppp.modelloader.render.EnhancedModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * 示例：GeckoLib 动画模型绑定到生物实体
 *
 * 适用场景：
 *   - 自定义怪物、NPC
 *   - 有行走、攻击、idle 等多种动画的生物
 *
 * 注册：
 *   RenderingRegistry.registerEntityRenderingHandler(
 *       EntityDragon.class,
 *       manager -> new GeckoLibEntityRenderer<>(manager));
 */
public class GeckoLibEntityRenderer<T extends EntityLiving> extends Render<T> {

    private static final ResourceLocation GEO =
            new ResourceLocation("mymod", "models/entity/dragon.geo.json");
    private static final ResourceLocation ANIM =
            new ResourceLocation("mymod", "animations/entity/dragon.animation.json");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("mymod", "textures/entity/dragon.png");

    private UnifiedModel model;
    private EnhancedModelRenderer renderer;
    private boolean initialized = false;
    private long lastRenderTime = 0;

    /** 动画名称常量 */
    private static final String ANIM_IDLE   = "animation.dragon.idle";
    private static final String ANIM_WALK   = "animation.dragon.walk";
    private static final String ANIM_ATTACK = "animation.dragon.attack";
    private static final String ANIM_DEATH  = "animation.dragon.death";

    public GeckoLibEntityRenderer(RenderManager renderManager) {
        super(renderManager);
    }

    private void init() {
        if (initialized) return;
        try {
            model = ModelLoaderRegistry.getInstance().load(GEO);

            // 加载动画
            Map<String, Animation> animations = GeckoLibAnimationLoader.loadAnimations(ANIM);
            for (Animation anim : animations.values()) {
                model.addAnimation(anim);
            }

            renderer = new EnhancedModelRenderer();
            renderer.setModel(model);
            renderer.setDefaultTexture(TEXTURE);
            renderer.setGlobalScale(1f / 16f);

            // 默认播放 idle
            renderer.getAnimController().play(ANIM_IDLE, true);

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

        // ============ 更新动画 ============
        long now = System.currentTimeMillis();
        float dt = Math.min((now - lastRenderTime) / 1000f, 0.1f);
        lastRenderTime = now;

        AnimationController ctrl = renderer.getAnimController();
        ctrl.update(dt);

        // ============ 动画状态机 ============
        updateAnimationState(entity, ctrl);

        // ============ 头部跟随 ============
        float headYaw = interpolateRotation(entity.prevRotationYawHead,
                entity.rotationYawHead, partialTicks) - entityYaw;
        float headPitch = entity.prevRotationPitch
                + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;

        // 限制头部旋转范围
        headYaw = Math.max(-45f, Math.min(45f, headYaw));
        headPitch = Math.max(-30f, Math.min(30f, headPitch));

        renderer.setTransformOverride("head",
                new Transform().setRotation(headPitch, headYaw, 0));

        // ============ 受伤闪红 ============
        if (entity.hurtTime > 0) {
            float hurt = (float) entity.hurtTime / entity.maxHurtTime;
            renderer.setColorOverlay(1f, 0.3f, 0.3f, hurt * 0.6f);
        } else {
            renderer.clearColorOverlay();
        }

        // ============ 渲染 ============
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);

        // 实体朝向
        GlStateManager.rotate(180f - entityYaw, 0, 1, 0);

        // 翻转模型（MC 实体默认是倒的）
        GlStateManager.scale(1f, -1f, -1f);
        // 或者根据你的模型朝向决定是否需要翻转
        // 如果模型是正确朝向的，去掉上面的 scale

        renderer.render(partialTicks);

        GlStateManager.popMatrix();

        // 渲染名牌
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    /**
     * 根据实体状态切换动画
     */
    private void updateAnimationState(T entity, AnimationController ctrl) {
        // 死亡动画
        if (entity.deathTime > 0) {
            if (!ctrl.isPlaying(ANIM_DEATH)) {
                ctrl.crossFade(ANIM_DEATH, 0.2f);
            }
            return;
        }

        // 攻击动画
        if (entity.swingProgress > 0) {
            if (!ctrl.isPlaying(ANIM_ATTACK)) {
                ctrl.crossFade(ANIM_ATTACK, 0.15f);
            }
            return;
        }

        // 移动 vs 静止
        double speed = entity.motionX * entity.motionX + entity.motionZ * entity.motionZ;
        boolean isMoving = speed > 0.001;

        if (isMoving) {
            if (!ctrl.isPlaying(ANIM_WALK)) {
                ctrl.crossFade(ANIM_WALK, 0.25f);
            }
            // 根据移动速度调整动画播放速度
            float moveSpeed = (float) Math.sqrt(speed) * 10f;
            ctrl.setGlobalSpeed(Math.max(0.5f, Math.min(2.0f, moveSpeed)));
        } else {
            if (!ctrl.isPlaying(ANIM_IDLE)) {
                ctrl.crossFade(ANIM_IDLE, 0.3f);
            }
            ctrl.setGlobalSpeed(1.0f);
        }
    }

    /**
     * 插值旋转角度
     */
    private float interpolateRotation(float prevRot, float rot, float partialTicks) {
        float delta = rot - prevRot;
        while (delta < -180f) delta += 360f;
        while (delta >= 180f) delta -= 360f;
        return prevRot + partialTicks * delta;
    }

    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(T entity) {
        return TEXTURE;
    }
}
