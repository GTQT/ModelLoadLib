package com.drppp.modelloader.example.block;

import com.drppp.modelloader.Tags;
import com.drppp.modelloader.api.ModelLoadException;
import com.drppp.modelloader.api.model.UnifiedModel;
import com.drppp.modelloader.core.ModelLoaderRegistry;
import com.drppp.modelloader.render.EnhancedModelRenderer;
import com.drppp.modelloader.render.VBOModelRenderer;
import com.drppp.modelloader.util.ModelUtils;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ObjBlockExample {

    // ========================= Block =========================

    public static class BlockCustomMachine extends Block implements ITileEntityProvider {

        public static final PropertyDirection FACING = PropertyDirection.create("facing",
                EnumFacing.Plane.HORIZONTAL);

        public BlockCustomMachine() {
            super(Material.IRON);
            setRegistryName(Tags.MOD_ID, "tree");
            setTranslationKey(Tags.MOD_ID + ".tree");
            setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
            setCreativeTab(CreativeTabs.MISC);
        }

        @Override
        public TileEntity createNewTileEntity(World world, int meta) {
            return new TileCustomMachine();
        }

        @Override
        public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
                                                float hitX, float hitY, float hitZ,
                                                int meta, EntityLivingBase placer, EnumHand hand) {
            return getDefaultState().withProperty(FACING,
                    placer.getHorizontalFacing().getOpposite());
        }

        @Override
        protected BlockStateContainer createBlockState() {
            return new BlockStateContainer(this, FACING);
        }

        @Override
        public int getMetaFromState(IBlockState state) {
            return state.getValue(FACING).getHorizontalIndex();
        }

        @Override
        public IBlockState getStateFromMeta(int meta) {
            return getDefaultState().withProperty(FACING,
                    EnumFacing.byHorizontalIndex(meta));
        }

        @Override
        public boolean isOpaqueCube(IBlockState state) { return false; }

        @Override
        public boolean isFullCube(IBlockState state) { return false; }

        /**
         * ★ 返回 ENTITYBLOCK_ANIMATED 让 Forge 跳过标准方块渲染
         * 所有显示完全由 TESR 负责，即使 JSON 有问题也不会显示紫黑块
         */
        @Override
        public EnumBlockRenderType getRenderType(IBlockState state) {
            return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
        }
    }

    // ========================= TileEntity =========================

    public static class TileCustomMachine extends TileEntity {
    }

    // ========================= TESR 渲染器 =========================

    public static class TESRCustomMachine extends TileEntitySpecialRenderer<TileCustomMachine> {

        private static final ResourceLocation MODEL =
                new ResourceLocation(Tags.MOD_ID, "models/obj/turbofan.obj");
        private static final ResourceLocation TEXTURE =
                new ResourceLocation(Tags.MOD_ID, "textures/block/turbofan.png");

        private UnifiedModel model;
        private VBOModelRenderer vboRenderer;
        private EnhancedModelRenderer enRenderer;
        private boolean initialized = false;
        private boolean loadFailed = false;

        private void init() {
            if (initialized || loadFailed) return;
            try {
                model = ModelLoaderRegistry.getInstance().load(MODEL);

                // ★ 不要调用 convertBlenderToMC()
                // tree.obj 来自 Blockbench，坐标系已经是 Y-up，不需要转换

                System.out.println("[TreeModel] " + ModelUtils.getDebugInfo(model));

                //vboRenderer = new VBOModelRenderer();
                //vboRenderer.upload(model);
                enRenderer=new EnhancedModelRenderer();
                enRenderer.setModel(model);
                initialized = true;

                System.out.println("[TreeModel] Loaded successfully!");
            } catch (ModelLoadException e) {
                System.err.println("[TreeModel] FAILED: " + e.getMessage());
                e.printStackTrace();
                loadFailed = true;
            }
        }

        @Override
        public void render(TileCustomMachine te, double x, double y, double z,
                           float partialTicks, int destroyStage, float alpha) {
            if (!initialized && !loadFailed) init();
            if (enRenderer == null) return;

            GlStateManager.pushMatrix();

            // tree.obj 顶点范围: X[-0.3125, 0.3125], Y[-0.375, 0.5], Z[-0.0625, 0.3125]
            // 模型已经是方块尺度，不需要 1/16 缩放
            // Y 底部在 -0.375，平移 y+0.375 让底部对齐方块底面
            GlStateManager.translate(x + 0.5, y + 0.375, z + 0.375);

            // ★ 不要缩放 1/16！模型已经是方块单位
            float scale = 10.0f;
            GlStateManager.scale(scale, scale, scale);

            // 根据方块朝向旋转
            if (te.getWorld() != null) {
                IBlockState state = te.getWorld().getBlockState(te.getPos());
                if (state.getBlock() instanceof BlockCustomMachine) {
                    EnumFacing facing = state.getValue(BlockCustomMachine.FACING);
                    GlStateManager.rotate(-facing.getHorizontalAngle(), 0, 1, 0);
                }
            }

            // 绑定纹理并渲染
            bindTexture(TEXTURE);
            enRenderer.render(partialTicks);

            GlStateManager.popMatrix();
        }
    }
}
