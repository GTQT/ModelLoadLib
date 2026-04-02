package com.drppp.modelloader.api.model;

/**
 * 轴向包围盒
 */
public class AABB {

    public final float minX, minY, minZ;
    public final float maxX, maxY, maxZ;

    public AABB(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        this.minX = minX; this.minY = minY; this.minZ = minZ;
        this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ;
    }

    public float getWidth()  { return maxX - minX; }
    public float getHeight() { return maxY - minY; }
    public float getDepth()  { return maxZ - minZ; }

    public float getCenterX() { return (minX + maxX) / 2f; }
    public float getCenterY() { return (minY + maxY) / 2f; }
    public float getCenterZ() { return (minZ + maxZ) / 2f; }
}
