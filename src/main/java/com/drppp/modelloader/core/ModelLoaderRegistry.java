package com.drppp.modelloader.core;


import com.drppp.modelloader.api.IModelLoader;
import com.drppp.modelloader.api.ModelLoadException;
import com.drppp.modelloader.api.model.UnifiedModel;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模型加载器注册中心（单例）
 * 
 * 职责：
 *   1. 管理所有已注册的 IModelLoader
 *   2. 根据资源路径自动选择合适的加载器
 *   3. 提供模型缓存，避免重复加载
 * 
 * 使用方式：
 *   ModelLoaderRegistry.getInstance().register(new ObjModelLoader());
 *   UnifiedModel model = ModelLoaderRegistry.getInstance().load(resourceLocation);
 */
public class ModelLoaderRegistry {

    private static final Logger LOGGER = LogManager.getLogger("ModelLoader");
    private static final ModelLoaderRegistry INSTANCE = new ModelLoaderRegistry();

    /** 已注册的加载器列表 */
    private final List<IModelLoader> loaders = new ArrayList<>();

    /** 扩展名 -> 加载器 的快速索引 */
    private final Map<String, List<IModelLoader>> extensionIndex = new HashMap<>();

    /** 模型缓存：ResourceLocation -> UnifiedModel */
    private final Map<ResourceLocation, UnifiedModel> cache = new ConcurrentHashMap<>();

    /** 缓存是否启用 */
    private boolean cacheEnabled = true;

    private ModelLoaderRegistry() {}

    public static ModelLoaderRegistry getInstance() {
        return INSTANCE;
    }

    // ========================= 注册 =========================

    /**
     * 注册一个模型加载器
     * @param loader 加载器实例
     */
    public void register(IModelLoader loader) {
        loaders.add(loader);
        // 按优先级排序
        loaders.sort(Comparator.comparingInt(IModelLoader::getPriority));

        // 更新扩展名索引
        for (String ext : loader.getSupportedExtensions()) {
            String key = ext.toLowerCase(Locale.ROOT);
            extensionIndex.computeIfAbsent(key, k -> new ArrayList<>()).add(loader);
            extensionIndex.get(key).sort(Comparator.comparingInt(IModelLoader::getPriority));
        }

        LOGGER.info("[ModelLoader] Registered loader: {} (priority={}, extensions={})",
                loader.getName(), loader.getPriority(), Arrays.toString(loader.getSupportedExtensions()));
    }

    /**
     * 注销一个加载器
     */
    public void unregister(IModelLoader loader) {
        loaders.remove(loader);
        for (String ext : loader.getSupportedExtensions()) {
            List<IModelLoader> list = extensionIndex.get(ext.toLowerCase(Locale.ROOT));
            if (list != null) list.remove(loader);
        }
        LOGGER.info("[ModelLoader] Unregistered loader: {}", loader.getName());
    }

    // ========================= 加载 =========================

    /**
     * 加载模型（优先使用缓存）
     * @param resource 资源路径
     * @return 统一模型
     * @throws ModelLoadException 加载失败
     */
    public UnifiedModel load(ResourceLocation resource) throws ModelLoadException {
        // 检查缓存
        if (cacheEnabled) {
            UnifiedModel cached = cache.get(resource);
            if (cached != null) {
                LOGGER.debug("[ModelLoader] Cache hit: {}", resource);
                return cached;
            }
        }

        // 查找加载器
        IModelLoader loader = findLoader(resource);
        if (loader == null) {
            throw new ModelLoadException("No suitable loader found for: " + resource
                    + " (registered loaders: " + loaders.size() + ")");
        }

        LOGGER.info("[ModelLoader] Loading {} with {}", resource, loader.getName());
        long startTime = System.currentTimeMillis();

        UnifiedModel model = loader.load(resource);

        long elapsed = System.currentTimeMillis() - startTime;
        LOGGER.info("[ModelLoader] Loaded {} in {}ms (groups={}, materials={})",
                resource, elapsed, model.getMeshGroupNames().size(), model.getMaterials().size());

        // 存入缓存
        if (cacheEnabled) {
            cache.put(resource, model);
        }

        return model;
    }

    /**
     * 强制重新加载（忽略缓存）
     */
    public UnifiedModel reload(ResourceLocation resource) throws ModelLoadException {
        cache.remove(resource);
        return load(resource);
    }

    /**
     * 查找能处理该资源的加载器
     */
    public IModelLoader findLoader(ResourceLocation resource) {
        // 先通过扩展名快速匹配
        String path = resource.getPath();
        int dotIndex = path.lastIndexOf('.');
        if (dotIndex >= 0) {
            String ext = path.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
            List<IModelLoader> candidates = extensionIndex.get(ext);
            if (candidates != null) {
                for (IModelLoader loader : candidates) {
                    if (loader.canLoad(resource)) {
                        return loader;
                    }
                }
            }

            // 特殊处理：检查双扩展名（如 .geo.json）
            int secondDot = path.lastIndexOf('.', dotIndex - 1);
            if (secondDot >= 0) {
                String doubleExt = path.substring(secondDot + 1).toLowerCase(Locale.ROOT);
                List<IModelLoader> doubleCandidates = extensionIndex.get(doubleExt);
                if (doubleCandidates != null) {
                    for (IModelLoader loader : doubleCandidates) {
                        if (loader.canLoad(resource)) {
                            return loader;
                        }
                    }
                }
            }
        }

        // 最后逐一询问所有加载器
        for (IModelLoader loader : loaders) {
            if (loader.canLoad(resource)) {
                return loader;
            }
        }

        return null;
    }

    // ========================= 缓存管理 =========================

    public void clearCache() {
        cache.clear();
        LOGGER.info("[ModelLoader] Cache cleared");
    }

    public void evict(ResourceLocation resource) {
        cache.remove(resource);
    }

    public int getCacheSize() {
        return cache.size();
    }

    public boolean isCacheEnabled() { return cacheEnabled; }
    public void setCacheEnabled(boolean enabled) { this.cacheEnabled = enabled; }

    // ========================= 查询 =========================

    public List<IModelLoader> getRegisteredLoaders() {
        return Collections.unmodifiableList(loaders);
    }

    /**
     * 获取所有已注册的、受支持的文件扩展名
     */
    public Set<String> getSupportedExtensions() {
        return Collections.unmodifiableSet(extensionIndex.keySet());
    }
}
