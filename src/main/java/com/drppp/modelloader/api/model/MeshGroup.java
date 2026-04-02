package com.drppp.modelloader.api.model;

import java.util.*;

/**
 * 网格组 - 对应模型中的一个逻辑部件
 * 例如 "body", "head", "arm_left"
 * 
 * 一个 MeshGroup 可以包含多个 Mesh（当部件使用多个材质时）
 */
public class MeshGroup {

    private final String name;
    private final List<Mesh> meshes;

    /** 该组相对于模型原点的局部变换 */
    private Transform localTransform;

    /** 该组的旋转枢纽点 */
    private float pivotX, pivotY, pivotZ;

    /** 父组名称，用于构建层次结构 */
    private String parentName;

    /** 子组名称列表 */
    private final List<String> childNames;

    /** 是否默认可见 */
    private boolean visible = true;

    public MeshGroup(String name) {
        this.name = name;
        this.meshes = new ArrayList<>();
        this.childNames = new ArrayList<>();
        this.localTransform = Transform.IDENTITY;
    }

    public String getName() { return name; }

    public void addMesh(Mesh mesh) {
        meshes.add(mesh);
    }

    public List<Mesh> getMeshes() {
        return Collections.unmodifiableList(meshes);
    }

    // ========== Transform ==========

    public Transform getLocalTransform() { return localTransform; }
    public void setLocalTransform(Transform localTransform) { this.localTransform = localTransform; }

    // ========== Pivot ==========

    public float getPivotX() { return pivotX; }
    public float getPivotY() { return pivotY; }
    public float getPivotZ() { return pivotZ; }

    public void setPivot(float x, float y, float z) {
        this.pivotX = x; this.pivotY = y; this.pivotZ = z;
    }

    // ========== Hierarchy ==========

    public String getParentName() { return parentName; }
    public void setParentName(String parentName) { this.parentName = parentName; }

    public List<String> getChildNames() { return childNames; }
    public void addChildName(String childName) { childNames.add(childName); }

    // ========== Visibility ==========

    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
}
