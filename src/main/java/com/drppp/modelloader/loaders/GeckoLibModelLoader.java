package com.drppp.modelloader.loaders;


import com.drppp.modelloader.api.IModelLoader;
import com.drppp.modelloader.api.ModelLoadException;
import com.drppp.modelloader.api.model.*;
import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * GeckoLib / Bedrock Edition 模型加载器
 * 
 * 支持格式：
 *   - Bedrock Entity Geometry (.geo.json)
 *   - GeckoLib 动画 (.animation.json)
 * 
 * 模型结构：
 *   geometry.xxx
 *     └── bones[]
 *           ├── name, parent, pivot
 *           └── cubes[]
 *                 ├── origin, size, uv
 *                 └── rotation (可选)
 */
public class GeckoLibModelLoader implements IModelLoader {

    private static final Gson GSON = new GsonBuilder().create();

    @Override
    public String[] getSupportedExtensions() {
        return new String[] { "geo.json", "json" };
    }

    @Override
    public boolean canLoad(ResourceLocation resource) {
        String path = resource.getPath();
        return path.endsWith(".geo.json")
            || (path.endsWith(".json") && path.contains("geo"));
    }

    @Override
    public String getName() {
        return "GeckoLib";
    }

    @Override
    public int getPriority() {
        return 40;
    }

    @Override
    public UnifiedModel load(ResourceLocation resource) throws ModelLoadException {
        try {
            IResource iResource = Minecraft.getMinecraft().getResourceManager().getResource(resource);
            InputStreamReader reader = new InputStreamReader(iResource.getInputStream(), StandardCharsets.UTF_8);
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            reader.close();

            UnifiedModel model = new UnifiedModel(resource.toString(), "geckolib");
            Skeleton skeleton = new Skeleton();

            // 解析 format_version
            String formatVersion = root.has("format_version") ?
                    root.get("format_version").getAsString() : "1.12.0";
            model.putExtra("format_version", formatVersion);

            // 获取几何体数据
            JsonObject geometry = extractGeometry(root);
            if (geometry == null) {
                throw new ModelLoadException("No geometry found in: " + resource);
            }

            // 解析纹理尺寸
            int texWidth = 64, texHeight = 64;
            if (geometry.has("texturewidth")) texWidth = geometry.get("texturewidth").getAsInt();
            if (geometry.has("textureheight")) texHeight = geometry.get("textureheight").getAsInt();
            if (geometry.has("description")) {
                JsonObject desc = geometry.getAsJsonObject("description");
                if (desc.has("texture_width")) texWidth = desc.get("texture_width").getAsInt();
                if (desc.has("texture_height")) texHeight = desc.get("texture_height").getAsInt();
            }
            model.putExtra("texture_width", texWidth);
            model.putExtra("texture_height", texHeight);

            // 解析骨骼
            JsonArray bones = geometry.has("bones") ? geometry.getAsJsonArray("bones") : new JsonArray();
            // 先注册所有骨骼名称
            for (int i = 0; i < bones.size(); i++) {
                JsonObject boneJson = bones.get(i).getAsJsonObject();
                String boneName = boneJson.get("name").getAsString();
                String parentName = boneJson.has("parent") ? boneJson.get("parent").getAsString() : null;
                int parentIdx = parentName != null ? skeleton.getBoneIndex(parentName) : -1;

                Bone bone = new Bone(boneName, parentIdx);

                // 解析枢纽点
                if (boneJson.has("pivot")) {
                    JsonArray pivot = boneJson.getAsJsonArray("pivot");
                    bone.setPivot(pivot.get(0).getAsFloat(), pivot.get(1).getAsFloat(), pivot.get(2).getAsFloat());
                }

                // 解析绑定姿态旋转
                if (boneJson.has("rotation")) {
                    JsonArray rot = boneJson.getAsJsonArray("rotation");
                    bone.setBindPoseLocal(new Transform()
                            .setRotation(rot.get(0).getAsFloat(), rot.get(1).getAsFloat(), rot.get(2).getAsFloat()));
                }

                skeleton.addBone(bone);
            }
            model.setSkeleton(skeleton);

            // 解析每个骨骼的 cube → 生成 MeshGroup
            for (int i = 0; i < bones.size(); i++) {
                JsonObject boneJson = bones.get(i).getAsJsonObject();
                String boneName = boneJson.get("name").getAsString();

                MeshGroup group = new MeshGroup(boneName);

                // 设置枢纽点
                if (boneJson.has("pivot")) {
                    JsonArray pivot = boneJson.getAsJsonArray("pivot");
                    group.setPivot(pivot.get(0).getAsFloat(), pivot.get(1).getAsFloat(), pivot.get(2).getAsFloat());
                }

                // 设置旋转
                if (boneJson.has("rotation")) {
                    JsonArray rot = boneJson.getAsJsonArray("rotation");
                    group.setLocalTransform(new Transform()
                            .setRotation(rot.get(0).getAsFloat(), rot.get(1).getAsFloat(), rot.get(2).getAsFloat()));
                }

                // 设置父子关系
                if (boneJson.has("parent")) {
                    group.setParentName(boneJson.get("parent").getAsString());
                }

                // 解析 cubes
                if (boneJson.has("cubes")) {
                    JsonArray cubes = boneJson.getAsJsonArray("cubes");
                    for (int j = 0; j < cubes.size(); j++) {
                        JsonObject cubeJson = cubes.get(j).getAsJsonObject();
                        Mesh mesh = buildCubeMesh(cubeJson, texWidth, texHeight);
                        group.addMesh(mesh);
                    }
                }

                model.addMeshGroup(group);
            }

            model.computeBoundingBox();
            return model;

        } catch (ModelLoadException e) {
            throw e;
        } catch (Exception e) {
            throw new ModelLoadException("Failed to load GeckoLib model: " + resource, e);
        }
    }

