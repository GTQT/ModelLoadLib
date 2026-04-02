package com.drppp.modelloader.api.model;

/**
 * 3D变换（平移、旋转、缩放）
 * 使用欧拉角表示旋转（与 Minecraft/Blockbench 保持一致）
 */
public class Transform {

    public static final Transform IDENTITY = new Transform();

    /** 平移 */
    private float tx, ty, tz;

    /** 旋转（度数，绕各轴） */
    private float rx, ry, rz;

    /** 缩放 */
    private float sx = 1f, sy = 1f, sz = 1f;

    public Transform() {}

    public Transform(float tx, float ty, float tz,
                     float rx, float ry, float rz,
                     float sx, float sy, float sz) {
        this.tx = tx; this.ty = ty; this.tz = tz;
        this.rx = rx; this.ry = ry; this.rz = rz;
        this.sx = sx; this.sy = sy; this.sz = sz;
    }

    // ========== Translation ==========
    public float getTx() { return tx; }
    public float getTy() { return ty; }
    public float getTz() { return tz; }
    public Transform setTranslation(float x, float y, float z) {
        this.tx = x; this.ty = y; this.tz = z;
        return this;
    }

    // ========== Rotation ==========
    public float getRx() { return rx; }
    public float getRy() { return ry; }
    public float getRz() { return rz; }
    public Transform setRotation(float x, float y, float z) {
        this.rx = x; this.ry = y; this.rz = z;
        return this;
    }

    // ========== Scale ==========
    public float getSx() { return sx; }
    public float getSy() { return sy; }
    public float getSz() { return sz; }
    public Transform setScale(float x, float y, float z) {
        this.sx = x; this.sy = y; this.sz = z;
        return this;
    }

    /**
     * 将变换应用到 4x4 矩阵（列优先，OpenGL布局）
     * @return 16元素的float数组
     */
    public float[] toMatrix4f() {
        float cosX = (float) Math.cos(Math.toRadians(rx));
        float sinX = (float) Math.sin(Math.toRadians(rx));
        float cosY = (float) Math.cos(Math.toRadians(ry));
        float sinY = (float) Math.sin(Math.toRadians(ry));
        float cosZ = (float) Math.cos(Math.toRadians(rz));
        float sinZ = (float) Math.sin(Math.toRadians(rz));

        // R = Rz * Ry * Rx (column-major)
        float[] m = new float[16];

        m[0]  = sx * cosY * cosZ;
        m[1]  = sx * cosY * sinZ;
        m[2]  = sx * (-sinY);
        m[3]  = 0;

        m[4]  = sy * (sinX * sinY * cosZ - cosX * sinZ);
        m[5]  = sy * (sinX * sinY * sinZ + cosX * cosZ);
        m[6]  = sy * (sinX * cosY);
        m[7]  = 0;

        m[8]  = sz * (cosX * sinY * cosZ + sinX * sinZ);
        m[9]  = sz * (cosX * sinY * sinZ - sinX * cosZ);
        m[10] = sz * (cosX * cosY);
        m[11] = 0;

        m[12] = tx;
        m[13] = ty;
        m[14] = tz;
        m[15] = 1;

        return m;
    }

    public boolean isIdentity() {
        return tx == 0 && ty == 0 && tz == 0
            && rx == 0 && ry == 0 && rz == 0
            && sx == 1 && sy == 1 && sz == 1;
    }
}
