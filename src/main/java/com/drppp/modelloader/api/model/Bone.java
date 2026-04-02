package com.drppp.modelloader.api.model;

/**
 * 单根骨骼
 */
public class Bone {

    private final String name;

    /** 父骨骼在 Skeleton 中的索引，-1 表示根骨骼 */
    private final int parentIndex;

    /** 绑定姿态下的局部变换 */
    private Transform bindPoseLocal;

    /** 绑定姿态的逆矩阵（用于蒙皮计算） */
    private float[] inverseBindMatrix;

    /** 旋转枢纽点 */
    private float pivotX, pivotY, pivotZ;

    public Bone(String name, int parentIndex) {
        this.name = name;
        this.parentIndex = parentIndex;
        this.bindPoseLocal = Transform.IDENTITY;
    }

    public String getName() { return name; }
    public int getParentIndex() { return parentIndex; }

    public Transform getBindPoseLocal() { return bindPoseLocal; }
    public void setBindPoseLocal(Transform transform) { this.bindPoseLocal = transform; }

    public float[] getInverseBindMatrix() { return inverseBindMatrix; }
    public void setInverseBindMatrix(float[] matrix) { this.inverseBindMatrix = matrix; }

    public float getPivotX() { return pivotX; }
    public float getPivotY() { return pivotY; }
    public float getPivotZ() { return pivotZ; }
    public void setPivot(float x, float y, float z) {
        this.pivotX = x; this.pivotY = y; this.pivotZ = z;
    }
}
