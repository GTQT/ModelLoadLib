package com.drppp.modelloader.loaders;


import com.drppp.modelloader.api.IModelLoader;
import com.drppp.modelloader.api.ModelLoadException;
import com.drppp.modelloader.api.model.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;

import java.io.DataInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Blitz3D (.b3d) 模型加载器
 * 
 * B3D 是二进制分块格式，结构为嵌套的 chunk:
 *   BB3D (文件头)
 *     ├── TEXS (纹理列表)
 *     ├── BRUS (画刷/材质列表)
 *     └── NODE (节点树)
 *           ├── MESH
 *           │     └── VRTS → TRIS
 *           ├── BONE (骨骼权重)
 *           ├── KEYS (关键帧)
 *           └── ANIM (动画信息)
 *           └── NODE (子节点, 递归)
 */
public class B3dModelLoader implements IModelLoader {

    @Override
    public String[] getSupportedExtensions() {
        return new String[] { "b3d" };
    }

    @Override
    public boolean canLoad(ResourceLocation resource) {
        return resource.getPath().endsWith(".b3d");
    }

    @Override
    public String getName() {
        return "B3D";
    }

    @Override
    public int getPriority() {
        return 50;
    }

    @Override
    public UnifiedModel load(ResourceLocation resource) throws ModelLoadException {
        try {
            IResource iResource = Minecraft.getMinecraft().getResourceManager().getResource(resource);
            InputStream stream = iResource.getInputStream();

            // 读取整个文件到内存（B3D文件通常不大）
            byte[] data = readAllBytes(stream);
            stream.close();

            ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

            UnifiedModel model = new UnifiedModel(resource.toString(), "b3d");
            Skeleton skeleton = new Skeleton();

            // 读取文件头 "BB3D"
            String header = readTag(buf);
            if (!"BB3D".equals(header)) {
                throw new ModelLoadException("Not a valid B3D file: " + resource + " (header=" + header + ")");
            }
            int fileSize = buf.getInt();
            int version = buf.getInt(); // 应该是 1

            // 解析上下文
            B3dContext ctx = new B3dContext();
            ctx.model = model;
            ctx.skeleton = skeleton;
            ctx.resource = resource;

            // 解析所有顶级 chunk
            int endPos = Math.min(buf.position() + fileSize - 4, buf.capacity());
            while (buf.position() < endPos) {
                parseChunk(buf, ctx, -1);
            }

            model.setSkeleton(skeleton);
            model.computeBoundingBox();
            return model;

        } catch (ModelLoadException e) {
            throw e;
        } catch (Exception e) {
            throw new ModelLoadException("Failed to load B3D: " + resource, e);
        }
    }

    // ========================= Chunk 解析 =========================

    private void parseChunk(ByteBuffer buf, B3dContext ctx, int parentBoneIdx) {
        if (buf.remaining() < 8) return;

        String tag = readTag(buf);
        int size = buf.getInt();
        int chunkEnd = buf.position() + size;

        switch (tag) {
            case "TEXS": parseTexs(buf, ctx, chunkEnd); break;
            case "BRUS": parseBrus(buf, ctx, chunkEnd); break;
            case "NODE": parseNode(buf, ctx, chunkEnd, parentBoneIdx); break;
            case "MESH": parseMesh(buf, ctx, chunkEnd); break;
            case "VRTS": parseVrts(buf, ctx, chunkEnd); break;
            case "TRIS": parseTris(buf, ctx, chunkEnd); break;
            case "BONE": parseBone(buf, ctx, chunkEnd); break;
            case "KEYS": parseKeys(buf, ctx, chunkEnd); break;
            case "ANIM": parseAnim(buf, ctx, chunkEnd); break;
            default:
                // 跳过未知 chunk
                break;
        }

        // 确保读到 chunk 末尾
        buf.position(Math.min(chunkEnd, buf.capacity()));
    }

