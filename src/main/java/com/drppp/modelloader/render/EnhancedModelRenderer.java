package com.drppp.modelloader.render;


import com.drppp.modelloader.anim.AnimationController;
import com.drppp.modelloader.api.model.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.HashSet;
import java.util.Set;

/**
 * 增强版统一模型渲染器
 *
 * 相比基础版 UnifiedModelRenderer，增加了：
 *   - 与 AnimationController 深度集成
 *   - 发光材质（emissive）支持
 *   - 透明/半透明材质分层渲染
 *   - 颜色叠加（用于受伤闪红、药水效果等）
 *   - 模型部件的运行时变换覆盖
 * 
 * 用法：
 *   EnhancedModelRenderer renderer = new EnhancedModelRenderer();
 *   renderer.setModel(model);
 *   renderer.getAnimController().play("idle", true);
 *   
 *   // 每帧：
 *   renderer.getAnimController().update(deltaTime);
 *   renderer.render(partialTicks);
 */
public class EnhancedModelRenderer {

    private UnifiedModel model;
    private AnimationController animController;

    /** 隐藏的组 */
    private final Set<String> hiddenGroups = new HashSet<>();

    /** 运行时变换覆盖 */
    private final java.util.Map<String, Transform> transformOverrides = new java.util.HashMap<>();

    /** 颜色叠加 (RGBA) */
    private float overlayR = 1f, overlayG = 1f, overlayB = 1f, overlayA = 0f;

    /** 全局缩放因子 */
    private float globalScale = 1f / 16f;

    /** 默认纹理 */
    private ResourceLocation defaultTexture;

    /** 是否渲染透明部分 */
    private boolean renderTranslucent = true;

    // ========================= 设置 =========================

    public void setModel(UnifiedModel model) {
        this.model = model;
        this.animController = new AnimationController(model);
    }

    public UnifiedModel getModel() { return model; }
    public AnimationController getAnimController() { return animController; }

    public void setGlobalScale(float scale) { this.globalScale = scale; }
    public void setDefaultTexture(ResourceLocation tex) { this.defaultTexture = tex; }

    public void hideGroup(String name) { hiddenGroups.add(name); }
    public void showGroup(String name) { hiddenGroups.remove(name); }
    public void setGroupVisible(String name, boolean visible) {
        if (visible) hiddenGroups.remove(name);
        else hiddenGroups.add(name);
    }

    /**
     * 设置运行时变换覆盖（会在动画变换之后额外叠加）
     * 用途：头部跟随视角旋转、手臂持物姿势等
     */
    public void setTransformOverride(String groupName, Transform transform) {
        transformOverrides.put(groupName, transform);
    }

    public void clearTransformOverride(String groupName) {
        transformOverrides.remove(groupName);
    }

    /**
     * 设置颜色叠加（用于受伤闪红等）
     * @param r 红色分量
     * @param g 绿色分量
     * @param b 蓝色分量
     * @param a 叠加强度 (0=无叠加, 1=完全覆盖)
     */
    public void setColorOverlay(float r, float g, float b, float a) {
        this.overlayR = r; this.overlayG = g; this.overlayB = b; this.overlayA = a;
    }

    public void clearColorOverlay() {
        this.overlayR = 1f; this.overlayG = 1f; this.overlayB = 1f; this.overlayA = 0f;
    }

    // ========================= 渲染 =========================

    /**
     * 渲染模型（两遍：先不透明，再半透明）
     */
    public void render(float partialTicks) {
        if (model == null) return;

        // 获取动画状态
        AnimationController.AnimationState animState =
                animController != null ? animController.getCurrentState() : null;

        GlStateManager.pushMatrix();
        GlStateManager.scale(globalScale, globalScale, globalScale);

        // 应用颜色叠加
        if (overlayA > 0f) {
            float r = lerp(1f, overlayR, overlayA);
            float g = lerp(1f, overlayG, overlayA);
            float b = lerp(1f, overlayB, overlayA);
            GlStateManager.color(r, g, b, 1f);
        }

        // Pass 1: 不透明部分
        for (MeshGroup group : model.getAllMeshGroups()) {
            if (group.getParentName() == null) {
                renderGroupRecursive(group, animState, false);
            }
        }

        // Pass 2: 半透明部分
        if (renderTranslucent) {
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.depthMask(false);

            for (MeshGroup group : model.getAllMeshGroups()) {
                if (group.getParentName() == null) {
                    renderGroupRecursive(group, animState, true);
                }
            }

            GlStateManager.depthMask(true);
            GlStateManager.disableBlend();
        }

        // 恢复颜色
        if (overlayA > 0f) {
            GlStateManager.color(1f, 1f, 1f, 1f);
        }

        GlStateManager.popMatrix();
    }

