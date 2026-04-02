package com.drppp.modelloader;


import com.drppp.modelloader.core.ModelLoaderRegistry;
import com.drppp.modelloader.example.entity.EntityRegistration;
import com.drppp.modelloader.loaders.B3dModelLoader;
import com.drppp.modelloader.loaders.GeckoLibModelLoader;
import com.drppp.modelloader.loaders.ObjModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION)
public class ModelLoaderMain {

    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);

    @Mod.Instance(Tags.MOD_ID)
    public static ModelLoaderMain INSTANCE;

    @SidedProxy(
            clientSide = "com.drppp.modelloader.ModelLoaderMain$ClientProxy",
            serverSide = "com.drppp.modelloader.ModelLoaderMain$CommonProxy"
    )
    public static ModelLoaderMain.CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // ① 注册模型加载器（客户端和服务端都可以注册，但只在客户端使用）
        ModelLoaderRegistry registry = ModelLoaderRegistry.getInstance();
        registry.register(new ObjModelLoader());
        registry.register(new GeckoLibModelLoader());
        registry.register(new B3dModelLoader());

        // ② 注册实体
        EntityRegistration.registerEntities(INSTANCE);

        // ③ 客户端特有的注册（通过 Proxy）
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    // ========================= Proxy =========================

    public static class CommonProxy {
        public void preInit(FMLPreInitializationEvent event) {
            // 通用初始化
        }
        public void init(FMLInitializationEvent event) {}
    }

    public static class ClientProxy extends ModelLoaderMain.CommonProxy {
        @Override
        public void preInit(FMLPreInitializationEvent event) {
            super.preInit(event);

            // 注册实体渲染器（必须在 preInit！）
            EntityRegistration.registerRenderers();

            // Block 的 TESR 和 Item 的模型通过 @SubscribeEvent 自动注册
            // 见 BlockRegistration 和 ItemRegistration
        }
    }

}
