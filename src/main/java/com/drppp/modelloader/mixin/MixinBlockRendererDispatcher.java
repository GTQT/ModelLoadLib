package com.drppp.modelloader.mixin;


import com.drppp.modelloader.multiblock.HiddenBlockManager;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 拦截方块渲染
 * 当方块位置在 HiddenRegionManager 的隐藏区域内时，跳过渲染
 */
@Mixin(BlockRendererDispatcher.class)
public abstract class MixinBlockRendererDispatcher  {

    @Inject(method = "renderBlock", at = @At("HEAD"), cancellable = true)
    private void modelloader$onRenderBlock(IBlockState state, BlockPos pos,
                                           IBlockAccess world, BufferBuilder buffer,
                                           CallbackInfoReturnable<Boolean> cir) {
        if (HiddenBlockManager.getInstance().isHidden(pos)) {
            cir.setReturnValue(false);
        }
    }
}