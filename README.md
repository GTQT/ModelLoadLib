# ModelLoaderLib — 其他 Mod 调用指南

> ModelLoaderLib 是一个 Forge 1.12.2 的前置模型加载库  
> 其他 Mod 引用后可以加载 OBJ / B3D / Bedrock Geo / 原版方块结构  
> 不需要 GeckoLib 或任何其他前置

---

## 目录

1. [引入依赖](#1-引入依赖)
2. [API 总览](#2-api-总览)
3. [加载 OBJ 模型](#3-加载-obj-模型)
4. [加载 B3D 模型](#4-加载-b3d-模型)
5. [加载 Bedrock Geo 模型](#5-加载-bedrock-geo-模型)
6. [捕获原版方块结构](#6-捕获原版方块结构)
7. [绑定到 Block + TileEntity](#7-绑定到-block--tileentity)
8. [绑定到 Item](#8-绑定到-item)
9. [绑定到 Entity](#9-绑定到-entity)
10. [JSON 文件模板](#10-json-文件模板)
11. [坐标与缩放速查](#11-坐标与缩放速查)
12. [完整案例：从零到渲染](#12-完整案例从零到渲染)

---

## 1. 引入依赖

### build.gradle

```groovy
repositories {
    // ModelLoaderLib 的 maven 仓库（替换为你实际发布的地址）
    maven { url 'https://your-maven-repo.com' }
    // 或者使用本地 jar
    flatDir { dirs 'libs' }
}

dependencies {
    // 方式 A: maven 依赖
    implementation 'com.drppp:modelloader:1.0.0'
    
    // 方式 B: 本地 jar（把 modelloader-1.0.0.jar 放到 libs/ 目录）
    implementation files('libs/modelloader-1.0.0.jar')
}
```

### mcmod.info 声明前置

```json
{
    "modid": "mymod",
    "name": "My Mod",
    "dependencies": ["modelloader"]
}
```

### Mod 类声明依赖

```java
@Mod(modid = "mymod", dependencies = "required-after:modelloader")
public class MyMod {
    // ModelLoaderLib 会在你的 mod 之前初始化
    // 加载器已经自动注册好了，直接用就行
}
```

---

## 2. API 总览

### 核心入口

```java
import com.drppp.modelloader.core.ModelLoaderRegistry;
import com.drppp.modelloader.api.model.UnifiedModel;

// 加载任意格式的模型（自动匹配加载器）
UnifiedModel model = ModelLoaderRegistry.getInstance().load(resourceLocation);
```

### 全部公开 API

| 包          | 类                              | 用途                       |
| ----------- | ------------------------------- | -------------------------- |
| `api`       | `IModelLoader`                  | 加载器接口（扩展用）       |
| `api`       | `ModelLoadException`            | 加载异常                   |
| `api.model` | `UnifiedModel`                  | 模型根对象                 |
| `api.model` | `MeshGroup, Mesh, Vertex, Face` | 几何数据                   |
| `api.model` | `Material`                      | 材质                       |
| `api.model` | `Transform, AABB`               | 变换、包围盒               |
| `api.model` | `Skeleton, Bone, Animation`     | 骨骼动画                   |
| `core`      | `ModelLoaderRegistry`           | 注册中心（加载入口）       |
| `loaders`   | `ObjModelLoader`                | OBJ 加载                   |
| `loaders`   | `B3dModelLoader`                | B3D 加载                   |
| `loaders`   | `GeckoLibModelLoader`           | Bedrock Geo 加载           |
| `loaders`   | `GeckoLibAnimationLoader`       | Bedrock 动画加载           |
| `loaders`   | `StructureBlockLoader`          | 原版方块结构捕获           |
| `render`    | `VBOModelRenderer`              | GPU 静态渲染（高性能）     |
| `render`    | `UnifiedModelRenderer`          | Tessellator 即时渲染       |
| `render`    | `EnhancedModelRenderer`         | 增强渲染（动画+发光+透明） |
| `render`    | `StructureRenderer`             | 方块结构渲染（带动画）     |
| `anim`      | `AnimationController`           | 动画播放控制               |
| `util`      | `ModelUtils`                    | 法线计算、坐标转换、调试   |

---

## 3. 加载 OBJ 模型

### 资源文件

```
assets/mymod/
├── models/obj/
│   ├── machine.obj
│   └── machine.mtl
└── textures/block/
    └── machine.png
```

### 加载 + 渲染

```java
import com.drppp.modelloader.core.ModelLoaderRegistry;
import com.drppp.modelloader.api.model.UnifiedModel;
import com.drppp.modelloader.render.VBOModelRenderer;
import com.drppp.modelloader.util.ModelUtils;

// 加载
UnifiedModel model = ModelLoaderRegistry.getInstance().load(
    new ResourceLocation("mymod", "models/obj/machine.obj"));

// 调试（看看模型信息）
System.out.println(ModelUtils.getDebugInfo(model));

// 上传到 GPU
VBOModelRenderer vbo = new VBOModelRenderer();
vbo.upload(model);

// 渲染时
bindTexture(new ResourceLocation("mymod", "textures/block/machine.png"));
vbo.render();

// 不再使用时释放
vbo.dispose();
```

### 坐标处理

```java
// Blockbench 导出的 OBJ → 方块单位，scale = 1.0
// Blender 导出的 OBJ (Z-up) → 需要坐标转换
ModelUtils.convertBlenderToMC(model);  // 只有 Blender Z-up 导出时才调用
```

---

## 4. 加载 B3D 模型

### 资源文件

```
assets/mymod/models/entity/robot.b3d
assets/mymod/textures/entity/robot.png
```

### 加载 + 渲染（B3D 自带动画）

```java
UnifiedModel model = ModelLoaderRegistry.getInstance().load(
    new ResourceLocation("mymod", "models/entity/robot.b3d"));

// B3D 自带动画数据
if (model.hasAnimations()) {
    System.out.println("Animations: " + model.getAnimations().keySet());
}

// 用增强渲染器播放动画
EnhancedModelRenderer renderer = new EnhancedModelRenderer();
renderer.setModel(model);
renderer.setDefaultTexture(new ResourceLocation("mymod", "textures/entity/robot.png"));
renderer.setGlobalScale(1f / 16f);

// 播放动画
renderer.getAnimController().play("default", true);

// 每帧
renderer.getAnimController().update(deltaTime);
renderer.render(partialTicks);
```

---

## 5. 加载 Bedrock Geo 模型

### 资源文件

```
assets/mymod/
├── models/geo/
│   └── dragon.geo.json
├── animations/
│   └── dragon.animation.json
└── textures/entity/
    └── dragon.png
```

### 加载模型 + 动画

```java
import com.drppp.modelloader.loaders.GeckoLibAnimationLoader;
import com.drppp.modelloader.api.model.Animation;

// ① 加载几何模型
UnifiedModel model = ModelLoaderRegistry.getInstance().load(
    new ResourceLocation("mymod", "models/geo/dragon.geo.json"));

// ② 加载动画文件（独立于模型）
Map<String, Animation> anims = GeckoLibAnimationLoader.loadAnimations(
    new ResourceLocation("mymod", "animations/dragon.animation.json"));
for (Animation anim : anims.values()) {
    model.addAnimation(anim);
}

// ③ 渲染
EnhancedModelRenderer renderer = new EnhancedModelRenderer();
renderer.setModel(model);
renderer.setDefaultTexture(new ResourceLocation("mymod", "textures/entity/dragon.png"));
renderer.setGlobalScale(1f / 16f);  // geo.json 坐标是像素单位

// ④ 透明/双面组设置（如果有玻璃等部件）
renderer.setGroupTranslucent("glass", true);
renderer.setGroupDoubleSided("glass", true);
```

### 动画控制

```java
AnimationController ctrl = renderer.getAnimController();

// 播放
ctrl.play("animation.dragon.idle", true);    // 循环
ctrl.play("animation.dragon.attack", false); // 一次

// 过渡
ctrl.crossFade("animation.dragon.walk", 0.25f);  // 0.25秒过渡

// 叠加
ctrl.additive("animation.dragon.breathe", 0.5f, true);

// 速度
ctrl.setGlobalSpeed(1.5f);

// 每帧更新
ctrl.update(deltaTime);  // 秒

// 查询
ctrl.isPlaying("animation.dragon.walk");
ctrl.getPlayingAnimations();
```

### 运行时控制

```java
// 隐藏部件
renderer.hideGroup("armor");

// 头部跟随视角
renderer.setTransformOverride("head",
    new Transform().setRotation(headPitch, headYaw, 0));

// 受伤闪红
renderer.setColorOverlay(1f, 0f, 0f, 0.5f);
renderer.clearColorOverlay();
```

---

## 6. 捕获原版方块结构

### 方式 A：StructureRenderer（推荐，保留着色+光照）

```java
import com.drppp.modelloader.loaders.StructureBlockLoader;
import com.drppp.modelloader.loaders.StructureBlockLoader.BlockStructure;
import com.drppp.modelloader.render.StructureRenderer;

// 捕获区域内的方块
BlockPos corner1 = new BlockPos(10, 64, 20);
BlockPos corner2 = new BlockPos(14, 66, 24);
BlockStructure structure = StructureBlockLoader.capture(world, corner1, corner2);

System.out.println("Captured " + structure.getBlockCount() + " blocks");

// 创建渲染器
StructureRenderer renderer = new StructureRenderer();

// 设置动画
renderer.setRotation(0, angle, 0);       // Y轴旋转
renderer.setTranslation(0, liftY, 0);    // 上浮
renderer.setScale(1, 1, 1);
renderer.setAlpha(0.8f);                 // 半透明

// 渲染（在 RenderWorldLastEvent 或 TESR 中）
// viewX/Y/Z = 结构中心相对相机的偏移
renderer.render(structure, viewX, viewY, viewZ, partialTicks);
```

### 方式 B：转为 UnifiedModel（可用 VBO）

```java
// 捕获并转为模型
UnifiedModel structModel = StructureBlockLoader.captureAsModel(world, corner1, corner2);

// 可以用 VBO 高性能渲染（但失去动态着色）
VBOModelRenderer vbo = new VBOModelRenderer();
vbo.upload(structModel);
// ...
vbo.render();
```

### 方式 A vs 方式 B

|            | StructureRenderer      | captureAsModel + VBO |
| ---------- | ---------------------- | -------------------- |
| 草方块颜色 | ✅ 正确（biome 着色）   | ❌ 可能丢失           |
| 光照       | ✅ 逐方块正确           | ❌ 无光照信息         |
| 性能       | 中等（逐块 draw call） | 高（一次 draw call） |
| 适用       | 实时动画               | 静态展示             |

### 叠加自定义模型

```java
renderer.setCustomModelRenderer(() -> {
    // 坐标原点在结构中心，已施加旋转/平移/缩放
    // 在这里渲染任何 UnifiedModel
    Minecraft.getMinecraft().getTextureManager().bindTexture(MY_TEXTURE);
    myVbo.render();
});
```

---

## 7. 绑定到 Block + TileEntity

### 完整代码模板

```java
// ========== Block ==========
public class BlockMachine extends Block implements ITileEntityProvider {

    public BlockMachine() {
        super(Material.IRON);
        setRegistryName("mymod", "machine");
        setTranslationKey("mymod.machine");
        setCreativeTab(CreativeTabs.MISC);
    }

    @Override
    public TileEntity createNewTileEntity(World w, int m) {
        return new TileMachine();
    }

    // ★ 三个必须重写的方法
    @Override public boolean isOpaqueCube(IBlockState s) { return false; }
    @Override public boolean isFullCube(IBlockState s) { return false; }
    @Override public EnumBlockRenderType getRenderType(IBlockState s) {
        return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
    }
}

// ========== TileEntity ==========
public class TileMachine extends TileEntity { }

// ========== TESR ==========
public class TESRMachine extends TileEntitySpecialRenderer<TileMachine> {

    private static final ResourceLocation MODEL =
        new ResourceLocation("mymod", "models/obj/machine.obj");
    private static final ResourceLocation TEXTURE =
        new ResourceLocation("mymod", "textures/block/machine.png");

    private VBOModelRenderer vbo;
    private boolean initialized, loadFailed;

    private void init() {
        if (initialized || loadFailed) return;
        try {
            UnifiedModel model = ModelLoaderRegistry.getInstance().load(MODEL);
            vbo = new VBOModelRenderer();
            vbo.upload(model);
            initialized = true;
        } catch (ModelLoadException e) {
            e.printStackTrace();
            loadFailed = true;
        }
    }

    @Override
    public void render(TileMachine te, double x, double y, double z,
                       float pt, int ds, float a) {
        if (!initialized && !loadFailed) init();
        if (vbo == null) return;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5, y, z + 0.5);
        // OBJ from Blockbench: scale = 1.0
        // Geo .json: scale = 1/16f
        bindTexture(TEXTURE);
        vbo.render();
        GlStateManager.popMatrix();
    }
}

// ========== 注册 ==========
@Mod.EventBusSubscriber(modid = "mymod")
public class ModBlocks {

    public static final Block MACHINE = new BlockMachine();
    public static final Item MACHINE_ITEM = new ItemBlock(MACHINE)
        .setRegistryName(MACHINE.getRegistryName());

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> e) {
        e.getRegistry().register(MACHINE);
        GameRegistry.registerTileEntity(TileMachine.class,
            new ResourceLocation("mymod", "machine_te"));
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> e) {
        e.getRegistry().register(MACHINE_ITEM);
    }

    @SubscribeEvent @SideOnly(Side.CLIENT)
    public static void registerModels(ModelRegistryEvent e) {
        ClientRegistry.bindTileEntitySpecialRenderer(
            TileMachine.class, new TESRMachine());

        // ★ 物品 TEISR（不加这行物品在手里透明）
        MACHINE_ITEM.setTileEntityItemStackRenderer(new MachineItemTEISR());

        ModelLoader.setCustomModelResourceLocation(MACHINE_ITEM, 0,
            new ModelResourceLocation("mymod:machine", "inventory"));
    }
}
```

---

## 8. 绑定到 Item

### TEISR 模板

```java
public class MachineItemTEISR extends TileEntityItemStackRenderer {

    private VBOModelRenderer vbo;
    private boolean initialized, loadFailed;

    private void init() {
        if (initialized || loadFailed) return;
        try {
            UnifiedModel model = ModelLoaderRegistry.getInstance().load(
                new ResourceLocation("mymod", "models/obj/machine.obj"));
            vbo = new VBOModelRenderer();
            vbo.upload(model);
            initialized = true;
        } catch (ModelLoadException e) {
            e.printStackTrace();
            loadFailed = true;
        }
    }

    @Override
    public void renderByItem(ItemStack stack) {
        init();
        if (vbo == null) return;

        Minecraft.getMinecraft().getTextureManager().bindTexture(
            new ResourceLocation("mymod", "textures/block/machine.png"));

        GlStateManager.pushMatrix();
        GlStateManager.translate(0.5f, 0f, 0.5f);
        vbo.render();
        GlStateManager.popMatrix();
    }
}
```

### 注册

```java
// 在 registerModels 中
MACHINE_ITEM.setTileEntityItemStackRenderer(new MachineItemTEISR());
```

### 对应的 JSON

**`models/item/machine.json`** — 必须用 `builtin/entity`：

```json
{
  "parent": "builtin/entity",
  "display": {
    "gui": {
      "rotation": [30, 225, 0],
      "translation": [0, 2, 0],
      "scale": [0.625, 0.625, 0.625]
    },
    "ground": {
      "translation": [0, 2, 0],
      "scale": [0.25, 0.25, 0.25]
    },
    "thirdperson_righthand": {
      "rotation": [75, 45, 0],
      "translation": [0, 2.5, 0],
      "scale": [0.375, 0.375, 0.375]
    },
    "firstperson_righthand": {
      "rotation": [0, 45, 0],
      "translation": [0, 4, 2],
      "scale": [0.4, 0.4, 0.4]
    },
    "fixed": {
      "scale": [0.5, 0.5, 0.5]
    }
  }
}
```

---

## 9. 绑定到 Entity

### 静态模型实体（OBJ）

```java
public class RenderMyEntity<T extends Entity> extends Render<T> {

    private UnifiedModelRenderer renderer;
    private boolean initialized;

    public RenderMyEntity(RenderManager mgr) { super(mgr); }

    private void init() {
        if (initialized) return;
        try {
            UnifiedModel model = ModelLoaderRegistry.getInstance().load(
                new ResourceLocation("mymod", "models/entity/projectile.obj"));
            renderer = new UnifiedModelRenderer();
            renderer.setDefaultTexture(
                new ResourceLocation("mymod", "textures/entity/projectile.png"));
            renderer.setGlobalScale(1.0f);
            initialized = true;
        } catch (ModelLoadException e) { e.printStackTrace(); }
    }

    @Override
    public void doRender(T entity, double x, double y, double z,
                         float yaw, float pt) {
        if (!initialized) init();
        if (renderer == null) return;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(180f - yaw, 0, 1, 0);
        renderer.render(model, pt);
        GlStateManager.popMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(T e) {
        return new ResourceLocation("mymod", "textures/entity/projectile.png");
    }
}
```

### 动画实体（Geo）

```java
public class RenderDragon<T extends EntityLiving> extends Render<T> {

    private EnhancedModelRenderer renderer;
    private boolean initialized;
    private long lastTime;

    public RenderDragon(RenderManager mgr) { super(mgr); }

    private void init() {
        if (initialized) return;
        try {
            UnifiedModel model = ModelLoaderRegistry.getInstance().load(
                new ResourceLocation("mymod", "models/geo/dragon.geo.json"));

            Map<String, Animation> anims = GeckoLibAnimationLoader.loadAnimations(
                new ResourceLocation("mymod", "animations/dragon.animation.json"));
            for (Animation a : anims.values()) model.addAnimation(a);

            renderer = new EnhancedModelRenderer();
            renderer.setModel(model);
            renderer.setDefaultTexture(
                new ResourceLocation("mymod", "textures/entity/dragon.png"));
            renderer.setGlobalScale(1f / 16f);
            renderer.getAnimController().play("animation.dragon.idle", true);

            initialized = true;
            lastTime = System.currentTimeMillis();
        } catch (ModelLoadException e) { e.printStackTrace(); }
    }

    @Override
    public void doRender(T entity, double x, double y, double z,
                         float yaw, float pt) {
        if (!initialized) init();
        if (renderer == null) return;

        // 更新动画
        long now = System.currentTimeMillis();
        float dt = Math.min((now - lastTime) / 1000f, 0.1f);
        lastTime = now;

        AnimationController ctrl = renderer.getAnimController();
        ctrl.update(dt);

        // 动画状态机
        double speed = entity.motionX * entity.motionX + entity.motionZ * entity.motionZ;
        if (speed > 0.001 && !ctrl.isPlaying("animation.dragon.walk")) {
            ctrl.crossFade("animation.dragon.walk", 0.25f);
        } else if (speed <= 0.001 && !ctrl.isPlaying("animation.dragon.idle")) {
            ctrl.crossFade("animation.dragon.idle", 0.3f);
        }

        // 受伤闪红
        if (entity.hurtTime > 0) {
            renderer.setColorOverlay(1, 0, 0, 0.5f);
        } else {
            renderer.clearColorOverlay();
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(180f - yaw, 0, 1, 0);
        renderer.render(pt);
        GlStateManager.popMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(T e) {
        return new ResourceLocation("mymod", "textures/entity/dragon.png");
    }
}
```

### 注册（必须在 ClientProxy.preInit）

```java
// ClientProxy.preInit() 中
RenderingRegistry.registerEntityRenderingHandler(EntityDragon.class, RenderDragon::new);
```

---

## 10. JSON 文件模板

每个使用自定义模型的方块都需要 3 个 JSON。

### blockstates（无属性）

```json
{ "variants": { "normal": { "model": "mymod:machine" } } }
```

### blockstates（有朝向）

```json
{
  "variants": {
    "facing=north": { "model": "mymod:machine" },
    "facing=south": { "model": "mymod:machine", "y": 180 },
    "facing=west":  { "model": "mymod:machine", "y": 270 },
    "facing=east":  { "model": "mymod:machine", "y": 90 }
  }
}
```

> ⚠️ model 写 `"mymod:machine"` 不是 `"mymod:block/machine"`。Forge 自动加 `models/block/` 前缀。

### models/block（空模型）

```json
{ "textures": { "particle": "mymod:block/machine" }, "elements": [] }
```

### models/item（TEISR 物品）

```json
{ "parent": "builtin/entity", "display": { ... } }
```

> ⚠️ 必须是 `"builtin/entity"`。写成 `"mymod:block/machine"` 物品会透明。

---

## 11. 坐标与缩放速查

### 判断方法

```java
UnifiedModel model = ModelLoaderRegistry.getInstance().load(MODEL);
System.out.println(ModelUtils.getDebugInfo(model));
// 看 BoundingBox 输出
```

### 速查表

| 模型来源               | 坐标范围   | `scale`    | 需要坐标转换                      |
| ---------------------- | ---------- | ---------- | --------------------------------- |
| Blockbench → OBJ       | -0.5 ~ 0.5 | `1.0f`     | 不需要                            |
| Blockbench → .geo.json | 0 ~ 16     | `1f / 16f` | 不需要                            |
| Blender → OBJ (Z-up)   | 看模型     | 手动算     | `ModelUtils.convertBlenderToMC()` |
| B3D                    | 看模型     | 手动算     | 不需要                            |
| 方块结构               | 方块单位   | `1.0f`     | 不需要                            |

### TESR 定位公式

```java
// (x, y, z) 是方块左下前角的渲染坐标
model.computeBoundingBox();
AABB box = model.getBoundingBox();

GlStateManager.translate(
    x + 0.5,         // X 居中
    y - box.minY,     // Y 底部对齐
    z + 0.5           // Z 居中
);
```

---

## 12. 完整案例：从零到渲染

以下是一个完整的 Mod，加载一个 OBJ 方块 + Geo 实体 + 方块结构旋转。

### 12.1 资源文件结构

```
assets/mymod/
├── blockstates/
│   └── my_block.json
├── models/
│   ├── block/my_block.json
│   ├── item/my_block.json
│   ├── obj/my_block.obj
│   ├── obj/my_block.mtl
│   └── geo/my_entity.geo.json
├── animations/
│   └── my_entity.animation.json
└── textures/
    ├── block/my_block.png
    └── entity/my_entity.png
```

### 12.2 Mod 主类

```java
@Mod(modid = "mymod", name = "My Mod", version = "1.0",
     dependencies = "required-after:modelloader")
public class MyMod {

    @SidedProxy(clientSide = "com.example.mymod.ClientProxy",
                serverSide = "com.example.mymod.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit();
    }
}

public class CommonProxy {
    public void preInit() {}
}

public class ClientProxy extends CommonProxy {
    @Override
    public void preInit() {
        // Entity 渲染器必须在 preInit 注册
        RenderingRegistry.registerEntityRenderingHandler(
            MyEntity.class, RenderMyEntity::new);
    }
}
```

### 12.3 OBJ 方块（完整）

```java
// Block
public class MyBlock extends Block implements ITileEntityProvider {
    public MyBlock() {
        super(Material.IRON);
        setRegistryName("mymod", "my_block");
    }
    public TileEntity createNewTileEntity(World w, int m) { return new MyTile(); }
    public boolean isOpaqueCube(IBlockState s) { return false; }
    public boolean isFullCube(IBlockState s) { return false; }
    public EnumBlockRenderType getRenderType(IBlockState s) {
        return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
    }
}

// TileEntity
public class MyTile extends TileEntity {}

// TESR
public class MyTESR extends TileEntitySpecialRenderer<MyTile> {
    private VBOModelRenderer vbo;
    private boolean init;

    @Override
    public void render(MyTile te, double x, double y, double z,
                       float pt, int ds, float a) {
        if (!init) {
            try {
                UnifiedModel m = ModelLoaderRegistry.getInstance().load(
                    new ResourceLocation("mymod", "models/obj/my_block.obj"));
                vbo = new VBOModelRenderer();
                vbo.upload(m);
            } catch (Exception e) { e.printStackTrace(); }
            init = true;
        }
        if (vbo == null) return;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5, y, z + 0.5);
        bindTexture(new ResourceLocation("mymod", "textures/block/my_block.png"));
        vbo.render();
        GlStateManager.popMatrix();
    }
}

// TEISR
public class MyItemTEISR extends TileEntityItemStackRenderer {
    private VBOModelRenderer vbo;
    private boolean init;

    @Override
    public void renderByItem(ItemStack stack) {
        if (!init) {
            try {
                UnifiedModel m = ModelLoaderRegistry.getInstance().load(
                    new ResourceLocation("mymod", "models/obj/my_block.obj"));
                vbo = new VBOModelRenderer();
                vbo.upload(m);
            } catch (Exception e) { e.printStackTrace(); }
            init = true;
        }
        if (vbo == null) return;

        Minecraft.getMinecraft().getTextureManager().bindTexture(
            new ResourceLocation("mymod", "textures/block/my_block.png"));
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.5f, 0f, 0.5f);
        vbo.render();
        GlStateManager.popMatrix();
    }
}

// 注册
@Mod.EventBusSubscriber(modid = "mymod")
public class ModBlocks {
    public static final Block MY_BLOCK = new MyBlock();
    public static final Item MY_ITEM = new ItemBlock(MY_BLOCK)
        .setRegistryName(MY_BLOCK.getRegistryName());

    @SubscribeEvent
    public static void blocks(RegistryEvent.Register<Block> e) {
        e.getRegistry().register(MY_BLOCK);
        GameRegistry.registerTileEntity(MyTile.class,
            new ResourceLocation("mymod", "my_tile"));
    }

    @SubscribeEvent
    public static void items(RegistryEvent.Register<Item> e) {
        e.getRegistry().register(MY_ITEM);
    }

    @SubscribeEvent @SideOnly(Side.CLIENT)
    public static void models(ModelRegistryEvent e) {
        ClientRegistry.bindTileEntitySpecialRenderer(MyTile.class, new MyTESR());
        MY_ITEM.setTileEntityItemStackRenderer(new MyItemTEISR());
        ModelLoader.setCustomModelResourceLocation(MY_ITEM, 0,
            new ModelResourceLocation("mymod:my_block", "inventory"));
    }
}
```

### 12.4 方块结构旋转（在 TESR 中）

```java
// 在某个 TESR 中捕获并旋转周围方块
public class MyMultiblockTESR extends TileEntitySpecialRenderer<MyMultiblockTile> {

    private StructureRenderer structRenderer;
    private BlockStructure structure;
    private boolean captured;
    private float angle;

    @Override
    public void render(MyMultiblockTile te, double x, double y, double z,
                       float pt, int ds, float a) {
        if (!captured && te.isActive()) {
            // 捕获周围 5×3×5 方块
            BlockPos p = te.getPos();
            structure = StructureBlockLoader.capture(te.getWorld(),
                p.add(-2, -1, -2), p.add(2, 1, 2));
            structRenderer = new StructureRenderer();
            captured = true;
        }

        if (structure != null && structRenderer != null && te.isActive()) {
            angle += 2f;
            if (angle >= 360) angle -= 360;
            structRenderer.setRotation(0, angle, 0);

            // 相对当前 TE 位置的偏移
            BlockPos center = structure.getCenter();
            double vx = x + (center.getX() - te.getPos().getX());
            double vy = y + (center.getY() - te.getPos().getY());
            double vz = z + (center.getZ() - te.getPos().getZ());

            structRenderer.render(structure, vx, vy, vz, pt);
        }
    }
}
```
