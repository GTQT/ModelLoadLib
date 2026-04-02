package com.drppp.modelloader.render;


import com.drppp.modelloader.api.model.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * 统一模型渲染器
 * 
 * 能够渲染任何 UnifiedModel，不论其来源格式。
 * 针对 Forge 1.12.2 的 Tessellator/BufferBuilder 进行渲染。
 * 
 * 用法示例:
 *   UnifiedModelRenderer renderer = new UnifiedModelRenderer();
 *   renderer.render(model, partialTicks);
 * 
 * 高级用法（选择性渲染部件、动画）:
 *   renderer.setHiddenGroups(Sets.newHashSet("arm_left"));
 *   renderer.setAnimation(model.getAnimation("walk"), animationTime);
 *   renderer.render(model, partialTicks);
 */
public class UnifiedModelRenderer {

    /** 隐藏的组名称集合 */
    private final Set<String> hiddenGroups = new HashSet<>();

    /** 当前播放的动画 */
    private Animation currentAnimation;
    private float animationTime;

    /** 全局缩放 */
    private float globalScale = 1f / 16f; // 默认 1/16（1个MC单位 = 1像素）

    /** 是否启用法线 */
    private boolean enableNormals = true;

    /** 默认纹理（当材质没有指定纹理时使用） */
    private ResourceLocation defaultTexture;

    // ========================= 配置 =========================

    public void setHiddenGroups(Set<String> hidden) {
        hiddenGroups.clear();
        hiddenGroups.addAll(hidden);
    }

    public void hideGroup(String name) { hiddenGroups.add(name); }
    public void showGroup(String name) { hiddenGroups.remove(name); }

    public void setAnimation(Animation animation, float time) {
        this.currentAnimation = animation;
        this.animationTime = time;
    }

    public void clearAnimation() {
        this.currentAnimation = null;
    }

    public void setGlobalScale(float scale) { this.globalScale = scale; }
    public void setEnableNormals(boolean enable) { this.enableNormals = enable; }
    public void setDefaultTexture(ResourceLocation texture) { this.defaultTexture = texture; }

    // ========================= 渲染 =========================

    /**
     * 渲染整个模型
     */
    public void render(UnifiedModel model, float partialTicks) {
        GlStateManager.pushMatrix();
        GlStateManager.scale(globalScale, globalScale, globalScale);

        // 先渲染根组（没有父节点的组），递归渲染子组
        for (MeshGroup group : model.getAllMeshGroups()) {
            if (group.getParentName() == null) {
                renderGroupRecursive(model, group, partialTicks);
            }
        }

        GlStateManager.popMatrix();
    }

    /**
     * 仅渲染指定的组
     */
    public void renderGroup(UnifiedModel model, String groupName, float partialTicks) {
        MeshGroup group = model.getMeshGroup(groupName);
        if (group == null) return;

        GlStateManager.pushMatrix();
        GlStateManager.scale(globalScale, globalScale, globalScale);
        renderSingleGroup(model, group, partialTicks);
        GlStateManager.popMatrix();
    }

    /**
     * 递归渲染组及其子组
     */
    private void renderGroupRecursive(UnifiedModel model, MeshGroup group, float partialTicks) {
        if (hiddenGroups.contains(group.getName()) || !group.isVisible()) {
            return;
        }

        GlStateManager.pushMatrix();

        // 应用枢纽点变换
        float px = group.getPivotX();
        float py = group.getPivotY();
        float pz = group.getPivotZ();

        GlStateManager.translate(px, py, pz);

        // 应用动画变换
        if (currentAnimation != null) {
            applyAnimationTransform(group.getName());
        }

        // 应用局部变换
        Transform local = group.getLocalTransform();
        if (local != null && !local.isIdentity()) {
            GlStateManager.rotate(local.getRz(), 0, 0, 1);
            GlStateManager.rotate(local.getRy(), 0, 1, 0);
            GlStateManager.rotate(local.getRx(), 1, 0, 0);
            GlStateManager.scale(local.getSx(), local.getSy(), local.getSz());
        }

        GlStateManager.translate(-px, -py, -pz);

        // 应用平移
        if (local != null) {
            GlStateManager.translate(local.getTx(), local.getTy(), local.getTz());
        }

        // 渲染此组的网格
        renderMeshes(model, group);

        // 递归渲染子组
        for (String childName : group.getChildNames()) {
            MeshGroup child = model.getMeshGroup(childName);
            if (child != null) {
                renderGroupRecursive(model, child, partialTicks);
            }
        }

        // 如果没有显式的子组列表，查找所有以当前组为父的组
        if (group.getChildNames().isEmpty()) {
            for (MeshGroup candidate : model.getAllMeshGroups()) {
                if (group.getName().equals(candidate.getParentName())) {
                    renderGroupRecursive(model, candidate, partialTicks);
                }
            }
        }

        GlStateManager.popMatrix();
    }

