package com.drppp.modelloader.multiblock;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;

import java.util.*;

/**
 * 隐藏方块管理器
 *
 * 记录哪些方块位置需要跳过渲染。
 * 由 Mixin 在 renderBlock() 中查询。
 *
 * 用法：
 *   // 隐藏某区域
 *   HiddenBlockManager.getInstance().hideRegion("my_id", pos1, pos2);
 *
 *   // 恢复
 *   HiddenBlockManager.getInstance().showRegion("my_id");
 */
public class HiddenBlockManager {

    private static final HiddenBlockManager INSTANCE = new HiddenBlockManager();
    public static HiddenBlockManager getInstance() { return INSTANCE; }

    /** 被隐藏的位置 → 所属 region id */
    private final Map<BlockPos, String> hiddenPositions = new HashMap<>();

    /** 区域范围记录（用于刷新区块） */
    private final Map<String, BlockPos[]> regionBounds = new HashMap<>();

    private HiddenBlockManager() {}

    // ========================= 区域操作 =========================

    /**
     * 隐藏一个区域内的所有方块
     *
     * @param regionId 区域唯一标识
     * @param min      区域最小角
     * @param max      区域最大角
     */
    public void hideRegion(String regionId, BlockPos min, BlockPos max) {
        // 先移除旧的同名区域
        showRegion(regionId);

        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    hiddenPositions.put(new BlockPos(x, y, z), regionId);
                }
            }
        }

        regionBounds.put(regionId, new BlockPos[]{min, max});
        markForRerender(min, max);
    }

    /**
     * 恢复一个区域，方块重新显示
     *
     * @param regionId 区域标识
     */
    public void showRegion(String regionId) {
        hiddenPositions.entrySet().removeIf(e -> regionId.equals(e.getValue()));

        BlockPos[] bounds = regionBounds.remove(regionId);
        if (bounds != null) {
            markForRerender(bounds[0], bounds[1]);
        }
    }

    /**
     * 隐藏单个方块
     */
    public void hideBlock(BlockPos pos) {
        hiddenPositions.put(pos, "__single__");
    }

    /**
     * 恢复单个方块
     */
    public void showBlock(BlockPos pos) {
        hiddenPositions.remove(pos);
        markForRerender(pos, pos);
    }

    /**
     * 查询某位置是否被隐藏（由 Mixin 调用）
     */
    public boolean isHidden(BlockPos pos) {
        return hiddenPositions.containsKey(pos);
    }

    /**
     * 清除所有隐藏
     */
    public void clearAll() {
        // 收集所有需要刷新的区域
        Set<String> regionIds = new HashSet<>(regionBounds.keySet());
        hiddenPositions.clear();

        for (String id : regionIds) {
            BlockPos[] bounds = regionBounds.remove(id);
            if (bounds != null) markForRerender(bounds[0], bounds[1]);
        }
    }

    /**
     * 获取所有被隐藏的区域 ID
     */
    public Set<String> getHiddenRegions() {
        return Collections.unmodifiableSet(regionBounds.keySet());
    }

    /**
     * 查询某区域是否正在被隐藏
     */
    public boolean isRegionHidden(String regionId) {
        return regionBounds.containsKey(regionId);
    }

    // ========================= 内部 =========================

    private void markForRerender(BlockPos min, BlockPos max) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.renderGlobal == null) return;
            mc.renderGlobal.markBlockRangeForRenderUpdate(
                    min.getX(), min.getY(), min.getZ(),
                    max.getX(), max.getY(), max.getZ());
        } catch (Exception ignored) {}
    }
}
