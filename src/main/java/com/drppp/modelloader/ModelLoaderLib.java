package com.drppp.modelloader;

import com.drppp.modelloader.core.ModelLoaderRegistry;
import com.drppp.modelloader.loaders.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * ModelLoaderLib — 通用模型加载库
 *
 * 作为前置 Mod 供其他 Mod 引用，提供统一的模型加载和渲染 API。
 *
 * 支持的格式：
 *   1. WaveFront OBJ (.obj + .mtl)
 *   2. Blitz3D (.b3d)
 *   3. Bedrock Geometry (.geo.json) — 不需要 GeckoLib
 *   4. Bedrock Animation (.animation.json)
 *   5. 原版方块结构快照
 *
 * 其他 Mod 的用法：
 *   // build.gradle 添加依赖后直接使用
 *   UnifiedModel model = ModelLoaderRegistry.getInstance().load(
 *       new ResourceLocation("mymod", "models/obj/something.obj"));
 */
@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION)
public class ModelLoaderLib {

    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);

    @Mod.Instance(Tags.MOD_ID)
    public static ModelLoaderLib INSTANCE;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // 自动注册所有内置加载器
        ModelLoaderRegistry registry = ModelLoaderRegistry.getInstance();
        registry.register(new ObjModelLoader());
        registry.register(new B3dModelLoader());
        registry.register(new GeckoLibModelLoader());

        LOGGER.info("ModelLoaderLib initialized. Supported formats: {}",
                registry.getSupportedExtensions());

    }
}
