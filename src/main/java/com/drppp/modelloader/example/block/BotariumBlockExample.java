package com.drppp.modelloader.example.block;

import com.drppp.modelloader.Tags;
import com.drppp.modelloader.anim.AnimationController;
import com.drppp.modelloader.api.ModelLoadException;
import com.drppp.modelloader.api.model.Animation;
import com.drppp.modelloader.api.model.UnifiedModel;
import com.drppp.modelloader.core.ModelLoaderRegistry;
import com.drppp.modelloader.loaders.GeckoLibAnimationLoader;
import com.drppp.modelloader.render.EnhancedModelRenderer;
import com.drppp.modelloader.util.ModelUtils;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.ITickable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import java.util.Map;

/**
 * Botarium 方块示例
 *
 * 模型: botarium.geo.json (24 bones, 128×128 纹理)
 * 动画: Botarium.anim.deploy (3秒展开动画) + Botarium.anim.idle (1秒循环)
 *
 * 行为: 放置时播放 deploy 展开动画，结束后切换到 idle 循环
 */
public class BotariumBlockExample {

    // ========================= Block =========================

    public static class BlockBotarium extends Block implements ITileEntityProvider {

        public BlockBotarium() {
            super(Material.IRON);
            setRegistryName(Tags.MOD_ID, "botarium");
            setTranslationKey(Tags.MOD_ID + ".botarium");
            setCreativeTab(CreativeTabs.MISC);
            setHardness(3.0f);
        }

        @Override
        public TileEntity createNewTileEntity(World world, int meta) {
            return new TileBotarium();
        }

        @Override public boolean isOpaqueCube(IBlockState s) { return false; }
        @Override public boolean isFullCube(IBlockState s) { return false; }

        @Override
        public EnumBlockRenderType getRenderType(IBlockState state) {
            return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
        }
    }

    // ========================= TileEntity =========================

    public static class TileBotarium extends TileEntity implements ITickable {

        /** 是否已播放过 deploy 动画 */
        private boolean deployed = false;

        /** 放置后的 tick 计数（用于触发动画） */
        private int ticksExisted = 0;

        public boolean isDeployed() { return deployed; }

        public void setDeployed(boolean deployed) {
            this.deployed = deployed;
            markDirty();
        }

        @Override
        public void update() {
            ticksExisted++;
            // 放置后 2 tick 标记为已部署（给渲染器时间初始化）
            if (!deployed && ticksExisted > 2) {
                deployed = true;
                markDirty();
            }
        }

        @Override
        public NBTTagCompound writeToNBT(NBTTagCompound tag) {
            super.writeToNBT(tag);
            tag.setBoolean("deployed", deployed);
            return tag;
        }

        @Override
        public void readFromNBT(NBTTagCompound tag) {
            super.readFromNBT(tag);
            deployed = tag.getBoolean("deployed");
        }
    }

    // ========================= TESR =========================

    public static class TESRBotarium extends TileEntitySpecialRenderer<TileBotarium> {

        // geo 模型路径
        private static final ResourceLocation GEO_MODEL =
                new ResourceLocation(Tags.MOD_ID, "models/geo/botarium.geo.json");

        // 动画文件路径
        private static final ResourceLocation ANIM_FILE =
                new ResourceLocation(Tags.MOD_ID, "animations/botarium.animation.json");

        // 纹理路径
        private static final ResourceLocation TEXTURE =
                new ResourceLocation(Tags.MOD_ID, "textures/block/botarium.png");

        // 动画名称
        private static final String ANIM_DEPLOY = "Botarium.anim.deploy";
        private static final String ANIM_IDLE = "Botarium.anim.idle";

        private EnhancedModelRenderer renderer;
        private boolean initialized = false;
        private boolean loadFailed = false;
        private long lastRenderTime = 0;

        /** 记录每个 TileEntity 是否已经播放过 deploy */
        private final java.util.Set<net.minecraft.util.math.BlockPos> deployedPositions
                = new java.util.HashSet<>();

        private void init() {
            if (initialized || loadFailed) return;
            try {
                // ① 加载 geo 模型
                UnifiedModel model = ModelLoaderRegistry.getInstance().load(GEO_MODEL);

                // ② 加载动画文件
                Map<String, Animation> animations =
                        GeckoLibAnimationLoader.loadAnimations(ANIM_FILE);
                for (Animation anim : animations.values()) {
                    model.addAnimation(anim);
                }

                // ③ 打印调试信息
                System.out.println("[Botarium] ===== MODEL DEBUG =====");
                System.out.println(ModelUtils.getDebugInfo(model));
                System.out.println("[Botarium] =======================");

                // ④ 初始化渲染器
                renderer = new EnhancedModelRenderer();
                renderer.setModel(model);
                renderer.setDefaultTexture(TEXTURE);

                // geo.json 的坐标是像素单位 (0~16)，需要 1/16 缩放
                renderer.setGlobalScale(1f / 16f);

                // ⑤ 标记 glass 为半透明 + 双面渲染
                renderer.setGroupTranslucent("glass", true);
                renderer.setGroupDoubleSided("glass", true);

                // screen 也是薄片，需要双面
                renderer.setGroupDoubleSided("screen", true);

                // 默认播放 idle
                renderer.getAnimController().play(ANIM_IDLE, true);

                initialized = true;
                lastRenderTime = System.currentTimeMillis();

                System.out.println("[Botarium] Loaded successfully! Animations: "
                        + model.getAnimations().keySet());

            } catch (ModelLoadException e) {
                System.err.println("[Botarium] FAILED: " + e.getMessage());
                e.printStackTrace();
                loadFailed = true;
            }
        }

        @Override
        public void render(TileBotarium te, double x, double y, double z,
                           float partialTicks, int destroyStage, float alpha) {
            if (!initialized && !loadFailed) init();
            if (renderer == null) return;

            // 计算 deltaTime
            long now = System.currentTimeMillis();
            float dt = Math.min((now - lastRenderTime) / 1000f, 0.1f);
            lastRenderTime = now;

            AnimationController ctrl = renderer.getAnimController();

            // 动画状态管理
            net.minecraft.util.math.BlockPos pos = te.getPos();
            if (te.isDeployed() && !deployedPositions.contains(pos)) {
                // 首次检测到 deployed → 播放展开动画
                ctrl.play(ANIM_DEPLOY, false);
                deployedPositions.add(pos);
            }

            // deploy 动画播放完毕后切换到 idle
            if (deployedPositions.contains(pos)
                    && !ctrl.isPlaying(ANIM_DEPLOY)
                    && !ctrl.isPlaying(ANIM_IDLE)) {
                ctrl.play(ANIM_IDLE, true);
            }

            ctrl.update(dt);

            // 渲染
            GlStateManager.pushMatrix();

            // geo.json 的 pivot 原点在 (0, 0, 0)，即方块底部中心
            // 经过 1/16 缩放后坐标系对齐方块
            GlStateManager.translate(x + 0.5, y, z + 0.5);

            // 绑定纹理
            bindTexture(TEXTURE);

            renderer.render(partialTicks);

            GlStateManager.popMatrix();
        }
    }
}
