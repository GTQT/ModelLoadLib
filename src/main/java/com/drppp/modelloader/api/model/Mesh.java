package com.drppp.modelloader.api.model;

import java.util.*;

/**
 * 网格 - 一组共享相同材质的三角面片
 */
public class Mesh {

    private final List<Vertex> vertices;
    private final List<Face> faces;
    private String materialName;

    public Mesh() {
        this.vertices = new ArrayList<>();
        this.faces = new ArrayList<>();
    }

    public void addVertex(Vertex vertex) {
        vertices.add(vertex);
    }

    public void addFace(Face face) {
        faces.add(face);
    }

    /**
     * 将四边面拆分为两个三角面并添加
     */
    public void addQuadAsTris(int v0, int v1, int v2, int v3) {
        faces.add(new Face(v0, v1, v2));
        faces.add(new Face(v0, v2, v3));
    }

    public List<Vertex> getVertices() {
        return vertices;
    }

    public List<Face> getFaces() {
        return faces;
    }

    public int getVertexCount() {
        return vertices.size();
    }

    public int getFaceCount() {
        return faces.size();
    }

    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }
}