    /**
     * 渲染单个组（不递归）
     */
    private void renderSingleGroup(UnifiedModel model, MeshGroup group, float partialTicks) {
        if (hiddenGroups.contains(group.getName()) || !group.isVisible()) return;

        GlStateManager.pushMatrix();

        float px = group.getPivotX();
        float py = group.getPivotY();
        float pz = group.getPivotZ();

        GlStateManager.translate(px, py, pz);

        if (currentAnimation != null) {
            applyAnimationTransform(group.getName());
        }

        Transform local = group.getLocalTransform();
        if (local != null && !local.isIdentity()) {
            GlStateManager.rotate(local.getRz(), 0, 0, 1);
            GlStateManager.rotate(local.getRy(), 0, 1, 0);
            GlStateManager.rotate(local.getRx(), 1, 0, 0);
        }

        GlStateManager.translate(-px, -py, -pz);

        renderMeshes(model, group);

        GlStateManager.popMatrix();
    }

    /**
     * 渲染一个 MeshGroup 内的所有 Mesh
     */
    private void renderMeshes(UnifiedModel model, MeshGroup group) {
        for (Mesh mesh : group.getMeshes()) {
            // 绑定材质纹理
            ResourceLocation texture = resolveTexture(model, mesh.getMaterialName());
            if (texture != null) {
                Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
            } else if (defaultTexture != null) {
                Minecraft.getMinecraft().getTextureManager().bindTexture(defaultTexture);
            }

            // 检查材质是否需要双面渲染
            Material mat = mesh.getMaterialName() != null ? model.getMaterial(mesh.getMaterialName()) : null;
            boolean doubleSided = mat != null && mat.isDoubleSided();
            if (doubleSided) {
                GlStateManager.disableCull();
            }

            // 使用 Tessellator 渲染
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();

            if (enableNormals) {
                buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_TEX_NORMAL);
            } else {
                buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_TEX);
            }

            for (Face face : mesh.getFaces()) {
                emitVertex(buffer, mesh.getVertices().get(face.getV0()));
                emitVertex(buffer, mesh.getVertices().get(face.getV1()));
                emitVertex(buffer, mesh.getVertices().get(face.getV2()));
            }

            tessellator.draw();

            if (doubleSided) {
                GlStateManager.enableCull();
            }
        }
    }

    /**
     * 发射一个顶点到 BufferBuilder
     */
    private void emitVertex(BufferBuilder buffer, Vertex v) {
        if (enableNormals) {
            buffer.pos(v.getX(), v.getY(), v.getZ())
                  .tex(v.getU(), v.getV())
                  .normal(v.getNx(), v.getNy(), v.getNz())
                  .endVertex();
        } else {
            buffer.pos(v.getX(), v.getY(), v.getZ())
                  .tex(v.getU(), v.getV())
                  .endVertex();
        }
    }

    /**
     * 应用动画变换到当前矩阵
     */
    private void applyAnimationTransform(String targetName) {
        if (currentAnimation == null) return;

        Animation.AnimationChannel channel = currentAnimation.getChannel(targetName);
        if (channel == null) return;

        float time = animationTime;
        if (currentAnimation.isLoop() && currentAnimation.getDuration() > 0) {
            time = time % currentAnimation.getDuration();
        }

        // 位移
        float[] pos = Animation.AnimationChannel.interpolateLinear(channel.getPositionKeys(), time);
        if (pos != null) {
            GlStateManager.translate(pos[0], pos[1], pos[2]);
        }

        // 旋转
        float[] rot = Animation.AnimationChannel.interpolateLinear(channel.getRotationKeys(), time);
        if (rot != null) {
            GlStateManager.rotate(rot[2], 0, 0, 1);
            GlStateManager.rotate(rot[1], 0, 1, 0);
            GlStateManager.rotate(rot[0], 1, 0, 0);
        }

        // 缩放
        float[] scale = Animation.AnimationChannel.interpolateLinear(channel.getScaleKeys(), time);
        if (scale != null) {
            GlStateManager.scale(scale[0], scale[1], scale[2]);
        }
    }

    /**
     * 根据材质名查找纹理资源
     */
    private ResourceLocation resolveTexture(UnifiedModel model, String materialName) {
        if (materialName == null) return null;
        Material mat = model.getMaterial(materialName);
        return mat != null ? mat.getDiffuseTexture() : null;
    }
}