    /** TEXS - 纹理列表 */
    private void parseTexs(ByteBuffer buf, B3dContext ctx, int end) {
        while (buf.position() < end) {
            String name = readCString(buf);
            int flags = buf.getInt();
            int blend = buf.getInt();
            float posX = buf.getFloat();
            float posY = buf.getFloat();
            float scaleX = buf.getFloat();
            float scaleY = buf.getFloat();
            float rotation = buf.getFloat();

            ctx.textureNames.add(name);
        }
    }

    /** BRUS - 材质/画刷列表 */
    private void parseBrus(ByteBuffer buf, B3dContext ctx, int end) {
        int texCount = buf.getInt();
        while (buf.position() < end) {
            String name = readCString(buf);
            float r = buf.getFloat();
            float g = buf.getFloat();
            float b = buf.getFloat();
            float a = buf.getFloat();
            float shininess = buf.getFloat();
            int blend = buf.getInt();
            int fx = buf.getInt();

            Material mat = new Material(name.isEmpty() ? "brush_" + ctx.brushes.size() : name);
            mat.setDiffuseColor(r, g, b, a);
            if (a < 1f) mat.setTranslucent(true);

            // 读取纹理引用
            for (int i = 0; i < texCount; i++) {
                int texId = buf.getInt();
                if (i == 0 && texId >= 0 && texId < ctx.textureNames.size()) {
                    String texName = ctx.textureNames.get(texId);
                    String basePath = ctx.resource.getPath();
                    String dir = basePath.contains("/") ? basePath.substring(0, basePath.lastIndexOf('/') + 1) : "";
                    mat.setDiffuseTexture(new ResourceLocation(ctx.resource.getNamespace(), dir + texName));
                }
            }

            ctx.brushes.add(mat);
            ctx.model.addMaterial(mat);
        }
    }

    /** NODE - 节点（递归结构） */
    private void parseNode(ByteBuffer buf, B3dContext ctx, int end, int parentBoneIdx) {
        String nodeName = readCString(buf);
        float posX = buf.getFloat(), posY = buf.getFloat(), posZ = buf.getFloat();
        float scaleX = buf.getFloat(), scaleY = buf.getFloat(), scaleZ = buf.getFloat();
        // 四元数旋转
        float rotW = buf.getFloat(), rotX = buf.getFloat(), rotY = buf.getFloat(), rotZ = buf.getFloat();

        // 注册为骨骼
        Bone bone = new Bone(nodeName.isEmpty() ? "node_" + ctx.skeleton.getBoneCount() : nodeName, parentBoneIdx);
        bone.setBindPoseLocal(new Transform()
                .setTranslation(posX, posY, posZ)
                .setScale(scaleX, scaleY, scaleZ));
        // 四元数转欧拉角（简化处理）
        float[] euler = quaternionToEuler(rotW, rotX, rotY, rotZ);
        bone.getBindPoseLocal().setRotation(euler[0], euler[1], euler[2]);

        int boneIdx = ctx.skeleton.addBone(bone);

        // 创建对应的 MeshGroup
        MeshGroup group = new MeshGroup(bone.getName());
        group.setLocalTransform(bone.getBindPoseLocal());
        if (parentBoneIdx >= 0) {
            group.setParentName(ctx.skeleton.getBone(parentBoneIdx).getName());
        }

        ctx.currentGroup = group;
        ctx.currentBoneIndex = boneIdx;

        // 解析子 chunk
        while (buf.position() < end) {
            parseChunk(buf, ctx, boneIdx);
        }

        if (!group.getMeshes().isEmpty()) {
            ctx.model.addMeshGroup(group);
        }
    }

    /** MESH - 网格 */
    private void parseMesh(ByteBuffer buf, B3dContext ctx, int end) {
        int brushId = buf.getInt();
        ctx.currentBrushId = brushId;
        ctx.currentVertices.clear();

        while (buf.position() < end) {
            parseChunk(buf, ctx, ctx.currentBoneIndex);
        }
    }