    /**
     * 从根 JSON 中提取几何体对象（兼容多种格式版本）
     */
    private JsonObject extractGeometry(JsonObject root) {
        // 新版 format_version >= 1.12.0
        if (root.has("minecraft:geometry")) {
            JsonArray geoArray = root.getAsJsonArray("minecraft:geometry");
            if (geoArray.size() > 0) return geoArray.get(0).getAsJsonObject();
        }
        // 旧版 - 直接有 geometry.xxx 键
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            if (entry.getKey().startsWith("geometry.") && entry.getValue().isJsonObject()) {
                return entry.getValue().getAsJsonObject();
            }
        }
        // 兜底 - 直接使用 root（可能就是几何体数据）
        if (root.has("bones")) {
            return root;
        }
        return null;
    }

    /**
     * 将 Bedrock 的一个 cube 转换为 Mesh（6个面 × 2个三角形 = 12 三角面）
     */
    private Mesh buildCubeMesh(JsonObject cubeJson, int texWidth, int texHeight) {
        Mesh mesh = new Mesh();

        // origin 和 size
        JsonArray originArr = cubeJson.getAsJsonArray("origin");
        JsonArray sizeArr = cubeJson.getAsJsonArray("size");
        float ox = originArr.get(0).getAsFloat();
        float oy = originArr.get(1).getAsFloat();
        float oz = originArr.get(2).getAsFloat();
        float sx = sizeArr.get(0).getAsFloat();
        float sy = sizeArr.get(1).getAsFloat();
        float sz = sizeArr.get(2).getAsFloat();

        // UV（支持 box UV 和 per-face UV）
        float uvX = 0, uvY = 0;
        boolean perFaceUV = false;
        JsonObject perFaceUVData = null;

        if (cubeJson.has("uv")) {
            JsonElement uvElem = cubeJson.get("uv");
            if (uvElem.isJsonArray()) {
                JsonArray uvArr = uvElem.getAsJsonArray();
                uvX = uvArr.get(0).getAsFloat();
                uvY = uvArr.get(1).getAsFloat();
            } else if (uvElem.isJsonObject()) {
                perFaceUV = true;
                perFaceUVData = uvElem.getAsJsonObject();
            }
        }

        // inflate (膨胀量)
        float inflate = cubeJson.has("inflate") ? cubeJson.get("inflate").getAsFloat() : 0;
        float x0 = ox - inflate;
        float y0 = oy - inflate;
        float z0 = oz - inflate;
        float x1 = ox + sx + inflate;
        float y1 = oy + sy + inflate;
        float z1 = oz + sz + inflate;

        // 8个顶点
        // Bedrock坐标系：X右 Y上 Z前
        float[][] verts = {
                {x0, y0, z0}, {x1, y0, z0}, {x1, y1, z0}, {x0, y1, z0}, // back face (z-)
                {x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}, {x0, y1, z1}, // front face (z+)
        };

        if (perFaceUV && perFaceUVData != null) {
            buildPerFaceUVCube(mesh, verts, perFaceUVData, texWidth, texHeight);
        } else {
            buildBoxUVCube(mesh, verts, uvX, uvY, sx, sy, sz, texWidth, texHeight);
        }

        return mesh;
    }

    /**
     * 使用 Box UV 布局构建立方体面
     */
    private void buildBoxUVCube(Mesh mesh, float[][] v, float uvX, float uvY,
                                float sx, float sy, float sz, int texW, int texH) {
        // Bedrock Box UV 布局:
        //        [top]
        // [left] [front] [right] [back]
        //        [bottom]
        // 各面的位置 (以 UV 单位):
        // front:  (uvX + sz,       uvY + sz,       sx, sy)
        // back:   (uvX + sz*2+sx,  uvY + sz,       sx, sy)
        // left:   (uvX,            uvY + sz,       sz, sy)
        // right:  (uvX + sz + sx,  uvY + sz,       sz, sy)
        // top:    (uvX + sz,       uvY,            sx, sz)
        // bottom: (uvX + sz + sx,  uvY,            sx, sz)

        addFaceWithUV(mesh, v, new int[]{4, 5, 6, 7},  // front (+Z)
                uvX + sz, uvY + sz, sx, sy, texW, texH, 0, 0, 1);
        addFaceWithUV(mesh, v, new int[]{1, 0, 3, 2},  // back (-Z)
                uvX + sz * 2 + sx, uvY + sz, sx, sy, texW, texH, 0, 0, -1);
        addFaceWithUV(mesh, v, new int[]{0, 4, 7, 3},  // left (-X)
                uvX, uvY + sz, sz, sy, texW, texH, -1, 0, 0);
        addFaceWithUV(mesh, v, new int[]{5, 1, 2, 6},  // right (+X)
                uvX + sz + sx, uvY + sz, sz, sy, texW, texH, 1, 0, 0);
        addFaceWithUV(mesh, v, new int[]{4, 0, 1, 5},  // bottom (-Y) (注意: Bedrock 底面UV翻转)
                uvX + sz + sx, uvY, sx, sz, texW, texH, 0, -1, 0);
        addFaceWithUV(mesh, v, new int[]{7, 6, 2, 3},  // top (+Y) (注意: Bedrock 顶面UV翻转)
                uvX + sz, uvY, sx, sz, texW, texH, 0, 1, 0);
    }

    /**
     * 使用 Per-face UV 构建立方体面
     */
    private void buildPerFaceUVCube(Mesh mesh, float[][] v, JsonObject uvData,
                                    int texW, int texH) {
        String[] faceNames = {"north", "south", "west", "east", "down", "up"};
        int[][] faceVerts = {
                {1, 0, 3, 2},  // north (-Z, back)
                {4, 5, 6, 7},  // south (+Z, front)
                {0, 4, 7, 3},  // west (-X, left)
                {5, 1, 2, 6},  // east (+X, right)
                {4, 0, 1, 5},  // down (-Y)
                {7, 6, 2, 3},  // up (+Y)
        };
        float[][] faceNormals = {
                {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}, {0, -1, 0}, {0, 1, 0}
        };

        for (int i = 0; i < faceNames.length; i++) {
            if (uvData.has(faceNames[i])) {
                JsonObject faceUV = uvData.getAsJsonObject(faceNames[i]);
                JsonArray uvArr = faceUV.getAsJsonArray("uv");
                JsonArray uvSize = faceUV.getAsJsonArray("uv_size");
                float fu = uvArr.get(0).getAsFloat();
                float fv = uvArr.get(1).getAsFloat();
                float fw = uvSize.get(0).getAsFloat();
                float fh = uvSize.get(1).getAsFloat();
                addFaceWithUV(mesh, v, faceVerts[i], fu, fv, fw, fh, texW, texH,
                        faceNormals[i][0], faceNormals[i][1], faceNormals[i][2]);
            }
        }
    }

    /**
     * 添加一个四边面（拆为2个三角面）并设置UV和法线
     */
    private void addFaceWithUV(Mesh mesh, float[][] allVerts, int[] indices,
                               float uvX, float uvY, float uvW, float uvH,
                               int texW, int texH,
                               float nx, float ny, float nz) {
        float u0 = uvX / texW;
        float v0 = uvY / texH;
        float u1 = (uvX + uvW) / texW;
        float v1 = (uvY + uvH) / texH;

        float[][] uvs = { {u0, v0}, {u1, v0}, {u1, v1}, {u0, v1} };

        int baseIdx = mesh.getVertexCount();
        for (int i = 0; i < 4; i++) {
            float[] pos = allVerts[indices[i]];
            Vertex vertex = new Vertex()
                    .setPosition(pos[0], pos[1], pos[2])
                    .setNormal(nx, ny, nz)
                    .setUV(uvs[i][0], uvs[i][1]);
            mesh.addVertex(vertex);
        }

        mesh.addQuadAsTris(baseIdx, baseIdx + 1, baseIdx + 2, baseIdx + 3);
    }
}
