package com.drppp.modelloader.example.block;

import com.drppp.modelloader.Tags;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

/**
 * B3D 方块占位示例（暂无实际模型文件，先注册空壳避免崩溃）
 */
public class B3dBlockExample {

    public static class BlockWindmill extends Block implements ITileEntityProvider {
        public BlockWindmill() {
            super(Material.WOOD);
            setRegistryName(Tags.MOD_ID, "windmill");
            setTranslationKey(Tags.MOD_ID + ".windmill");
        }

        @Override
        public TileEntity createNewTileEntity(World world, int meta) {
            return new TileWindmill();
        }

        @Override public boolean isOpaqueCube(IBlockState state) { return false; }
        @Override public boolean isFullCube(IBlockState state) { return false; }
        @Override public EnumBlockRenderType getRenderType(IBlockState state) {
            return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
        }
    }

    public static class TileWindmill extends TileEntity {
    }

    public static class TESRWindmill extends TileEntitySpecialRenderer<TileWindmill> {
        @Override
        public void render(TileWindmill te, double x, double y, double z,
                           float partialTicks, int destroyStage, float alpha) {
            // 暂空 — 等有 B3D 模型文件时再实现
        }
    }
}