    /** VRTS - 顶点数据 */
    private void parseVrts(ByteBuffer buf, B3dContext ctx, int end) {
        int flags = buf.getInt();
        int texCoordSets = buf.getInt();
        int texCoordSize = buf.getInt();

        boolean hasNormal = (flags & 1) != 0;
        boolean hasColor = (flags & 2) != 0;

        ctx.currentVertices.clear();

        while (buf.position() < end) {
            Vertex v = new Vertex();

            float x = buf.getFloat(), y = buf.getFloat(), z = buf.getFloat();
            v.setPosition(x, y, z);

            if (hasNormal) {
                v.setNormal(buf.getFloat(), buf.getFloat(), buf.getFloat());
            }

            if (hasColor) {
                v.setColor(buf.getFloat(), buf.getFloat(), buf.getFloat(), buf.getFloat());
            }

            // UV 坐标
            for (int t = 0; t < texCoordSets; t++) {
                float u = 0, vv = 0;
                if (texCoordSize >= 1) u = buf.getFloat();
                if (texCoordSize >= 2) vv = buf.getFloat();
                if (texCoordSize >= 3) buf.getFloat(); // w, 忽略
                if (t == 0) {
                    v.setUV(u, vv);
                }
            }

            ctx.currentVertices.add(v);
        }
    }

    /** TRIS - 三角面索引 */
    private void parseTris(ByteBuffer buf, B3dContext ctx, int end) {
        int brushId = buf.getInt();

        Mesh mesh = new Mesh();
        String matName = null;
        if (brushId >= 0 && brushId < ctx.brushes.size()) {
            matName = ctx.brushes.get(brushId).getName();
        } else if (ctx.currentBrushId >= 0 && ctx.currentBrushId < ctx.brushes.size()) {
            matName = ctx.brushes.get(ctx.currentBrushId).getName();
        }
        mesh.setMaterialName(matName);

        // 添加当前所有顶点
        for (Vertex v : ctx.currentVertices) {
            mesh.addVertex(v);
        }

        // 读取三角面索引
        while (buf.position() < end - 8) {
            int i0 = buf.getInt();
            int i1 = buf.getInt();
            int i2 = buf.getInt();
            if (i0 >= 0 && i1 >= 0 && i2 >= 0
                    && i0 < ctx.currentVertices.size()
                    && i1 < ctx.currentVertices.size()
                    && i2 < ctx.currentVertices.size()) {
                mesh.addFace(new Face(i0, i1, i2));
            }
        }

        if (mesh.getFaceCount() > 0 && ctx.currentGroup != null) {
            ctx.currentGroup.addMesh(mesh);
        }
    }

    /** BONE - 骨骼权重 */
    private void parseBone(ByteBuffer buf, B3dContext ctx, int end) {
        while (buf.position() < end) {
            int vertexId = buf.getInt();
            float weight = buf.getFloat();

            if (vertexId >= 0 && vertexId < ctx.currentVertices.size()) {
                Vertex v = ctx.currentVertices.get(vertexId);
                // 追加骨骼权重
                int[] oldIndices = v.getBoneIndices();
                float[] oldWeights = v.getBoneWeights();
                if (oldIndices == null) {
                    v.setBoneBinding(new int[]{ctx.currentBoneIndex}, new float[]{weight});
                } else if (oldIndices.length < 4) {
                    int[] newIdx = new int[oldIndices.length + 1];
                    float[] newW = new float[oldWeights.length + 1];
                    System.arraycopy(oldIndices, 0, newIdx, 0, oldIndices.length);
                    System.arraycopy(oldWeights, 0, newW, 0, oldWeights.length);
                    newIdx[oldIndices.length] = ctx.currentBoneIndex;
                    newW[oldWeights.length] = weight;
                    v.setBoneBinding(newIdx, newW);
                }
            }
        }
    }

