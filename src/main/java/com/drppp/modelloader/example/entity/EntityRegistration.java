package com.drppp.modelloader.example.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 示例：如何注册使用自定义模型的实体
 *
 * 步骤：
 *   1. 在 CommonProxy.preInit 中注册实体
 *   2. 在 ClientProxy.preInit 中注册渲染器
 *
 * 注意：
 *   - 实体类本身（EntityDragon, EntityRobot等）需要你自己实现
 *   - 这里只展示注册和渲染器绑定部分
 *   - 渲染器注册必须在客户端 preInit 阶段完成
 */
public class EntityRegistration {

    // 假设这些实体类已经存在
    // public class EntityDragon extends EntityLiving { ... }
    // public class EntityRobot extends EntityLiving { ... }
    // public class EntityBullet extends Entity { ... }

    // 这里用 EntityLiving 和 Entity 作为占位符
    // 实际使用时替换为你的实体类

    /**
     * 在 CommonProxy.preInit() 中调用
     * 注册实体到 Forge 注册表
     *
     * @param modInstance 你的 @Mod 实例
     */
    public static void registerEntities(Object modInstance) {
        int entityId = 0;

        // 注册 GeckoLib 龙实体
        // EntityRegistry.registerModEntity(
        //     new ResourceLocation("mymod", "dragon"),
        //     EntityDragon.class,
        //     "dragon",
        //     entityId++,
        //     modInstance,
        //     64,     // 追踪范围
        //     3,      // 更新频率
        //     true    // 发送速度更新
        // );

        // 注册 B3D 机器人实体
        // EntityRegistry.registerModEntity(
        //     new ResourceLocation("mymod", "robot"),
        //     EntityRobot.class,
        //     "robot",
        //     entityId++,
        //     modInstance,
        //     64, 3, true
        // );

        // 注册 OBJ 投射物实体
        // EntityRegistry.registerModEntity(
        //     new ResourceLocation("mymod", "bullet"),
        //     EntityBullet.class,
        //     "bullet",
        //     entityId++,
        //     modInstance,
        //     64, 1, true
        // );
    }

    /**
     * 在 ClientProxy.preInit() 中调用
     * 绑定实体渲染器
     *
     * 重要：必须在 preInit 阶段调用，不能在 init 或 postInit！
     */
    @SideOnly(Side.CLIENT)
    public static void registerRenderers() {

        // GeckoLib 模型实体 → GeckoLibEntityRenderer
        // RenderingRegistry.registerEntityRenderingHandler(
        //     EntityDragon.class,
        //     GeckoLibEntityRenderer::new
        // );

        // B3D 模型实体 → B3dEntityRenderer
        // RenderingRegistry.registerEntityRenderingHandler(
        //     EntityRobot.class,
        //     B3dEntityRenderer::new
        // );

        // OBJ 模型实体 → ObjEntityRenderer
        // RenderingRegistry.registerEntityRenderingHandler(
        //     EntityBullet.class,
        //     ObjEntityRenderer::new
        // );

        // ======== 实际使用时取消上面的注释 ========
        // 并将 EntityDragon/EntityRobot/EntityBullet 替换为你的实体类
    }
}
