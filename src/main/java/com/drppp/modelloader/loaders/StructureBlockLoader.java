package com.drppp.modelloader.loaders;

import com.drppp.modelloader.api.model.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

/**
 * 原版方块结构加载器
 *
 * 将世界中一个区域的方块转换为可渲染的数据结构。
 * 不是 IModelLoader（因为数据来源是世界而不是文件），
 * 而是直接提供方块快照功能。
 *
 * 两种用法：
 *
 * 用法 A — 方块快照（推荐，高性能）：
 *   保存方块的 IBlockState + IBakedModel 引用，
 *   渲染时用 StructureRenderer 逐方块重绘。
 *   优点：完美保留着色、光照、连接纹理。
 *
 *   BlockStructure structure = StructureBlockLoader.capture(world, pos1, pos2);
 *   StructureRenderer renderer = new StructureRenderer();
 *   renderer.render(structure, viewX, viewY, viewZ, partialTicks);
 *
 * 用法 B — 转为 UnifiedModel（可组合，支持 VBO）：
 *   把方块的 BakedQuad 转为三角面，生成 UnifiedModel。
 *   可以用 VBOModelRenderer 渲染，但失去动态着色。
 *
 *   UnifiedModel model = StructureBlockLoader.captureAsModel(world, pos1, pos2);
 *   VBOModelRenderer vbo = new VBOModelRenderer();
 *   vbo.upload(model);
 */
public class StructureBlockLoader {

    /**
     * 捕获方块快照（推荐方式）
     *
     * @param world   世界
     * @param corner1 区域对角1
     * @param corner2 区域对角2
     * @return 方块结构数据
     */
    public static BlockStructure capture(World world, BlockPos corner1, BlockPos corner2) {
        BlockPos min = new BlockPos(
                Math.min(corner1.getX(), corner2.getX()),
                Math.min(corner1.getY(), corner2.getY()),
                Math.min(corner1.getZ(), corner2.getZ()));
        BlockPos max = new BlockPos(
                Math.max(corner1.getX(), corner2.getX()),
                Math.max(corner1.getY(), corner2.getY()),
                Math.max(corner1.getZ(), corner2.getZ()));
        BlockPos center = new BlockPos(
                (min.getX() + max.getX()) / 2,
                (min.getY() + max.getY()) / 2,
                (min.getZ() + max.getZ()) / 2);

        BlockStructure structure = new BlockStructure(min, max, center);

        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    IBlockState state = world.getBlockState(pos);
                    if (state.getBlock() != Blocks.AIR) {
                        BlockPos relative = pos.subtract(center);
                        int light = world.getCombinedLight(pos, 0);
                        structure.addBlock(relative, pos, state, light);
                    }
                }
            }
        }

        return structure;
    }

    /**
     * 捕获并转为 UnifiedModel（可用 VBO 渲染，但失去动态着色）
     */
    public static UnifiedModel captureAsModel(World world, BlockPos corner1, BlockPos corner2) {
        BlockStructure structure = capture(world, corner1, corner2);
        return structureToModel(structure);
    }

    /**
     * 将方块结构转为 UnifiedModel
     * 每个方块一个 MeshGroup，BakedQuad 转三角面
     */
    private static UnifiedModel structureToModel(BlockStructure structure) {
        UnifiedModel model = new UnifiedModel("block_structure", "structure");
        BlockRendererDispatcher dispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();

        int blockIndex = 0;
        for (BlockStructure.BlockEntry entry : structure.getBlocks()) {
            IBlockState state = entry.state;
            BlockPos relPos = entry.relativePos;

            IBakedModel bakedModel = dispatcher.getModelForState(state);
            if (bakedModel == null) continue;

            MeshGroup group = new MeshGroup("block_" + blockIndex);
            Mesh mesh = new Mesh();

            // 收集所有面的 BakedQuad
            List<BakedQuad> allQuads = new ArrayList<>();
            for (EnumFacing facing : EnumFacing.values()) {
                allQuads.addAll(bakedModel.getQuads(state, facing, 0));
            }
            allQuads.addAll(bakedModel.getQuads(state, null, 0));

            // 将 BakedQuad 转为 Vertex + Face
            for (BakedQuad quad : allQuads) {
                int baseIdx = mesh.getVertexCount();
                int[] vertexData = quad.getVertexData();

                // 每个 BakedQuad 有 4 个顶点，每顶点 7 个 int (x,y,z,color,u,v,normal)
                for (int v = 0; v < 4; v++) {
                    int offset = v * 7;
                    float vx = Float.intBitsToFloat(vertexData[offset]) + relPos.getX();
                    float vy = Float.intBitsToFloat(vertexData[offset + 1]) + relPos.getY();
                    float vz = Float.intBitsToFloat(vertexData[offset + 2]) + relPos.getZ();

                    int color = vertexData[offset + 3];
                    float cr = ((color >> 16) & 0xFF) / 255f;
                    float cg = ((color >> 8) & 0xFF) / 255f;
                    float cb = (color & 0xFF) / 255f;
                    float ca = ((color >> 24) & 0xFF) / 255f;

                    float vu = Float.intBitsToFloat(vertexData[offset + 4]);
                    float vv = Float.intBitsToFloat(vertexData[offset + 5]);

                    int packedNormal = vertexData[offset + 6];
                    float nx = (byte) (packedNormal & 0xFF) / 127f;
                    float ny = (byte) ((packedNormal >> 8) & 0xFF) / 127f;
                    float nz = (byte) ((packedNormal >> 16) & 0xFF) / 127f;

                    Vertex vertex = new Vertex(vx, vy, vz)
                            .setUV(vu, vv)
                            .setNormal(nx, ny, nz)
                            .setColor(cr, cg, cb, ca);
                    mesh.addVertex(vertex);
                }

                // 四边面 → 两个三角面
                mesh.addQuadAsTris(baseIdx, baseIdx + 1, baseIdx + 2, baseIdx + 3);
            }

            if (mesh.getFaceCount() > 0) {
                group.addMesh(mesh);
                model.addMeshGroup(group);
            }

            blockIndex++;
        }

        model.computeBoundingBox();
        return model;
    }

    /**
     * 方块结构数据
     */
    public static class BlockStructure {

        private final BlockPos min, max, center;
        private final List<BlockEntry> blocks = new ArrayList<>();

        public BlockStructure(BlockPos min, BlockPos max, BlockPos center) {
            this.min = min;
            this.max = max;
            this.center = center;
        }

        public void addBlock(BlockPos relativePos, BlockPos worldPos, IBlockState state, int light) {
            blocks.add(new BlockEntry(relativePos, worldPos, state, light));
        }

        public BlockPos getMin() { return min; }
        public BlockPos getMax() { return max; }
        public BlockPos getCenter() { return center; }
        public List<BlockEntry> getBlocks() { return Collections.unmodifiableList(blocks); }
        public int getBlockCount() { return blocks.size(); }

        /**
         * 单个方块的数据
         */
        public static class BlockEntry {
            public final BlockPos relativePos;  // 相对区域中心的偏移
            public final BlockPos worldPos;     // 原始世界坐标（用于光照/着色查询）
            public final IBlockState state;     // 方块状态
            public final int packedLight;       // 捕获时的光照值

            public BlockEntry(BlockPos relativePos, BlockPos worldPos, IBlockState state, int packedLight) {
                this.relativePos = relativePos;
                this.worldPos = worldPos;
                this.state = state;
                this.packedLight = packedLight;
            }
        }
    }
}
