package com.drppp.modelloader.anim;

import com.drppp.modelloader.api.model.Animation;
import com.drppp.modelloader.api.model.UnifiedModel;

import java.util.*;

/**
 * 动画控制器
 * 
 * 管理模型的动画播放状态，支持：
 *   - 播放/暂停/停止
 *   - 循环播放
 *   - 动画过渡（淡入淡出混合）
 *   - 动画队列
 *   - 播放速度控制
 * 
 * 用法:
 *   AnimationController controller = new AnimationController(model);
 *   controller.play("walk", true);       // 循环播放walk
 *   controller.crossFade("run", 0.3f);   // 0.3秒过渡到run
 *   
 *   // 每帧更新：
 *   controller.update(deltaTime);
 *   AnimationState state = controller.getCurrentState();
 */
public class AnimationController {

    private final UnifiedModel model;

    /** 当前活跃的动画层 */
    private final List<AnimationLayer> activeLayers = new ArrayList<>();

    /** 全局播放速度 */
    private float globalSpeed = 1.0f;

    /** 是否暂停 */
    private boolean paused = false;

    public AnimationController(UnifiedModel model) {
        this.model = model;
    }

    // ========================= 播放控制 =========================

    /**
     * 播放动画（立即切换，无过渡）
     * @param animationName 动画名称
     * @param loop 是否循环
     */
    public void play(String animationName, boolean loop) {
        Animation anim = model.getAnimation(animationName);
        if (anim == null) return;

        activeLayers.clear();
        AnimationLayer layer = new AnimationLayer(anim);
        layer.loop = loop;
        layer.weight = 1.0f;
        activeLayers.add(layer);
    }

    /**
     * 交叉淡入淡出过渡到新动画
     * @param animationName 目标动画名称
     * @param fadeDuration 过渡时长（秒）
     */
    public void crossFade(String animationName, float fadeDuration) {
        Animation anim = model.getAnimation(animationName);
        if (anim == null) return;

        // 将当前所有层标记为淡出
        for (AnimationLayer layer : activeLayers) {
            layer.fadingOut = true;
            layer.fadeSpeed = 1.0f / Math.max(fadeDuration, 0.001f);
        }

        // 添加新层，从权重0开始淡入
        AnimationLayer newLayer = new AnimationLayer(anim);
        newLayer.loop = anim.isLoop();
        newLayer.weight = 0f;
        newLayer.fadingIn = true;
        newLayer.fadeSpeed = 1.0f / Math.max(fadeDuration, 0.001f);
        activeLayers.add(newLayer);
    }

    /**
     * 叠加播放动画（在现有动画之上混合）
     * @param animationName 动画名称
     * @param weight 混合权重 (0~1)
     * @param loop 是否循环
     */
    public void additive(String animationName, float weight, boolean loop) {
        Animation anim = model.getAnimation(animationName);
        if (anim == null) return;

        AnimationLayer layer = new AnimationLayer(anim);
        layer.loop = loop;
        layer.weight = weight;
        layer.additive = true;
        activeLayers.add(layer);
    }

    /**
     * 停止所有动画
     */
    public void stop() {
        activeLayers.clear();
    }

    /**
     * 停止指定动画
     */
    public void stop(String animationName) {
        activeLayers.removeIf(layer -> layer.animation.getName().equals(animationName));
    }

    public void pause() { paused = true; }
    public void resume() { paused = false; }
    public boolean isPaused() { return paused; }

    public void setGlobalSpeed(float speed) { this.globalSpeed = speed; }
    public float getGlobalSpeed() { return globalSpeed; }

    // ========================= 更新 =========================

    /**
     * 每帧更新，推进动画时间
     * @param deltaTime 距离上一帧的时间（秒）
     */
    public void update(float deltaTime) {
        if (paused) return;

        float dt = deltaTime * globalSpeed;
        Iterator<AnimationLayer> it = activeLayers.iterator();

        while (it.hasNext()) {
            AnimationLayer layer = it.next();

            // 推进时间
            layer.time += dt * layer.speed;

            // 处理循环/结束
            if (layer.animation.getDuration() > 0) {
                if (layer.loop) {
                    layer.time = layer.time % layer.animation.getDuration();
                } else if (layer.time > layer.animation.getDuration()) {
                    layer.time = layer.animation.getDuration();
                    layer.finished = true;
                }
            }

            // 处理淡入
            if (layer.fadingIn) {
                layer.weight += dt * layer.fadeSpeed;
                if (layer.weight >= 1.0f) {
                    layer.weight = 1.0f;
                    layer.fadingIn = false;
                }
            }

            // 处理淡出
            if (layer.fadingOut) {
                layer.weight -= dt * layer.fadeSpeed;
                if (layer.weight <= 0f) {
                    it.remove();
                    continue;
                }
            }

            // 移除已结束且非循环的动画
            if (layer.finished && !layer.loop && !layer.fadingOut) {
                it.remove();
            }
        }
    }

