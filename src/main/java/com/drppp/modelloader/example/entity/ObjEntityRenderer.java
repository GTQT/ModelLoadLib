package com.drppp.modelloader.example.entity;

import com.drppp.modelloader.api.ModelLoadException;
import com.drppp.modelloader.api.model.UnifiedModel;
import com.drppp.modelloader.core.ModelLoaderRegistry;
import com.drppp.modelloader.render.UnifiedModelRenderer;
import com.drppp.modelloader.util.ModelUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

/**
 * 示例：OBJ 模型绑定到实体
 *
 * 适用场景：
 *   - 无动画的简单实体（如投射物、载具、家具实体）
 *   - 只需要旋转但不需要骨骼动画的实体
 *
 * 注册：
 *   // 在 ClientProxy 的 preInit 或 init 中:
 *   RenderingRegistry.registerEntityRenderingHandler(
 *       EntityBullet.class,
 *       manager -> new ObjEntityRenderer(manager));
 */
public class ObjEntityRenderer<T extends Entity> extends Render<T> {

    private static final ResourceLocation MODEL =
            new ResourceLocation("mymod", "models/entity/bullet.obj");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("mymod", "textures/entity/bullet.png");

    private UnifiedModel model;
    private UnifiedModelRenderer renderer;
    private boolean initialized = false;

    public ObjEntityRenderer(RenderManager renderManager) {
        super(renderManager);
    }

    private void init() {
        if (initialized) return;
        try {
            model = ModelLoaderRegistry.getInstance().load(MODEL);
            ModelUtils.convertBlenderToMC(model);
            ModelUtils.computeFlatNormals(model);

            renderer = new UnifiedModelRenderer();
            renderer.setDefaultTexture(TEXTURE);
            renderer.setGlobalScale(1f / 16f);

            initialized = true;
        } catch (ModelLoadException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void doRender(T entity, double x, double y, double z,
                         float entityYaw, float partialTicks) {
        if (!initialized) init();
        if (renderer == null) return;

        GlStateManager.pushMatrix();

        // 定位
        GlStateManager.translate(x, y, z);

        // 朝向（投射物的运动方向）
        GlStateManager.rotate(180f - entityYaw, 0, 1, 0);
        GlStateManager.rotate(-entity.rotationPitch, 1, 0, 0);

        // 渲染
        renderer.render(model, partialTicks);

        GlStateManager.popMatrix();

        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(T entity) {
        return TEXTURE;
    }
}
