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

import java.util.*;

/**
 * 增强渲染器（修复版）
 *
 * 修复内容:
 *   1. 自动检测 alpha 并启用半透明混合渲染（不依赖 Material.translucent 标记）
 *   2. glass 等薄片 cube 自动双面渲染
 *   3. Bedrock 动画 position 正确使用像素坐标
 */
public class EnhancedModelRenderer {

    private UnifiedModel model;
    private AnimationController animController;

    private final Set<String> hiddenGroups = new HashSet<>();
    private final Map<String, Transform> transformOverrides = new HashMap<>();

    private float overlayR = 1f, overlayG = 1f, overlayB = 1f, overlayA = 0f;
    private float globalScale = 1f / 16f;
    private ResourceLocation defaultTexture;
    private boolean renderTranslucent = true;

    /** 需要强制半透明渲染的组名 */
    private final Set<String> translucentGroups = new HashSet<>();

    /** 需要强制双面渲染的组名 */
    private final Set<String> doubleSidedGroups = new HashSet<>();

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
        if (visible) hiddenGroups.remove(name); else hiddenGroups.add(name);
    }

    /**
     * 标记指定组为半透明（会在第二遍用 alpha 混合渲染）
     */
    public void setGroupTranslucent(String name, boolean translucent) {
        if (translucent) translucentGroups.add(name); else translucentGroups.remove(name);
    }

    /**
     * 标记指定组为双面渲染（薄片/玻璃/面片需要）
     */
    public void setGroupDoubleSided(String name, boolean doubleSided) {
        if (doubleSided) doubleSidedGroups.add(name); else doubleSidedGroups.remove(name);
    }

    public void setTransformOverride(String groupName, Transform transform) {
        transformOverrides.put(groupName, transform);
    }

    public void clearTransformOverride(String groupName) {
        transformOverrides.remove(groupName);
    }

    public void setColorOverlay(float r, float g, float b, float a) {
        this.overlayR = r; this.overlayG = g; this.overlayB = b; this.overlayA = a;
    }

    public void clearColorOverlay() {
        this.overlayR = 1f; this.overlayG = 1f; this.overlayB = 1f; this.overlayA = 0f;
    }

    // ========================= 渲染 =========================

    public void render(float partialTicks) {
        if (model == null) return;

        AnimationController.AnimationState animState =
                animController != null ? animController.getCurrentState() : null;

        GlStateManager.pushMatrix();
        GlStateManager.scale(globalScale, globalScale, globalScale);

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
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.depthMask(false);
            GlStateManager.enableAlpha();
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.003921569f);  // 1/255

            for (MeshGroup group : model.getAllMeshGroups()) {
                if (group.getParentName() == null) {
                    renderGroupRecursive(group, animState, true);
                }
            }

            GlStateManager.depthMask(true);
            GlStateManager.disableBlend();
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f);  // 恢复默认
        }

        if (overlayA > 0f) {
            GlStateManager.color(1f, 1f, 1f, 1f);
        }

        GlStateManager.popMatrix();
    }

    private void renderGroupRecursive(MeshGroup group,
                                      AnimationController.AnimationState animState,
                                      boolean translucentPass) {
        if (hiddenGroups.contains(group.getName()) || !group.isVisible()) return;

        // 判断这个组是否应该在当前 pass 渲染
        boolean groupIsTranslucent = translucentGroups.contains(group.getName());
        // 如果组没有被标记，看材质
        if (!groupIsTranslucent) {
            for (Mesh mesh : group.getMeshes()) {
                Material mat = mesh.getMaterialName() != null ? model.getMaterial(mesh.getMaterialName()) : null;
                if (mat != null && mat.isTranslucent()) {
                    groupIsTranslucent = true;
                    break;
                }
            }
        }

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
                    // ★ Bedrock 动画旋转方向与 OpenGL 相反，需要取反
                    GlStateManager.rotate(-bs.rz, 0, 0, 1);
                    GlStateManager.rotate(-bs.ry, 0, 1, 0);
                    GlStateManager.rotate(-bs.rx, 1, 0, 0);
                }
                if (bs.hasScale) GlStateManager.scale(bs.sx, bs.sy, bs.sz);
            }
        }

        // 局部变换（bone 的绑定姿态旋转，Bedrock 方向同样需要取反）
        Transform local = group.getLocalTransform();
        if (local != null && !local.isIdentity()) {
            GlStateManager.rotate(-local.getRz(), 0, 0, 1);
            GlStateManager.rotate(-local.getRy(), 0, 1, 0);
            GlStateManager.rotate(-local.getRx(), 1, 0, 0);
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

        if (local != null) {
            GlStateManager.translate(local.getTx(), local.getTy(), local.getTz());
        }

        // 渲染此组的网格
        // 非透明 pass 只渲染非透明组，透明 pass 只渲染透明组
        boolean shouldRenderMeshes;
        if (translucentPass) {
            shouldRenderMeshes = groupIsTranslucent;
        } else {
            shouldRenderMeshes = !groupIsTranslucent;
        }

        if (shouldRenderMeshes) {
            for (Mesh mesh : group.getMeshes()) {
                // 绑定纹理
                ResourceLocation tex = defaultTexture;
                Material mat = mesh.getMaterialName() != null ? model.getMaterial(mesh.getMaterialName()) : null;
                if (mat != null && mat.getDiffuseTexture() != null) {
                    tex = mat.getDiffuseTexture();
                }
                if (tex != null) {
                    Minecraft.getMinecraft().getTextureManager().bindTexture(tex);
                }

                // 发光材质
                boolean emissive = mat != null && mat.getEmissiveLevel() > 0;
                int prevLightX = 0, prevLightY = 0;
                if (emissive) {
                    prevLightX = (int) OpenGlHelper.lastBrightnessX;
                    prevLightY = (int) OpenGlHelper.lastBrightnessY;
                    int brightness = mat.getEmissiveLevel() * 16;
                    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, brightness, brightness);
                }

                // 双面渲染
                boolean doubleSided = doubleSidedGroups.contains(group.getName())
                        || (mat != null && mat.isDoubleSided());
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

                if (doubleSided) GlStateManager.enableCull();
                if (emissive) {
                    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, prevLightX, prevLightY);
                }
            }
        }

        // 递归子组（两个 pass 都要递归，否则子组会丢失）
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
