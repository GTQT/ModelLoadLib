package com.drppp.modelloader.render;


import com.drppp.modelloader.api.model.*;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;

/**
 * VBO 缓存渲染器
 * 
 * 将 UnifiedModel 的静态网格上传到 GPU VBO 中，
 * 后续渲染时直接绘制 VBO，避免每帧重新提交顶点数据。
 * 
 * 适用于不带骨骼动画的静态模型（如 OBJ 装饰物等）。
 * 
 * 用法:
 *   VBOModelRenderer vboRenderer = new VBOModelRenderer();
 *   vboRenderer.upload(model);  // 一次性上传
 *   
 *   // 每帧：
 *   vboRenderer.render();
 *   
 *   // 不再使用时释放：
 *   vboRenderer.dispose();
 */
public class VBOModelRenderer {

    /** 每个 MeshGroup 的 VBO 数据 */
    private final Map<String, VBOData> vboMap = new LinkedHashMap<>();

    /** 是否已上传 */
    private boolean uploaded = false;

    /**
     * 将模型上传到 GPU
     */
    public void upload(UnifiedModel model) {
        dispose(); // 先释放旧的

        for (MeshGroup group : model.getAllMeshGroups()) {
            for (int i = 0; i < group.getMeshes().size(); i++) {
                Mesh mesh = group.getMeshes().get(i);
                if (mesh.getFaceCount() == 0) continue;

                String key = group.getName() + "#" + i;
                VBOData data = uploadMesh(mesh);
                data.materialName = mesh.getMaterialName();
                data.groupName = group.getName();
                vboMap.put(key, data);
            }
        }

        uploaded = true;
    }

    /**
     * 渲染所有 VBO
     */
    public void render() {
        if (!uploaded) return;

        for (VBOData data : vboMap.values()) {
            renderVBO(data);
        }
    }

    /**
     * 仅渲染指定组
     */
    public void renderGroup(String groupName) {
        if (!uploaded) return;

        for (VBOData data : vboMap.values()) {
            if (groupName.equals(data.groupName)) {
                renderVBO(data);
            }
        }
    }

    /**
     * 释放所有 VBO 资源
     */
    public void dispose() {
        for (VBOData data : vboMap.values()) {
            if (data.vboId != 0) GL15.glDeleteBuffers(data.vboId);
            if (data.iboId != 0) GL15.glDeleteBuffers(data.iboId);
        }
        vboMap.clear();
        uploaded = false;
    }

    public boolean isUploaded() { return uploaded; }

    // ========================= 内部实现 =========================

    private VBOData uploadMesh(Mesh mesh) {
        VBOData data = new VBOData();
        List<Vertex> vertices = mesh.getVertices();
        List<Face> faces = mesh.getFaces();

        // 顶点数据：position(3) + texCoord(2) + normal(3) = 8 floats per vertex
        int stride = 8;
        FloatBuffer vertexBuf = BufferUtils.createFloatBuffer(vertices.size() * stride);
        for (Vertex v : vertices) {
            vertexBuf.put(v.getX()).put(v.getY()).put(v.getZ());
            vertexBuf.put(v.getU()).put(v.getV());
            vertexBuf.put(v.getNx()).put(v.getNy()).put(v.getNz());
        }
        vertexBuf.flip();

        // 索引数据
        IntBuffer indexBuf = BufferUtils.createIntBuffer(faces.size() * 3);
        for (Face f : faces) {
            indexBuf.put(f.getV0()).put(f.getV1()).put(f.getV2());
        }
        indexBuf.flip();

        // 上传 VBO
        data.vboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, data.vboId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuf, GL15.GL_STATIC_DRAW);

        // 上传 IBO
        data.iboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, data.iboId);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBuf, GL15.GL_STATIC_DRAW);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

        data.indexCount = faces.size() * 3;
        data.stride = stride * 4; // bytes

        return data;
    }

    private void renderVBO(VBOData data) {
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, data.vboId);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, data.iboId);

        // 启用顶点属性
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);

        int stride = data.stride;
        GL11.glVertexPointer(3, GL11.GL_FLOAT, stride, 0);
        GL11.glTexCoordPointer(2, GL11.GL_FLOAT, stride, 12); // offset = 3 * 4
        GL11.glNormalPointer(GL11.GL_FLOAT, stride, 20);       // offset = 5 * 4

        GL11.glDrawElements(GL11.GL_TRIANGLES, data.indexCount, GL11.GL_UNSIGNED_INT, 0);

        GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    private static class VBOData {
        int vboId;
        int iboId;
        int indexCount;
        int stride;
        String materialName;
        String groupName;
    }
}