    private void renderGroupRecursive(MeshGroup group,
                                      AnimationController.AnimationState animState,
                                      boolean translucentPass) {
        if (hiddenGroups.contains(group.getName()) || !group.isVisible()) return;

        GlStateManager.pushMatrix();

        // 枢纽点
        float px = group.getPivotX(), py = group.getPivotY(), pz = group.getPivotZ();
        GlStateManager.translate(px, py, pz);

        // 动画变换
        if (animState != null) {
            AnimationController.AnimationState.BoneState bs = animState.get(group.getName());
            if (bs != null) {
                if (bs.hasPosition) GlStateManager.translate(bs.tx, bs.ty, bs.tz);
                if (bs.hasRotation) {
                    GlStateManager.rotate(bs.rz, 0, 0, 1);
                    GlStateManager.rotate(bs.ry, 0, 1, 0);
                    GlStateManager.rotate(bs.rx, 1, 0, 0);
                }
                if (bs.hasScale) GlStateManager.scale(bs.sx, bs.sy, bs.sz);
            }
        }

        // 局部变换
        Transform local = group.getLocalTransform();
        if (local != null && !local.isIdentity()) {
            GlStateManager.rotate(local.getRz(), 0, 0, 1);
            GlStateManager.rotate(local.getRy(), 0, 1, 0);
            GlStateManager.rotate(local.getRx(), 1, 0, 0);
            GlStateManager.scale(local.getSx(), local.getSy(), local.getSz());
        }

        // 运行时变换覆盖
        Transform override = transformOverrides.get(group.getName());
        if (override != null) {
            if (override.getRx() != 0 || override.getRy() != 0 || override.getRz() != 0) {
                GlStateManager.rotate(override.getRz(), 0, 0, 1);
                GlStateManager.rotate(override.getRy(), 0, 1, 0);
                GlStateManager.rotate(override.getRx(), 1, 0, 0);
            }
        }

        GlStateManager.translate(-px, -py, -pz);

        // 平移部分
        if (local != null) {
            GlStateManager.translate(local.getTx(), local.getTy(), local.getTz());
        }

        // 渲染此组的网格
        for (Mesh mesh : group.getMeshes()) {
            Material mat = mesh.getMaterialName() != null ? model.getMaterial(mesh.getMaterialName()) : null;
            boolean isTranslucent = mat != null && mat.isTranslucent();

            // 根据当前 pass 决定是否渲染
            if (translucentPass != isTranslucent) continue;

            // 绑定纹理
            ResourceLocation tex = mat != null ? mat.getDiffuseTexture() : defaultTexture;
            if (tex != null) {
                Minecraft.getMinecraft().getTextureManager().bindTexture(tex);
            } else if (defaultTexture != null) {
                Minecraft.getMinecraft().getTextureManager().bindTexture(defaultTexture);
            }

            // 处理发光材质
            boolean emissive = mat != null && mat.getEmissiveLevel() > 0;
            int prevLightmapX = 0, prevLightmapY = 0;
            if (emissive) {
                prevLightmapX = (int) OpenGlHelper.lastBrightnessX;
                prevLightmapY = (int) OpenGlHelper.lastBrightnessY;
                int brightness = mat.getEmissiveLevel() * 16;
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, brightness, brightness);
            }

            // 双面渲染
            boolean doubleSided = mat != null && mat.isDoubleSided();
            if (doubleSided) GlStateManager.disableCull();

            // 绘制
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_TEX_NORMAL);

            for (Face face : mesh.getFaces()) {
                emitVertex(buffer, mesh.getVertices().get(face.getV0()));
                emitVertex(buffer, mesh.getVertices().get(face.getV1()));
                emitVertex(buffer, mesh.getVertices().get(face.getV2()));
            }

            tessellator.draw();

            // 恢复状态
            if (doubleSided) GlStateManager.enableCull();
            if (emissive) {
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, prevLightmapX, prevLightmapY);
            }
        }

        // 递归子组
        for (MeshGroup child : model.getAllMeshGroups()) {
            if (group.getName().equals(child.getParentName())) {
                renderGroupRecursive(child, animState, translucentPass);
            }
        }

        GlStateManager.popMatrix();
    }

    private void emitVertex(BufferBuilder buffer, Vertex v) {
        buffer.pos(v.getX(), v.getY(), v.getZ())
              .tex(v.getU(), v.getV())
              .normal(v.getNx(), v.getNy(), v.getNz())
              .endVertex();
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