    /** KEYS - 关键帧 */
    private void parseKeys(ByteBuffer buf, B3dContext ctx, int end) {
        int flags = buf.getInt();
        boolean hasPos = (flags & 1) != 0;
        boolean hasScale = (flags & 2) != 0;
        boolean hasRot = (flags & 4) != 0;

        String targetName = ctx.currentBoneIndex >= 0
                ? ctx.skeleton.getBone(ctx.currentBoneIndex).getName()
                : "unknown";

        Animation.AnimationChannel channel = new Animation.AnimationChannel(targetName);

        while (buf.position() < end) {
            int frame = buf.getInt();
            float time = frame / 60f; // B3D 帧率默认60

            if (hasPos) {
                float x = buf.getFloat(), y = buf.getFloat(), z = buf.getFloat();
                channel.addPositionKey(time, x, y, z);
            }
            if (hasScale) {
                float x = buf.getFloat(), y = buf.getFloat(), z = buf.getFloat();
                channel.addScaleKey(time, x, y, z);
            }
            if (hasRot) {
                float w = buf.getFloat(), x = buf.getFloat(), y = buf.getFloat(), z = buf.getFloat();
                float[] euler = quaternionToEuler(w, x, y, z);
                channel.addRotationKey(time, euler[0], euler[1], euler[2]);
            }
        }

        // 添加到当前动画
        if (ctx.currentAnimation != null) {
            ctx.currentAnimation.addChannel(channel);
        }
    }

    /** ANIM - 动画元数据 */
    private void parseAnim(ByteBuffer buf, B3dContext ctx, int end) {
        int flags = buf.getInt();
        int frames = buf.getInt();
        float fps = buf.getFloat();

        Animation anim = new Animation("default");
        anim.setDuration(frames / fps);
        anim.setLoop((flags & 1) != 0);

        ctx.currentAnimation = anim;
        ctx.model.addAnimation(anim);
    }

    // ========================= 工具方法 =========================

    private String readTag(ByteBuffer buf) {
        byte[] tag = new byte[4];
        buf.get(tag);
        return new String(tag);
    }

    private String readCString(ByteBuffer buf) {
        StringBuilder sb = new StringBuilder();
        while (buf.hasRemaining()) {
            byte b = buf.get();
            if (b == 0) break;
            sb.append((char) b);
        }
        return sb.toString();
    }

    private byte[] readAllBytes(InputStream in) throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] tmp = new byte[4096];
        int read;
        while ((read = in.read(tmp)) != -1) {
            baos.write(tmp, 0, read);
        }
        return baos.toByteArray();
    }

    /**
     * 四元数 (w, x, y, z) → 欧拉角 (度)
     */
    private float[] quaternionToEuler(float w, float x, float y, float z) {
        float sinr = 2f * (w * x + y * z);
        float cosr = 1f - 2f * (x * x + y * y);
        float rx = (float) Math.toDegrees(Math.atan2(sinr, cosr));

        float sinp = 2f * (w * y - z * x);
        float ry;
        if (Math.abs(sinp) >= 1f) {
            ry = (float) Math.toDegrees(Math.copySign(Math.PI / 2, sinp));
        } else {
            ry = (float) Math.toDegrees(Math.asin(sinp));
        }

        float siny = 2f * (w * z + x * y);
        float cosy = 1f - 2f * (y * y + z * z);
        float rz = (float) Math.toDegrees(Math.atan2(siny, cosy));

        return new float[] { rx, ry, rz };
    }

    // ========================= 解析上下文 =========================

    private static class B3dContext {
        UnifiedModel model;
        Skeleton skeleton;
        ResourceLocation resource;

        List<String> textureNames = new ArrayList<>();
        List<Material> brushes = new ArrayList<>();

        MeshGroup currentGroup;
        int currentBoneIndex = -1;
        int currentBrushId = -1;
        List<Vertex> currentVertices = new ArrayList<>();
        Animation currentAnimation;
    }
}
