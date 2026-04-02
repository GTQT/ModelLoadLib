package com.drppp.modelloader.api.model;

import java.util.*;

/**
 * 统一模型表示
 * 
 * 所有格式（OBJ, GeckoLib, B3D等）加载后都转换为此结构，
 * 渲染器只需要针对这一个结构编写渲染代码。
 * 
 * 结构层次:
 *   UnifiedModel
 *     ├── MeshGroup ("body", "head", "arm_left" ...)
 *     │     ├── Mesh (三角面片集合)
 *     │     │     ├── Vertex[] (顶点数据)
 *     │     │     └── Face[]   (面索引)
 *     │     └── Transform (局部变换)
 *     ├── Skeleton (可选，骨骼动画)
 *     │     └── Bone[] (骨骼树)
 *     ├── AnimationSet (可选，动画集合)
 *     │     └── Animation[] (关键帧动画)
 *     └── MaterialMap (材质映射)
 */
public class UnifiedModel {

    /** 模型名称 */
    private final String name;

    /** 网格组，按名称索引（对应模型中的 part / group） */
    private final Map<String, MeshGroup> meshGroups;

    /** 骨骼系统（可选） */
    private Skeleton skeleton;

    /** 动画集合（可选） */
    private final Map<String, Animation> animations;

    /** 材质映射：材质名 -> 材质数据 */
    private final Map<String, Material> materials;

    /** 模型的轴向包围盒 */
    private AABB boundingBox;

    /** 模型来源格式 */
    private final String sourceFormat;

    /** 自定义扩展数据，用于格式特有的信息透传 */
    private final Map<String, Object> extraData;

    public UnifiedModel(String name, String sourceFormat) {
        this.name = name;
        this.sourceFormat = sourceFormat;
        this.meshGroups = new LinkedHashMap<>();
        this.animations = new LinkedHashMap<>();
        this.materials = new LinkedHashMap<>();
        this.extraData = new HashMap<>();
    }

    // ========== MeshGroup 操作 ==========

    public void addMeshGroup(MeshGroup group) {
        this.meshGroups.put(group.getName(), group);
    }

    public MeshGroup getMeshGroup(String name) {
        return meshGroups.get(name);
    }

    public Collection<MeshGroup> getAllMeshGroups() {
        return Collections.unmodifiableCollection(meshGroups.values());
    }

    public Set<String> getMeshGroupNames() {
        return Collections.unmodifiableSet(meshGroups.keySet());
    }

    // ========== Skeleton ==========

    public void setSkeleton(Skeleton skeleton) {
        this.skeleton = skeleton;
    }

    public Skeleton getSkeleton() {
        return skeleton;
    }

    public boolean hasSkeleton() {
        return skeleton != null && !skeleton.getBones().isEmpty();
    }

    // ========== Animation ==========

    public void addAnimation(Animation animation) {
        this.animations.put(animation.getName(), animation);
    }

    public Animation getAnimation(String name) {
        return animations.get(name);
    }

    public Map<String, Animation> getAnimations() {
        return Collections.unmodifiableMap(animations);
    }

    public boolean hasAnimations() {
        return !animations.isEmpty();
    }

    // ========== Material ==========

    public void addMaterial(Material material) {
        this.materials.put(material.getName(), material);
    }

    public Material getMaterial(String name) {
        return materials.get(name);
    }

    public Map<String, Material> getMaterials() {
        return Collections.unmodifiableMap(materials);
    }

    // ========== BoundingBox ==========

    public AABB getBoundingBox() {
        return boundingBox;
    }

    public void setBoundingBox(AABB boundingBox) {
        this.boundingBox = boundingBox;
    }

    /**
     * 根据所有网格顶点自动计算包围盒
     */
    public void computeBoundingBox() {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

        for (MeshGroup group : meshGroups.values()) {
            for (Mesh mesh : group.getMeshes()) {
                for (Vertex v : mesh.getVertices()) {
                    minX = Math.min(minX, v.getX());
                    minY = Math.min(minY, v.getY());
                    minZ = Math.min(minZ, v.getZ());
                    maxX = Math.max(maxX, v.getX());
                    maxY = Math.max(maxY, v.getY());
                    maxZ = Math.max(maxZ, v.getZ());
                }
            }
        }

        this.boundingBox = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    // ========== Extra Data ==========

    public void putExtra(String key, Object value) {
        extraData.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getExtra(String key) {
        return (T) extraData.get(key);
    }

    // ========== Getters ==========

    public String getName() {
        return name;
    }

    public String getSourceFormat() {
        return sourceFormat;
    }
}
