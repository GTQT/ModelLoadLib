package com.drppp.modelloader.loaders;

import com.drppp.modelloader.api.ModelLoadException;
import com.drppp.modelloader.api.model.Animation;
import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * GeckoLib / Bedrock 动画文件加载器（修复版）
 *
 * 修复: 支持静态值的对象形式 {"vector": [x,y,z]}
 *       这种格式在 Blockbench 导出的动画中很常见
 */
public class GeckoLibAnimationLoader {

    private static final Gson GSON = new GsonBuilder().create();

    public static Map<String, Animation> loadAnimations(ResourceLocation resource) throws ModelLoadException {
        try {
            IResource iResource = Minecraft.getMinecraft().getResourceManager().getResource(resource);
            InputStreamReader reader = new InputStreamReader(iResource.getInputStream(), StandardCharsets.UTF_8);
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            reader.close();

            java.util.Map<String, Animation> result = new java.util.LinkedHashMap<>();

            JsonObject animations = root.getAsJsonObject("animations");
            if (animations == null) {
                throw new ModelLoadException("No 'animations' key in: " + resource);
            }

            for (Map.Entry<String, JsonElement> entry : animations.entrySet()) {
                String animName = entry.getKey();
                JsonObject animJson = entry.getValue().getAsJsonObject();

                Animation anim = new Animation(animName);

                if (animJson.has("animation_length")) {
                    anim.setDuration(animJson.get("animation_length").getAsFloat());
                }

                if (animJson.has("loop")) {
                    JsonElement loopElem = animJson.get("loop");
                    if (loopElem.isJsonPrimitive()) {
                        if (loopElem.getAsJsonPrimitive().isBoolean()) {
                            anim.setLoop(loopElem.getAsBoolean());
                        } else {
                            anim.setLoop(false);
                        }
                    }
                }

                if (animJson.has("bones")) {
                    JsonObject bonesJson = animJson.getAsJsonObject("bones");
                    for (Map.Entry<String, JsonElement> boneEntry : bonesJson.entrySet()) {
                        String boneName = boneEntry.getKey();
                        JsonObject boneAnim = boneEntry.getValue().getAsJsonObject();

                        Animation.AnimationChannel channel = new Animation.AnimationChannel(boneName);

                        if (boneAnim.has("rotation")) {
                            parseChannelKeys(boneAnim.get("rotation"), channel, "rotation");
                        }
                        if (boneAnim.has("position")) {
                            parseChannelKeys(boneAnim.get("position"), channel, "position");
                        }
                        if (boneAnim.has("scale")) {
                            parseChannelKeys(boneAnim.get("scale"), channel, "scale");
                        }

                        anim.addChannel(channel);
                    }
                }

                result.put(animName, anim);
            }

            return result;

        } catch (ModelLoadException e) {
            throw e;
        } catch (Exception e) {
            throw new ModelLoadException("Failed to load GeckoLib animation: " + resource, e);
        }
    }

    /**
     * 解析通道关键帧
     *
     * 支持的格式:
     *   1. 静态数组:        [x, y, z]
     *   2. 静态对象:        { "vector": [x, y, z] }                    ← 修复新增
     *   3. 关键帧数组映射:  { "0.0": [x,y,z], "0.5": [x,y,z] }
     *   4. 关键帧对象映射:  { "0.0": { "vector": [x,y,z], "easing": "..." } }
     *   5. Catmull-Rom:     { "0.0": { "pre": [x,y,z], "post": [x,y,z] } }
     */
    private static void parseChannelKeys(JsonElement element,
                                         Animation.AnimationChannel channel,
                                         String type) {
        if (element.isJsonArray()) {
            // 格式 1: 静态数组 [x, y, z]
            JsonArray arr = element.getAsJsonArray();
            float x = arr.get(0).getAsFloat();
            float y = arr.get(1).getAsFloat();
            float z = arr.get(2).getAsFloat();
            addKey(channel, type, 0f, x, y, z);

        } else if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();

            // ★ 修复: 格式 2 — 检查是否是静态对象 { "vector": [x,y,z] }
            // 判断方法: 如果对象里有 "vector" 键但没有任何数字键，就是静态值
            if (obj.has("vector") && !hasNumericKey(obj)) {
                JsonArray arr = obj.getAsJsonArray("vector");
                float x = arr.get(0).getAsFloat();
                float y = arr.get(1).getAsFloat();
                float z = arr.get(2).getAsFloat();
                addKey(channel, type, 0f, x, y, z);
                return;
            }

            // 格式 3/4/5: 关键帧映射 { "0.0": ..., "0.5": ... }
            for (Map.Entry<String, JsonElement> kfEntry : obj.entrySet()) {
                String key = kfEntry.getKey();

                // ★ 修复: 跳过非数字键（如 "vector", "easing", "easingArgs"）
                if (!isNumericKey(key)) {
                    continue;
                }

                float time = Float.parseFloat(key);
                JsonElement value = kfEntry.getValue();

                float x, y, z;
                if (value.isJsonArray()) {
                    // 格式 3: { "0.0": [x, y, z] }
                    JsonArray arr = value.getAsJsonArray();
                    x = arr.get(0).getAsFloat();
                    y = arr.get(1).getAsFloat();
                    z = arr.get(2).getAsFloat();
                } else if (value.isJsonObject()) {
                    JsonObject kfObj = value.getAsJsonObject();
                    if (kfObj.has("vector")) {
                        // 格式 4: { "0.0": { "vector": [x,y,z], "easing": "..." } }
                        JsonArray arr = kfObj.getAsJsonArray("vector");
                        x = arr.get(0).getAsFloat();
                        y = arr.get(1).getAsFloat();
                        z = arr.get(2).getAsFloat();
                    } else if (kfObj.has("pre") || kfObj.has("post")) {
                        // 格式 5: Catmull-Rom
                        JsonArray arr = kfObj.has("post")
                                ? kfObj.getAsJsonArray("post")
                                : kfObj.getAsJsonArray("pre");
                        x = arr.get(0).getAsFloat();
                        y = arr.get(1).getAsFloat();
                        z = arr.get(2).getAsFloat();
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }

                addKey(channel, type, time, x, y, z);
            }
        }
    }

    /**
     * 判断字符串是否是数字（可作为时间值解析）
     */
    private static boolean isNumericKey(String key) {
        if (key == null || key.isEmpty()) return false;
        try {
            Float.parseFloat(key);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 判断 JsonObject 中是否有任何数字键
     * 用于区分静态对象 {"vector":[...]} 和关键帧映射 {"0.0":{...}, "0.5":{...}}
     */
    private static boolean hasNumericKey(JsonObject obj) {
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            if (isNumericKey(entry.getKey())) return true;
        }
        return false;
    }

    private static void addKey(Animation.AnimationChannel channel, String type,
                               float time, float x, float y, float z) {
        switch (type) {
            case "rotation":  channel.addRotationKey(time, x, y, z); break;
            case "position":  channel.addPositionKey(time, x, y, z); break;
            case "scale":     channel.addScaleKey(time, x, y, z);    break;
        }
    }
}
