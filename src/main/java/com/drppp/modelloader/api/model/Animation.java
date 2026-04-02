package com.drppp.modelloader.api.model;

import java.util.*;

/**
 * 动画定义
 */
public class Animation {

    private final String name;

    /** 动画时长（秒） */
    private float duration;

    /** 是否循环播放 */
    private boolean loop = false;

    /** 每个骨骼/组的关键帧通道：目标名称 -> 通道列表 */
    private final Map<String, AnimationChannel> channels;

    public Animation(String name) {
        this.name = name;
        this.channels = new LinkedHashMap<>();
    }

    public String getName() { return name; }

    public float getDuration() { return duration; }
    public void setDuration(float duration) { this.duration = duration; }

    public boolean isLoop() { return loop; }
    public void setLoop(boolean loop) { this.loop = loop; }

    public void addChannel(AnimationChannel channel) {
        channels.put(channel.getTargetName(), channel);
    }

    public AnimationChannel getChannel(String targetName) {
        return channels.get(targetName);
    }

    public Map<String, AnimationChannel> getChannels() {
        return Collections.unmodifiableMap(channels);
    }

    /**
     * 动画通道 - 控制一个目标（骨骼或MeshGroup）的变换
     */
    public static class AnimationChannel {

        private final String targetName;

        /** 位置关键帧 */
        private final List<Keyframe> positionKeys = new ArrayList<>();

        /** 旋转关键帧 */
        private final List<Keyframe> rotationKeys = new ArrayList<>();

        /** 缩放关键帧 */
        private final List<Keyframe> scaleKeys = new ArrayList<>();

        public AnimationChannel(String targetName) {
            this.targetName = targetName;
        }

        public String getTargetName() { return targetName; }

        public void addPositionKey(float time, float x, float y, float z) {
            positionKeys.add(new Keyframe(time, x, y, z));
        }

        public void addRotationKey(float time, float x, float y, float z) {
            rotationKeys.add(new Keyframe(time, x, y, z));
        }

        public void addScaleKey(float time, float x, float y, float z) {
            scaleKeys.add(new Keyframe(time, x, y, z));
        }

        public List<Keyframe> getPositionKeys() { return positionKeys; }
        public List<Keyframe> getRotationKeys() { return rotationKeys; }
        public List<Keyframe> getScaleKeys() { return scaleKeys; }

        /**
         * 在指定时间点对关键帧进行线性插值
         * @param keys 关键帧列表
         * @param time 当前时间
         * @return 插值后的 [x, y, z]，如果无关键帧则返回 null
         */
        public static float[] interpolateLinear(List<Keyframe> keys, float time) {
            if (keys.isEmpty()) return null;
            if (keys.size() == 1) return keys.get(0).getValues();

            // 在第一帧之前
            if (time <= keys.get(0).time) return keys.get(0).getValues();
            // 在最后一帧之后
            if (time >= keys.get(keys.size() - 1).time) return keys.get(keys.size() - 1).getValues();

            // 找到相邻的两帧
            for (int i = 0; i < keys.size() - 1; i++) {
                Keyframe a = keys.get(i);
                Keyframe b = keys.get(i + 1);
                if (time >= a.time && time <= b.time) {
                    float t = (time - a.time) / (b.time - a.time);
                    return new float[] {
                        a.x + (b.x - a.x) * t,
                        a.y + (b.y - a.y) * t,
                        a.z + (b.z - a.z) * t
                    };
                }
            }
            return keys.get(keys.size() - 1).getValues();
        }
    }

    /**
     * 单个关键帧
     */
    public static class Keyframe {

        public final float time;
        public final float x, y, z;

        public Keyframe(float time, float x, float y, float z) {
            this.time = time;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public float[] getValues() {
            return new float[] { x, y, z };
        }
    }
}
