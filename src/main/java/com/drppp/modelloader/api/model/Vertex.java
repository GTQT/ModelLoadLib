package com.drppp.modelloader.api.model;

/**
 * 顶点数据
 * 包含位置、法线、UV、颜色、骨骼权重等信息
 */
public class Vertex {

    /** 位置 */
    private float x, y, z;

    /** 法线 */
    private float nx, ny, nz;

    /** 纹理坐标 */
    private float u, v;

    /** 顶点颜色 (RGBA, 0~1) */
    private float r = 1f, g = 1f, b = 1f, a = 1f;

    /** 骨骼绑定：最多支持4根骨骼 */
    private int[] boneIndices;
    private float[] boneWeights;

    public Vertex() {}

    public Vertex(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    // ========== Position ==========

    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }

    public Vertex setPosition(float x, float y, float z) {
        this.x = x; this.y = y; this.z = z;
        return this;
    }

    // ========== Normal ==========

    public float getNx() { return nx; }
    public float getNy() { return ny; }
    public float getNz() { return nz; }

    public Vertex setNormal(float nx, float ny, float nz) {
        this.nx = nx; this.ny = ny; this.nz = nz;
        return this;
    }

    // ========== UV ==========

    public float getU() { return u; }
    public float getV() { return v; }

    public Vertex setUV(float u, float v) {
        this.u = u; this.v = v;
        return this;
    }

    // ========== Color ==========

    public float getR() { return r; }
    public float getG() { return g; }
    public float getB() { return b; }
    public float getA() { return a; }

    public Vertex setColor(float r, float g, float b, float a) {
        this.r = r; this.g = g; this.b = b; this.a = a;
        return this;
    }

    // ========== Bone Binding ==========

    public int[] getBoneIndices() { return boneIndices; }
    public float[] getBoneWeights() { return boneWeights; }

    public boolean hasBoneData() {
        return boneIndices != null && boneIndices.length > 0;
    }

    /**
     * 设置骨骼绑定数据
     * @param indices 骨骼索引数组（最多4个）
     * @param weights 骨骼权重数组（与索引对应）
     */
    public Vertex setBoneBinding(int[] indices, float[] weights) {
        this.boneIndices = indices;
        this.boneWeights = weights;
        return this;
    }
}
