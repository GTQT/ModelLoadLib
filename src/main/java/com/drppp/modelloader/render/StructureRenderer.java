package com.drppp.modelloader.render;

import com.drppp.modelloader.loaders.StructureBlockLoader.BlockStructure;
import com.drppp.modelloader.loaders.StructureBlockLoader.BlockStructure.BlockEntry;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.opengl.GL11;

/**
 * 原版方块结构渲染器
 *
 * 将 StructureBlockLoader.capture() 捕获的方块快照渲染出来，
 * 并支持旋转、平移、缩放动画。
 *
 * 特点：
 *   - 逐方块设置光照（亮度正确）
 *   - 使用 BlockModelRenderer.renderModel（着色正确，草地/树叶有颜色）
 *   - 支持自定义模型叠加渲染
 *
 * 用法：
 *   BlockStructure structure = StructureBlockLoader.capture(world, pos1, pos2);
 *   StructureRenderer renderer = new StructureRenderer();
 *   renderer.setRotation(0, angle, 0);  // Y轴旋转动画
 *
 *   // 在 RenderWorldLastEvent 或 TESR 中：
 *   renderer.render(structure, viewX, viewY, viewZ, partialTicks);
 */
public class StructureRenderer {

    private float rotX, rotY, rotZ;
    private float transX, transY, transZ;
    private float scaleX = 1f, scaleY = 1f, scaleZ = 1f;
    private float alpha = 1f;
    private Runnable customModelRenderer;

    public void setRotation(float x, float y, float z) { this.rotX = x; this.rotY = y; this.rotZ = z; }
    public void setTranslation(float x, float y, float z) { this.transX = x; this.transY = y; this.transZ = z; }
    public void setScale(float x, float y, float z) { this.scaleX = x; this.scaleY = y; this.scaleZ = z; }
    public void setAlpha(float alpha) { this.alpha = alpha; }
    public float getRotX() { return rotX; }
    public float getRotY() { return rotY; }
    public float getRotZ() { return rotZ; }

    /**
     * 设置自定义模型渲染回调
     * 在方块渲染之后调用，坐标原点在结构中心，已施加动画变换
     */
    public void setCustomModelRenderer(Runnable callback) { this.customModelRenderer = callback; }

    /**
     * 渲染方块结构
     *
     * @param structure    StructureBlockLoader.capture() 返回的结构数据
     * @param viewX        结构中心相对相机的 X 偏移
     * @param viewY        Y 偏移
     * @param viewZ        Z 偏移
     * @param partialTicks 插值
     */
    public void render(BlockStructure structure, double viewX, double viewY, double viewZ,
                       float partialTicks) {
        if (structure == null || structure.getBlockCount() == 0) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) return;

        BlockRendererDispatcher blockRenderer = mc.getBlockRendererDispatcher();

        GlStateManager.pushMatrix();

        // 移到结构中心
        GlStateManager.translate(viewX, viewY, viewZ);

        // 动画变换
        GlStateManager.translate(transX, transY, transZ);
        GlStateManager.rotate(rotY, 0, 1, 0);
        GlStateManager.rotate(rotX, 1, 0, 0);
        GlStateManager.rotate(rotZ, 0, 0, 1);
        GlStateManager.scale(scaleX, scaleY, scaleZ);

        // 透明度
        if (alpha < 1f) {
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        }

        // 绑定方块纹理
        mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

        // 光照设置
        RenderHelper.disableStandardItemLighting();
        mc.entityRenderer.enableLightmap();

        // 逐方块渲染
        for (BlockEntry entry : structure.getBlocks()) {
            renderSingleBlock(mc, blockRenderer, entry);
        }

        // 恢复
        mc.entityRenderer.disableLightmap();
        RenderHelper.enableStandardItemLighting();

        if (alpha < 1f) {
            GlStateManager.disableBlend();
        }

        // 自定义模型叠加
        if (customModelRenderer != null) {
            customModelRenderer.run();
        }

        GlStateManager.popMatrix();
    }

    private void renderSingleBlock(Minecraft mc, BlockRendererDispatcher blockRenderer,
                                   BlockEntry entry) {
        BlockPos relativePos = entry.relativePos;
        BlockPos worldPos = entry.worldPos;
        IBlockState state = entry.state;

        try {
            // 设置光照
            int light = mc.world.getCombinedLight(worldPos, 0);
            int lightX = light & 0xFFFF;
            int lightY = light >> 16;
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);

            // 获取方块状态
            IBlockState actualState;
            try { actualState = state.getActualState(mc.world, worldPos); }
            catch (Exception e) { actualState = state; }

            IBlockState extendedState;
            try { extendedState = state.getBlock().getExtendedState(actualState, mc.world, worldPos); }
            catch (Exception e) { extendedState = actualState; }

            IBakedModel model = blockRenderer.getModelForState(actualState);

            // 渲染
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);

            buffer.setTranslation(
                    relativePos.getX() - worldPos.getX(),
                    relativePos.getY() - worldPos.getY(),
                    relativePos.getZ() - worldPos.getZ()
            );

            blockRenderer.getBlockModelRenderer().renderModel(
                    mc.world, model, extendedState, worldPos, buffer, false);

            buffer.setTranslation(0, 0, 0);
            tessellator.draw();

        } catch (Exception e) {
            try {
                Tessellator.getInstance().getBuffer().setTranslation(0, 0, 0);
                Tessellator.getInstance().getBuffer().finishDrawing();
            } catch (Exception ignored) {}
        }
    }
}
