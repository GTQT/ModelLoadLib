package com.drppp.modelloader.api.model;

import net.minecraft.util.ResourceLocation;

/**
 * 材质定义
 */
public class Material {

    private final String name;

    /** 漫反射贴图 */
    private ResourceLocation diffuseTexture;

    /** 法线贴图（可选） */
    private ResourceLocation normalTexture;

    /** 高光/发光贴图（可选） */
    private ResourceLocation emissiveTexture;

    /** 漫反射颜色 (RGBA) */
    private float diffuseR = 1f, diffuseG = 1f, diffuseB = 1f, diffuseA = 1f;

    /** 是否半透明 */
    private boolean translucent = false;

    /** 是否双面渲染 */
    private boolean doubleSided = false;

    /** 发光亮度 (0~15, Minecraft light level) */
    private int emissiveLevel = 0;

    public Material(String name) {
        this.name = name;
    }

    public String getName() { return name; }

    // ========== Textures ==========

    public ResourceLocation getDiffuseTexture() { return diffuseTexture; }
    public void setDiffuseTexture(ResourceLocation texture) { this.diffuseTexture = texture; }

    public ResourceLocation getNormalTexture() { return normalTexture; }
    public void setNormalTexture(ResourceLocation texture) { this.normalTexture = texture; }

    public ResourceLocation getEmissiveTexture() { return emissiveTexture; }
    public void setEmissiveTexture(ResourceLocation texture) { this.emissiveTexture = texture; }

    // ========== Color ==========

    public float getDiffuseR() { return diffuseR; }
    public float getDiffuseG() { return diffuseG; }
    public float getDiffuseB() { return diffuseB; }
    public float getDiffuseA() { return diffuseA; }

    public void setDiffuseColor(float r, float g, float b, float a) {
        this.diffuseR = r; this.diffuseG = g; this.diffuseB = b; this.diffuseA = a;
    }

    // ========== Render flags ==========

    public boolean isTranslucent() { return translucent; }
    public void setTranslucent(boolean translucent) { this.translucent = translucent; }

    public boolean isDoubleSided() { return doubleSided; }
    public void setDoubleSided(boolean doubleSided) { this.doubleSided = doubleSided; }

    public int getEmissiveLevel() { return emissiveLevel; }
    public void setEmissiveLevel(int emissiveLevel) { this.emissiveLevel = emissiveLevel; }
}
