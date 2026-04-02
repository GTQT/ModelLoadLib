package com.drppp.modelloader.util;



import com.drppp.modelloader.api.model.*;

import java.util.*;

/**
 * 模型工具类
 * 提供常用的模型处理操作
 */
public class ModelUtils {

    // ========================= 法线计算 =========================

    /**
     * 为整个模型重新计算面法线
     * 当模型文件缺少法线数据时使用
     */
    public static void computeFlatNormals(UnifiedModel model) {
        for (MeshGroup group : model.getAllMeshGroups()) {
            for (Mesh mesh : group.getMeshes()) {
                computeFlatNormals(mesh);
            }
        }
    }

    /**
     * 为单个 Mesh 计算面法线（平面着色）
     * 每个三角面的三个顶点共享同一法线
     */
    public static void computeFlatNormals(Mesh mesh) {
        List<Vertex> vertices = mesh.getVertices();
        for (Face face : mesh.getFaces()) {
            Vertex v0 = vertices.get(face.getV0());
            Vertex v1 = vertices.get(face.getV1());
            Vertex v2 = vertices.get(face.getV2());

            // 边向量
            float e1x = v1.getX() - v0.getX();
            float e1y = v1.getY() - v0.getY();
            float e1z = v1.getZ() - v0.getZ();
            float e2x = v2.getX() - v0.getX();
            float e2y = v2.getY() - v0.getY();
            float e2z = v2.getZ() - v0.getZ();

            // 叉积
            float nx = e1y * e2z - e1z * e2y;
            float ny = e1z * e2x - e1x * e2z;
            float nz = e1x * e2y - e1y * e2x;

            // 归一化
            float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len > 1e-6f) {
                nx /= len; ny /= len; nz /= len;
            }

            v0.setNormal(nx, ny, nz);
            v1.setNormal(nx, ny, nz);
            v2.setNormal(nx, ny, nz);
        }
    }

    /**
     * 计算平滑法线（顶点法线 = 共享该顶点的所有面法线的加权平均）
     * 适用于需要平滑着色的曲面模型
     */
    public static void computeSmoothNormals(Mesh mesh) {
        List<Vertex> vertices = mesh.getVertices();
        float[][] normals = new float[vertices.size()][3];

        for (Face face : mesh.getFaces()) {
            Vertex v0 = vertices.get(face.getV0());
            Vertex v1 = vertices.get(face.getV1());
            Vertex v2 = vertices.get(face.getV2());

            float e1x = v1.getX() - v0.getX();
            float e1y = v1.getY() - v0.getY();
            float e1z = v1.getZ() - v0.getZ();
            float e2x = v2.getX() - v0.getX();
            float e2y = v2.getY() - v0.getY();
            float e2z = v2.getZ() - v0.getZ();

            float nx = e1y * e2z - e1z * e2y;
            float ny = e1z * e2x - e1x * e2z;
            float nz = e1x * e2y - e1y * e2x;

            // 使用面积加权（叉积的模就是平行四边形面积）
            for (int idx : face.getIndices()) {
                normals[idx][0] += nx;
                normals[idx][1] += ny;
                normals[idx][2] += nz;
            }
        }

        // 归一化
        for (int i = 0; i < vertices.size(); i++) {
            float nx = normals[i][0], ny = normals[i][1], nz = normals[i][2];
            float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len > 1e-6f) {
                vertices.get(i).setNormal(nx / len, ny / len, nz / len);
            }
        }
    }

    // ========================= 坐标系转换 =========================

    /**
     * 将模型从 Bedrock 坐标系转换到 Java Edition / OpenGL 坐标系
     * 
     * Bedrock:     X右, Y上, Z前  (右手系，但模型原点和旋转方向不同)
     * Java/OpenGL: X右, Y上, Z后  (右手系)
     * 
     * 主要区别：Z轴取反，枢纽点偏移
     */
    public static void convertBedrockToJava(UnifiedModel model) {
        for (MeshGroup group : model.getAllMeshGroups()) {
            // 翻转枢纽点 Z
            group.setPivot(group.getPivotX(), group.getPivotY(), -group.getPivotZ());

            for (Mesh mesh : group.getMeshes()) {
                for (Vertex v : mesh.getVertices()) {
                    v.setPosition(v.getX(), v.getY(), -v.getZ());
                    v.setNormal(v.getNx(), v.getNy(), -v.getNz());
                }
            }
        }

        // 翻转骨骼枢纽点
        if (model.hasSkeleton()) {
            for (Bone bone : model.getSkeleton().getBones()) {
                bone.setPivot(bone.getPivotX(), bone.getPivotY(), -bone.getPivotZ());
            }
        }

        model.computeBoundingBox();
    }

    /**
     * 将 Blender 默认坐标系（Z-up）转换为 Minecraft 坐标系（Y-up）
     * Blender: X右, Y前, Z上
     * MC:      X右, Y上, Z后
     * 变换: (x, y, z) → (x, z, -y)
     */
    public static void convertBlenderToMC(UnifiedModel model) {
        for (MeshGroup group : model.getAllMeshGroups()) {
            float px = group.getPivotX(), py = group.getPivotY(), pz = group.getPivotZ();
            group.setPivot(px, pz, -py);

            for (Mesh mesh : group.getMeshes()) {
                for (Vertex v : mesh.getVertices()) {
                    float x = v.getX(), y = v.getY(), z = v.getZ();
                    v.setPosition(x, z, -y);
                    float nx = v.getNx(), ny = v.getNy(), nz = v.getNz();
                    v.setNormal(nx, nz, -ny);
                }
            }
        }

        model.computeBoundingBox();
    }

    // ========================= 模型变换 =========================

    /**
     * 对整个模型施加统一缩放
     */
    public static void scale(UnifiedModel model, float sx, float sy, float sz) {
        for (MeshGroup group : model.getAllMeshGroups()) {
            group.setPivot(group.getPivotX() * sx, group.getPivotY() * sy, group.getPivotZ() * sz);
            for (Mesh mesh : group.getMeshes()) {
                for (Vertex v : mesh.getVertices()) {
                    v.setPosition(v.getX() * sx, v.getY() * sy, v.getZ() * sz);
                }
            }
        }
        model.computeBoundingBox();
    }

    /**
     * 对整个模型施加平移
     */
    public static void translate(UnifiedModel model, float tx, float ty, float tz) {
        for (MeshGroup group : model.getAllMeshGroups()) {
            group.setPivot(group.getPivotX() + tx, group.getPivotY() + ty, group.getPivotZ() + tz);
            for (Mesh mesh : group.getMeshes()) {
                for (Vertex v : mesh.getVertices()) {
                    v.setPosition(v.getX() + tx, v.getY() + ty, v.getZ() + tz);
                }
            }
        }
        model.computeBoundingBox();
    }

    /**
     * 将模型居中到原点（基于包围盒中心）
     */
    public static void centerToOrigin(UnifiedModel model) {
        model.computeBoundingBox();
        AABB box = model.getBoundingBox();
        if (box == null) return;
        translate(model, -box.getCenterX(), -box.getCenterY(), -box.getCenterZ());
    }

    // ========================= 模型合并 =========================

    /**
     * 合并两个模型
     * source 的所有 MeshGroup 会被添加到 target 中
     * 如果有同名组，会添加后缀避免冲突
     */
    public static void merge(UnifiedModel target, UnifiedModel source) {
        Set<String> existingNames = target.getMeshGroupNames();
        for (MeshGroup group : source.getAllMeshGroups()) {
            String name = group.getName();
            if (existingNames.contains(name)) {
                // 解决命名冲突
                int suffix = 1;
                while (existingNames.contains(name + "_" + suffix)) suffix++;
                // 需要创建一个新的组（因为名称不可变）
                MeshGroup newGroup = new MeshGroup(name + "_" + suffix);
                for (Mesh mesh : group.getMeshes()) {
                    newGroup.addMesh(mesh);
                }
                newGroup.setLocalTransform(group.getLocalTransform());
                newGroup.setPivot(group.getPivotX(), group.getPivotY(), group.getPivotZ());
                newGroup.setVisible(group.isVisible());
                target.addMeshGroup(newGroup);
            } else {
                target.addMeshGroup(group);
            }
        }

        // 合并材质
        for (Material mat : source.getMaterials().values()) {
            if (target.getMaterial(mat.getName()) == null) {
                target.addMaterial(mat);
            }
        }

        target.computeBoundingBox();
    }

    // ========================= 统计 =========================

    /**
     * 统计模型的顶点数和面数
     * @return [totalVertices, totalFaces]
     */
    public static int[] countVerticesAndFaces(UnifiedModel model) {
        int totalVerts = 0, totalFaces = 0;
        for (MeshGroup group : model.getAllMeshGroups()) {
            for (Mesh mesh : group.getMeshes()) {
                totalVerts += mesh.getVertexCount();
                totalFaces += mesh.getFaceCount();
            }
        }
        return new int[] { totalVerts, totalFaces };
    }

    /**
     * 获取模型信息的调试字符串
     */
    public static String getDebugInfo(UnifiedModel model) {
        int[] counts = countVerticesAndFaces(model);
        StringBuilder sb = new StringBuilder();
        sb.append("=== Model: ").append(model.getName()).append(" ===\n");
        sb.append("Source format: ").append(model.getSourceFormat()).append("\n");
        sb.append("Total vertices: ").append(counts[0]).append("\n");
        sb.append("Total faces: ").append(counts[1]).append("\n");
        sb.append("MeshGroups (").append(model.getMeshGroupNames().size()).append("):\n");
        for (MeshGroup group : model.getAllMeshGroups()) {
            int gv = 0, gf = 0;
            for (Mesh m : group.getMeshes()) { gv += m.getVertexCount(); gf += m.getFaceCount(); }
            sb.append("  - ").append(group.getName())
              .append(" [meshes=").append(group.getMeshes().size())
              .append(", verts=").append(gv)
              .append(", faces=").append(gf)
              .append(", parent=").append(group.getParentName())
              .append("]\n");
        }
        sb.append("Materials (").append(model.getMaterials().size()).append("):\n");
        for (Material mat : model.getMaterials().values()) {
            sb.append("  - ").append(mat.getName());
            if (mat.getDiffuseTexture() != null) {
                sb.append(" [tex=").append(mat.getDiffuseTexture()).append("]");
            }
            sb.append("\n");
        }
        if (model.hasSkeleton()) {
            sb.append("Skeleton: ").append(model.getSkeleton().getBoneCount()).append(" bones\n");
        }
        if (model.hasAnimations()) {
            sb.append("Animations (").append(model.getAnimations().size()).append("):\n");
            for (Animation anim : model.getAnimations().values()) {
                sb.append("  - ").append(anim.getName())
                  .append(" [duration=").append(anim.getDuration())
                  .append("s, loop=").append(anim.isLoop())
                  .append(", channels=").append(anim.getChannels().size())
                  .append("]\n");
            }
        }
        AABB box = model.getBoundingBox();
        if (box != null) {
            sb.append(String.format("BoundingBox: [%.2f,%.2f,%.2f] ~ [%.2f,%.2f,%.2f] (size: %.2f x %.2f x %.2f)\n",
                    box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
                    box.getWidth(), box.getHeight(), box.getDepth()));
        }
        return sb.toString();
    }
}
