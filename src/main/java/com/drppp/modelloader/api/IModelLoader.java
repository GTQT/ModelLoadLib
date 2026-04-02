package com.drppp.modelloader.api;


import com.drppp.modelloader.api.model.UnifiedModel;
import net.minecraft.util.ResourceLocation;

/**
 * 通用模型加载器接口
 * 所有格式的加载器都需要实现此接口
 */
public interface IModelLoader {

    /**
     * 获取加载器支持的文件扩展名
     * @return 支持的扩展名数组，例如 {"obj"}, {"geo.json", "json"}, {"b3d"}
     */
    String[] getSupportedExtensions();

    /**
     * 判断该加载器是否能处理指定的资源
     * @param resource 资源路径
     * @return 是否可以加载
     */
    boolean canLoad(ResourceLocation resource);

    /**
     * 加载模型并转换为统一格式
     * @param resource 资源路径
     * @return 统一模型对象
     * @throws ModelLoadException 加载失败时抛出
     */
    UnifiedModel load(ResourceLocation resource) throws ModelLoadException;

    /**
     * 获取加载器的优先级，数值越小优先级越高
     * 当多个加载器都能处理同一资源时，选择优先级最高的
     * @return 优先级
     */
    default int getPriority() {
        return 100;
    }

    /**
     * 获取加载器名称（用于日志和调试）
     * @return 加载器名称
     */
    String getName();
}
