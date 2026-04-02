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
 * GeckoLib 方块占位示例（暂无实际模型文件，先注册空壳避免崩溃）
 */
public class GeckoLibBlockExample {

    public static class BlockAnimatedChest extends Block implements ITileEntityProvider {
        public BlockAnimatedChest() {
            super(Material.WOOD);
            setRegistryName(Tags.MOD_ID, "animated_chest");
            setTranslationKey(Tags.MOD_ID + ".animated_chest");
        }

        @Override
        public TileEntity createNewTileEntity(World world, int meta) {
            return new TileAnimatedChest();
        }

        @Override public boolean isOpaqueCube(IBlockState state) { return false; }
        @Override public boolean isFullCube(IBlockState state) { return false; }
        @Override public EnumBlockRenderType getRenderType(IBlockState state) {
            return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
        }
    }

    public static class TileAnimatedChest extends TileEntity {
    }

    public static class TESRAnimatedChest extends TileEntitySpecialRenderer<TileAnimatedChest> {
        @Override
        public void render(TileAnimatedChest te, double x, double y, double z,
                           float partialTicks, int destroyStage, float alpha) {
            // 暂空 — 等有 GeckoLib 模型文件时再实现
        }
    }
}