    // ========================= 查询状态 =========================

    /**
     * 获取当前动画状态（用于渲染）
     * 
     * 返回每个骨骼/组的最终变换值（所有活跃层混合后的结果）
     */
    public AnimationState getCurrentState() {
        AnimationState state = new AnimationState();

        for (AnimationLayer layer : activeLayers) {
            Animation anim = layer.animation;
            float time = layer.time;
            float weight = layer.weight;

            for (Map.Entry<String, Animation.AnimationChannel> entry : anim.getChannels().entrySet()) {
                String target = entry.getKey();
                Animation.AnimationChannel channel = entry.getValue();

                AnimationState.BoneState boneState = state.getOrCreate(target);

                // 位移
                float[] pos = Animation.AnimationChannel.interpolateLinear(channel.getPositionKeys(), time);
                if (pos != null) {
                    if (layer.additive) {
                        boneState.tx += pos[0] * weight;
                        boneState.ty += pos[1] * weight;
                        boneState.tz += pos[2] * weight;
                    } else {
                        boneState.tx = lerp(boneState.tx, pos[0], weight);
                        boneState.ty = lerp(boneState.ty, pos[1], weight);
                        boneState.tz = lerp(boneState.tz, pos[2], weight);
                    }
                    boneState.hasPosition = true;
                }

                // 旋转
                float[] rot = Animation.AnimationChannel.interpolateLinear(channel.getRotationKeys(), time);
                if (rot != null) {
                    if (layer.additive) {
                        boneState.rx += rot[0] * weight;
                        boneState.ry += rot[1] * weight;
                        boneState.rz += rot[2] * weight;
                    } else {
                        boneState.rx = lerp(boneState.rx, rot[0], weight);
                        boneState.ry = lerp(boneState.ry, rot[1], weight);
                        boneState.rz = lerp(boneState.rz, rot[2], weight);
                    }
                    boneState.hasRotation = true;
                }

                // 缩放
                float[] scale = Animation.AnimationChannel.interpolateLinear(channel.getScaleKeys(), time);
                if (scale != null) {
                    if (layer.additive) {
                        boneState.sx += (scale[0] - 1f) * weight;
                        boneState.sy += (scale[1] - 1f) * weight;
                        boneState.sz += (scale[2] - 1f) * weight;
                    } else {
                        boneState.sx = lerp(boneState.sx, scale[0], weight);
                        boneState.sy = lerp(boneState.sy, scale[1], weight);
                        boneState.sz = lerp(boneState.sz, scale[2], weight);
                    }
                    boneState.hasScale = true;
                }
            }
        }

        return state;
    }

    /**
     * 获取当前正在播放的动画名称列表
     */
    public List<String> getPlayingAnimations() {
        List<String> names = new ArrayList<>();
        for (AnimationLayer layer : activeLayers) {
            names.add(layer.animation.getName());
        }
        return names;
    }

    /**
     * 检查指定动画是否正在播放
     */
    public boolean isPlaying(String animationName) {
        for (AnimationLayer layer : activeLayers) {
            if (layer.animation.getName().equals(animationName)) return true;
        }
        return false;
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    // ========================= 内部类 =========================

    private static class AnimationLayer {
        final Animation animation;
        float time = 0f;
        float weight = 1f;
        float speed = 1f;
        boolean loop = false;
        boolean additive = false;
        boolean finished = false;

        boolean fadingIn = false;
        boolean fadingOut = false;
        float fadeSpeed = 1f;

        AnimationLayer(Animation animation) {
            this.animation = animation;
        }
    }

    /**
     * 动画状态快照 - 包含所有骨骼的当前变换
     */
    public static class AnimationState {

        private final Map<String, BoneState> bones = new LinkedHashMap<>();

        public BoneState getOrCreate(String boneName) {
            return bones.computeIfAbsent(boneName, k -> new BoneState());
        }

        public BoneState get(String boneName) {
            return bones.get(boneName);
        }

        public Map<String, BoneState> getAllBoneStates() {
            return Collections.unmodifiableMap(bones);
        }

        /**
         * 单根骨骼的变换状态
         */
        public static class BoneState {
            public float tx, ty, tz;
            public float rx, ry, rz;
            public float sx = 1f, sy = 1f, sz = 1f;

            public boolean hasPosition;
            public boolean hasRotation;
            public boolean hasScale;
        }
    }
}
