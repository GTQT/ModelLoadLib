package com.drppp.modelloader.loaders;

import com.drppp.modelloader.api.IModelLoader;
import com.drppp.modelloader.api.ModelLoadException;
import com.drppp.modelloader.api.model.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * WaveFront OBJ 模型加载器 (修复版)
 *
 * 修复内容：
 *   1. 处理 CRLF 换行符（Windows / Blockbench 导出的文件）
 *   2. MTL 纹理路径智能解析：支持相对路径、自动搜索 textures/ 目录
 *   3. 四边面正确拆分为三角面
 *   4. 正确处理带空格的材质名和文件名
 */
public class ObjModelLoader implements IModelLoader {

    private static final Logger LOGGER = LogManager.getLogger("ObjModelLoader");

    @Override
    public String[] getSupportedExtensions() {
        return new String[] { "obj" };
    }

    @Override
    public boolean canLoad(ResourceLocation resource) {
        return resource.getPath().endsWith(".obj");
    }

    @Override
    public String getName() {
        return "OBJ";
    }

    @Override
    public int getPriority() {
        return 50;
    }

    @Override
    public UnifiedModel load(ResourceLocation resource) throws ModelLoadException {
        try {
            IResource iResource = Minecraft.getMinecraft().getResourceManager().getResource(resource);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(iResource.getInputStream(), StandardCharsets.UTF_8));

            UnifiedModel model = new UnifiedModel(resource.toString(), "obj");

            List<float[]> positions = new ArrayList<>();
            List<float[]> texCoords = new ArrayList<>();
            List<float[]> normals = new ArrayList<>();

            String currentGroupName = "default";
            String currentMaterial = null;
            int currentSmoothGroup = -1;

            MeshGroup currentGroup = new MeshGroup(currentGroupName);
            Mesh currentMesh = new Mesh();

            java.util.Map<String, Integer> vertexCache = new java.util.HashMap<>();

            String line;
            while ((line = reader.readLine()) != null) {
                // ★ 修复1: 去掉 \r (CRLF 换行符)
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("\\s+");
                String cmd = parts[0];

                switch (cmd) {
                    case "v":
                        positions.add(new float[] {
                                Float.parseFloat(parts[1]),
                                Float.parseFloat(parts[2]),
                                Float.parseFloat(parts[3])
                        });
                        break;

                    case "vt":
                        texCoords.add(new float[] {
                                Float.parseFloat(parts[1]),
                                parts.length > 2 ? Float.parseFloat(parts[2]) : 0f
                        });
                        break;

                    case "vn":
                        normals.add(new float[] {
                                Float.parseFloat(parts[1]),
                                Float.parseFloat(parts[2]),
                                Float.parseFloat(parts[3])
                        });
                        break;

                    case "g":
                    case "o":
                        finishMesh(currentMesh, currentGroup);
                        if (!currentGroup.getMeshes().isEmpty()) {
                            model.addMeshGroup(currentGroup);
                        }
                        currentGroupName = parts.length > 1 ? parts[1] : "group_" + model.getMeshGroupNames().size();
                        currentGroup = new MeshGroup(currentGroupName);
                        currentMesh = new Mesh();
                        currentMesh.setMaterialName(currentMaterial);
                        vertexCache.clear();
                        break;

                    case "usemtl":
                        finishMesh(currentMesh, currentGroup);
                        // ★ 修复2: 材质名可能包含空格，用原始行截取
                        currentMaterial = parts.length > 1 ? line.substring(line.indexOf(' ') + 1).trim() : null;
                        currentMesh = new Mesh();
                        currentMesh.setMaterialName(currentMaterial);
                        vertexCache.clear();
                        break;

                    case "s":
                        currentSmoothGroup = "off".equals(parts[1]) || "0".equals(parts[1])
                                ? -1 : Integer.parseInt(parts[1]);
                        break;

                    case "mtllib":
                        if (parts.length > 1) {
                            // ★ 修复3: mtl 文件名可能有空格
                            String mtlName = line.substring(line.indexOf(' ') + 1).trim();
                            parseMtl(resource, mtlName, model);
                        }
                        break;

                    case "f":
                        int[] faceIndices = new int[parts.length - 1];
                        for (int i = 1; i < parts.length; i++) {
                            faceIndices[i - 1] = resolveVertex(
                                    parts[i], positions, texCoords, normals,
                                    currentMesh, vertexCache);
                        }
                        if (faceIndices.length == 3) {
                            Face face = new Face(faceIndices[0], faceIndices[1], faceIndices[2]);
                            face.setMaterialName(currentMaterial);
                            face.setSmoothGroup(currentSmoothGroup);
                            currentMesh.addFace(face);
                        } else if (faceIndices.length == 4) {
                            currentMesh.addQuadAsTris(
                                    faceIndices[0], faceIndices[1], faceIndices[2], faceIndices[3]);
                        } else if (faceIndices.length > 4) {
                            for (int i = 1; i < faceIndices.length - 1; i++) {
                                currentMesh.addFace(new Face(faceIndices[0], faceIndices[i], faceIndices[i + 1]));
                            }
                        }
                        break;
                }
            }

            finishMesh(currentMesh, currentGroup);
            if (!currentGroup.getMeshes().isEmpty()) {
                model.addMeshGroup(currentGroup);
            }

            reader.close();
            model.computeBoundingBox();

            LOGGER.info("OBJ loaded: {} (groups={}, materials={})",
                    resource, model.getMeshGroupNames().size(), model.getMaterials().size());

            return model;

        } catch (Exception e) {
            throw new ModelLoadException("Failed to load OBJ: " + resource, e);
        }
    }

    private int resolveVertex(String vertexDef,
                              List<float[]> positions,
                              List<float[]> texCoords,
                              List<float[]> normals,
                              Mesh mesh,
                              java.util.Map<String, Integer> cache) {
        Integer cached = cache.get(vertexDef);
        if (cached != null) return cached;

        String[] components = vertexDef.split("/", -1);

        int posIdx = Integer.parseInt(components[0]);
        if (posIdx < 0) posIdx = positions.size() + posIdx;
        else posIdx -= 1;

        Vertex vertex = new Vertex();
        float[] pos = positions.get(posIdx);
        vertex.setPosition(pos[0], pos[1], pos[2]);

        if (components.length > 1 && !components[1].isEmpty()) {
            int texIdx = Integer.parseInt(components[1]);
            if (texIdx < 0) texIdx = texCoords.size() + texIdx;
            else texIdx -= 1;
            float[] uv = texCoords.get(texIdx);
            vertex.setUV(uv[0], 1f - uv[1]);
        }

        if (components.length > 2 && !components[2].isEmpty()) {
            int normIdx = Integer.parseInt(components[2]);
            if (normIdx < 0) normIdx = normals.size() + normIdx;
            else normIdx -= 1;
            float[] n = normals.get(normIdx);
            vertex.setNormal(n[0], n[1], n[2]);
        }

        int localIndex = mesh.getVertexCount();
        mesh.addVertex(vertex);
        cache.put(vertexDef, localIndex);
        return localIndex;
    }

    private void finishMesh(Mesh mesh, MeshGroup group) {
        if (mesh.getFaceCount() > 0) {
            group.addMesh(mesh);
        }
    }

    /**
     * ★ 修复后的 MTL 解析 ★
     *
     * 纹理路径解析策略（按优先级）：
     *   1. 如果 map_Kd 的值已经是完整的 ResourceLocation 路径 (包含 textures/)，直接使用
     *   2. 尝试在 OBJ 同目录下查找纹理文件
     *   3. 尝试在 textures/block/ 目录下查找（同名或去掉扩展名匹配）
     *   4. 尝试在 textures/ 目录下查找
     */
    private void parseMtl(ResourceLocation objResource, String mtlFileName, UnifiedModel model) {
        try {
            String objPath = objResource.getPath();
            String basePath = objPath.contains("/") ? objPath.substring(0, objPath.lastIndexOf('/') + 1) : "";
            ResourceLocation mtlResource = new ResourceLocation(objResource.getNamespace(), basePath + mtlFileName);

            IResource iResource = Minecraft.getMinecraft().getResourceManager().getResource(mtlResource);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(iResource.getInputStream(), StandardCharsets.UTF_8));

            Material currentMat = null;
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("\\s+");
                switch (parts[0]) {
                    case "newmtl":
                        if (currentMat != null) model.addMaterial(currentMat);
                        currentMat = new Material(line.substring(line.indexOf(' ') + 1).trim());
                        break;
                    case "Kd":
                        if (currentMat != null) {
                            currentMat.setDiffuseColor(
                                    Float.parseFloat(parts[1]),
                                    Float.parseFloat(parts[2]),
                                    Float.parseFloat(parts[3]),
                                    1f
                            );
                        }
                        break;
                    case "d":
                    case "Tr":
                        if (currentMat != null) {
                            float alpha = Float.parseFloat(parts[1]);
                            if ("Tr".equals(parts[0])) alpha = 1f - alpha;
                            currentMat.setDiffuseColor(
                                    currentMat.getDiffuseR(),
                                    currentMat.getDiffuseG(),
                                    currentMat.getDiffuseB(),
                                    alpha
                            );
                            if (alpha < 1f) currentMat.setTranslucent(true);
                        }
                        break;
                    case "map_Kd":
                        if (currentMat != null) {
                            String rawTexName = line.substring(line.indexOf(' ') + 1).trim();
                            ResourceLocation resolvedTex = resolveTexturePath(
                                    objResource.getNamespace(), basePath, rawTexName);
                            if (resolvedTex != null) {
                                currentMat.setDiffuseTexture(resolvedTex);
                                LOGGER.debug("MTL texture resolved: {} -> {}", rawTexName, resolvedTex);
                            } else {
                                LOGGER.warn("MTL texture NOT FOUND: {} (basePath={})", rawTexName, basePath);
                            }
                        }
                        break;
                    case "map_Bump":
                    case "bump":
                        if (currentMat != null) {
                            String rawNormName = line.substring(line.indexOf(' ') + 1).trim();
                            ResourceLocation resolvedNorm = resolveTexturePath(
                                    objResource.getNamespace(), basePath, rawNormName);
                            if (resolvedNorm != null) {
                                currentMat.setNormalTexture(resolvedNorm);
                            }
                        }
                        break;
                }
            }
            if (currentMat != null) model.addMaterial(currentMat);
            reader.close();
        } catch (Exception e) {
            LOGGER.warn("Failed to load MTL: {} for OBJ: {}", mtlFileName, objResource, e);
        }
    }

    /**
     * 智能纹理路径解析
     *
     * Blockbench/Blender 导出的 MTL 中 map_Kd 通常只写文件名 (如 "texture.png")，
     * 但 Minecraft 的资源体系要求纹理在 textures/ 目录下。
     * 这个方法会尝试多种路径直到找到实际存在的纹理文件。
     */
    private ResourceLocation resolveTexturePath(String namespace, String basePath, String rawTexName) {
        // 去掉可能的路径前缀（如 "./" "../"），只保留文件名
        String texFileName = rawTexName;
        if (texFileName.contains("/")) {
            texFileName = texFileName.substring(texFileName.lastIndexOf('/') + 1);
        }
        if (texFileName.contains("\\")) {
            texFileName = texFileName.substring(texFileName.lastIndexOf('\\') + 1);
        }

        // 获取不带扩展名的文件名
        String texNameNoExt = texFileName;
        if (texNameNoExt.contains(".")) {
            texNameNoExt = texNameNoExt.substring(0, texNameNoExt.lastIndexOf('.'));
        }

        // 尝试的路径列表（按优先级排列）
        String[] candidates = {
                // 1. 如果 rawTexName 已经包含 textures/，直接用
                rawTexName.startsWith("textures/") ? rawTexName : null,

                // 2. OBJ 同目录
                basePath + texFileName,

                // 3. textures/block/ 目录（最常见的 Minecraft 位置）
                "textures/block/" + texFileName,

                // 4. textures/ 根目录
                "textures/" + texFileName,

                // 5. textures/block/ + OBJ同名 + .png
                // (Blockbench 有时纹理叫 texture.png 但实际文件跟模型同名)
                "textures/block/" + getObjBaseName(basePath) + ".png",

                // 6. textures/item/
                "textures/item/" + texFileName,

                // 7. textures/entity/
                "textures/entity/" + texFileName,
        };

        for (String candidate : candidates) {
            if (candidate == null || candidate.isEmpty()) continue;
            try {
                ResourceLocation loc = new ResourceLocation(namespace, candidate);
                // 尝试获取资源，如果不抛异常就说明存在
                Minecraft.getMinecraft().getResourceManager().getResource(loc);
                LOGGER.info("Texture found at: {}", loc);
                return loc;
            } catch (Exception ignored) {
                // 路径不存在，继续尝试下一个
            }
        }

        // 所有路径都找不到，返回最可能的路径（让后续绑定时报错）
        LOGGER.error("Texture not found for any candidate path! rawTexName={}, basePath={}", rawTexName, basePath);
        return new ResourceLocation(namespace, "textures/block/" + texFileName);
    }

    /**
     * 从 basePath 推断模型名称
     * 例如 "models/obj/" -> 无法推断，返回空
     * 用于匹配 "textures/block/<模型名>.png" 这种约定
     */
    private String getObjBaseName(String basePath) {
        // basePath 类似 "models/obj/"
        // 无法直接推断模型名，返回空
        return "";
    }
}
