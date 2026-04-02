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
 * GeckoLib 动画文件加载器
 * 
 * 用于加载 .animation.json 文件，可独立使用或配合 GeckoLibModelLoader
 * 
 * 格式示例:
 * {
 *   "format_version": "1.8.0",
 *   "animations": {
 *     "animation.model.walk": {
 *       "animation_length": 1.0,
 *       "loop": true,
 *       "bones": {
 *         "body": {
 *           "rotation": { "0.0": [0,0,0], "0.5": [10,0,0], "1.0": [0,0,0] },
 *           "position": { ... }
 *         }
 *       }
 *     }
 *   }
 * }
 */
public class GeckoLibAnimationLoader {

    private static final Gson GSON = new GsonBuilder().create();

    /**
     * 加载动画文件，返回所有动画
     */
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

                // 动画时长
                if (animJson.has("animation_length")) {
                    anim.setDuration(animJson.get("animation_length").getAsFloat());
                }

                // 循环
                if (animJson.has("loop")) {
                    JsonElement loopElem = animJson.get("loop");
                    if (loopElem.isJsonPrimitive()) {
                        if (loopElem.getAsJsonPrimitive().isBoolean()) {
                            anim.setLoop(loopElem.getAsBoolean());
                        } else {
                            // "hold_on_last_frame" 等
                            anim.setLoop(false);
                        }
                    }
                }

                // 骨骼通道
                if (animJson.has("bones")) {
                    JsonObject bonesJson = animJson.getAsJsonObject("bones");
                    for (Map.Entry<String, JsonElement> boneEntry : bonesJson.entrySet()) {
                        String boneName = boneEntry.getKey();
                        JsonObject boneAnim = boneEntry.getValue().getAsJsonObject();

                        Animation.AnimationChannel channel = new Animation.AnimationChannel(boneName);

                        // 旋转
                        if (boneAnim.has("rotation")) {
                            parseChannelKeys(boneAnim.get("rotation"), channel, "rotation");
                        }

                        // 位移
                        if (boneAnim.has("position")) {
                            parseChannelKeys(boneAnim.get("position"), channel, "position");
                        }

                        // 缩放
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
     *   1. 静态值: [x, y, z]
     *   2. 关键帧映射: { "0.0": [x,y,z], "0.5": [x,y,z] }
     *   3. 带缓动的关键帧: { "0.0": { "vector": [x,y,z], "easing": "..." } }
     */
    private static void parseChannelKeys(JsonElement element,
                                         Animation.AnimationChannel channel,
                                         String type) {
        if (element.isJsonArray()) {
            // 静态值 - 创建一个 0 时间的关键帧
            JsonArray arr = element.getAsJsonArray();
            float x = arr.get(0).getAsFloat();
            float y = arr.get(1).getAsFloat();
            float z = arr.get(2).getAsFloat();
            addKey(channel, type, 0f, x, y, z);
        } else if (element.isJsonObject()) {
            // 关键帧映射
            JsonObject keyframes = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> kfEntry : keyframes.entrySet()) {
                float time = Float.parseFloat(kfEntry.getKey());
                JsonElement value = kfEntry.getValue();

                float x, y, z;
                if (value.isJsonArray()) {
                    JsonArray arr = value.getAsJsonArray();
                    x = arr.get(0).getAsFloat();
                    y = arr.get(1).getAsFloat();
                    z = arr.get(2).getAsFloat();
                } else if (value.isJsonObject()) {
                    JsonObject kfObj = value.getAsJsonObject();
                    if (kfObj.has("vector")) {
                        JsonArray arr = kfObj.getAsJsonArray("vector");
                        x = arr.get(0).getAsFloat();
                        y = arr.get(1).getAsFloat();
                        z = arr.get(2).getAsFloat();
                    } else if (kfObj.has("pre") || kfObj.has("post")) {
                        // Catmull-Rom 关键帧，取 post 值
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

    private static void addKey(Animation.AnimationChannel channel, String type,
                               float time, float x, float y, float z) {
        switch (type) {
            case "rotation":  channel.addRotationKey(time, x, y, z); break;
            case "position":  channel.addPositionKey(time, x, y, z); break;
            case "scale":     channel.addScaleKey(time, x, y, z);    break;
        }
    }
}
