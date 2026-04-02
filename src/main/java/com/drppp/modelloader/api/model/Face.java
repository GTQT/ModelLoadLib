package com.drppp.modelloader.api.model;

/**
 * 面（三角形）
 * 统一使用三角面表示，加载器负责将四边面拆分为三角面
 */
public class Face {

    /** 三个顶点的索引 */
    private final int v0, v1, v2;

    /** 面所使用的材质名称 */
    private String materialName;

    /** 平滑组 ID，-1 表示不属于任何平滑组 */
    private int smoothGroup = -1;

    public Face(int v0, int v1, int v2) {
        this.v0 = v0;
        this.v1 = v1;
        this.v2 = v2;
    }

    public int getV0() { return v0; }
    public int getV1() { return v1; }
    public int getV2() { return v2; }

    /**
     * 以数组形式获取三个顶点索引
     */
    public int[] getIndices() {
        return new int[] { v0, v1, v2 };
    }

    public String getMaterialName() { return materialName; }
    public Face setMaterialName(String materialName) {
        this.materialName = materialName;
        return this;
    }

    public int getSmoothGroup() { return smoothGroup; }
    public Face setSmoothGroup(int smoothGroup) {
        this.smoothGroup = smoothGroup;
        return this;
    }
}
