package com.drppp.modelloader.api.model;

import java.util.*;

/**
 * 骨骼系统
 */
public class Skeleton {

    private final List<Bone> bones;
    private final Map<String, Integer> boneNameToIndex;

    public Skeleton() {
        this.bones = new ArrayList<>();
        this.boneNameToIndex = new HashMap<>();
    }

    public int addBone(Bone bone) {
        int index = bones.size();
        bones.add(bone);
        boneNameToIndex.put(bone.getName(), index);
        return index;
    }

    public Bone getBone(int index) {
        return bones.get(index);
    }

    public Bone getBone(String name) {
        Integer idx = boneNameToIndex.get(name);
        return idx != null ? bones.get(idx) : null;
    }

    public int getBoneIndex(String name) {
        Integer idx = boneNameToIndex.get(name);
        return idx != null ? idx : -1;
    }

    public List<Bone> getBones() {
        return Collections.unmodifiableList(bones);
    }

    public int getBoneCount() {
        return bones.size();
    }

    /**
     * 获取所有根骨骼（没有父节点的骨骼）
     */
    public List<Bone> getRootBones() {
        List<Bone> roots = new ArrayList<>();
        for (Bone bone : bones) {
            if (bone.getParentIndex() < 0) {
                roots.add(bone);
            }
        }
        return roots;
    }
}
