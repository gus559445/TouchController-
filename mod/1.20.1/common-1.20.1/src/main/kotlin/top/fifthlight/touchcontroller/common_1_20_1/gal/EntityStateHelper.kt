package top.fifthlight.touchcontroller.common_1_20_1.gal

import net.minecraft.client.Minecraft
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.animal.Cat
import net.minecraft.world.entity.animal.Parrot
import net.minecraft.world.entity.animal.Sheep
import net.minecraft.world.entity.animal.Wolf
import net.minecraft.world.entity.animal.horse.AbstractHorse
import net.minecraft.world.entity.npc.Villager
import net.minecraft.world.item.DyeColor
import top.fifthlight.touchcontroller.common.config.condition.TargetedEntityConditions

/**
 * 实体「状态」：给「瞄准指定实体」条件用，可以只对「驯服的坐着的狼」「躺着的村民」「白羊」生效。
 *
 * <p>状态用字符串键表示（[currentState] 读实例、[statesFor] 列某类型可选的状态），
 * 两边必须一一对应，否则条件永远匹配不上：
 * <ul>
 *   <li>幼年 / 成年：`baby` / `adult`——只要这个生物类重写了 {@code isBaby()}
 *       （原版的牛羊猪鸡、村民、僵尸、猪灵……以及模组里自己加的幼年生物）就有这一维</li>
 *   <li>狼 / 猫 / 鹦鹉：`wild`（野生）、`tamed_standing`（驯服站着）、`tamed_sitting`（驯服坐着，
 *       猫还有 `tamed_lying`）</li>
 *   <li>马 / 驴 / 骡：`wild`、`tamed`</li>
 *   <li>村民：`standing` / `sleeping`</li>
 *   <li>羊：`color:&lt;颜色名&gt;`（16 种）</li>
 * </ul></p>
 *
 * <p>幼年和基础状态可以叠加：幼年的个体返回 `baby:&lt;基础状态&gt;`（`baby:tamed_sitting` =
 * 幼年且驯服坐着、`baby:color:white` = 幼年白羊），成年个体就是基础状态本身。
 * 列表里还会额外给一条不挑状态的 `baby`（只要是幼年就算）——
 * 它可以和任何 `baby:…` 组合对应上。</p>
 *
 * <p>只有「没有别的状态维度、但有幼年形态」的类（牛、猪、鸡、僵尸…）才在
 * `baby` 之外再给一条 `adult`：这类生物成年状态没有别的维度可以表达。</p>
 */
object EntityStateHelper {

    private const val BABY = TargetedEntityConditions.BABY_STATE
    private const val BABY_LABEL = "幼年"
    private const val ADULT_STATE = "adult"
    private const val ADULT_LABEL = "成年"

    /** 读实体实例当前的状态键；没有状态维度的实体返回 null。 */
    fun currentState(entity: Entity): String? {
        val base = baseStatesOf(entity)
        val isBaby = entity is LivingEntity && entity.isBaby
        // 有基础状态维度（狼的驯服/坐、羊的颜色、村民的睡…）：幼年就拼成 `baby:<基础状态>`，
        // 这样「幼年的坐着的狼」和「成年的坐着的狼」能分开，而不是幼年一出现就把驯服状态盖掉。
        if (base != null) {
            return if (isBaby) TargetedEntityConditions.babyOf(base) else base
        }
        // 没有基础维度、但这一类有幼年形态：幼年 / 成年各一个键。
        if (entity is LivingEntity && canBeBaby(entity.javaClass)) {
            return if (isBaby) TargetedEntityConditions.BABY_STATE else ADULT_STATE
        }
        return null
    }

    /** 某实体类型可选的状态列表（状态键 to 中文名）；没有状态维度就返回空表。 */
    fun statesFor(type: EntityType<*>): List<Pair<String, String>> {
        val base = baseStatesFor(type)
        val cls = entityClassOf(type)
        // 拿不到实体类（主界面还没进世界）时不能因此少给状态：Mob 的构造函数要用
        // level.getProfilerSupplier()，create(null) 必然 NPE，没有别的途径拿 Class。
        // 这里放宽成「假设它有幼年」——运行时 currentState 按真实实例精确判断，
        // 多出来的条目永远不会匹配，顶多列表里多两条选不中的选项，
        // 比「主界面里根本看不到幼年」好。
        if (cls != null && !canBeBaby(cls)) {
            return base
        }
        // 没有别的状态维度：幼年 / 成年正好把这一类分完，两条都给。
        if (base.isEmpty()) {
            return listOf(BABY to BABY_LABEL, ADULT_STATE to ADULT_LABEL)
        }
        // 已经有别的维度（狼、羊、村民…）：
        //   · 一条「幼年」不挑具体状态（幼年狼 / 幼年白羊都算）
        //   · 再给每个基础状态配一条幼年组合（幼年 · 驯服·坐着）
        // 成年个体继续用原来的状态，不再额外生成永远匹配不上的 `adult`。
        return base +
            (BABY to BABY_LABEL) +
            base.map { (key, label) -> TargetedEntityConditions.babyOf(key) to "$BABY_LABEL · $label" }
    }

    /**
     * 这个类有没有幼年形态。
     *
     * <p>判断依据是「它自己重写了 isBaby()」：原版 {@code LivingEntity.isBaby()} 永远返回 false，
     * 牛羊村民僵尸猪灵这些都各自覆盖成读数据参数，模组生物同理。
     * 这样不用维护类型白名单，模组里加的幼年生物自动就在列表里。</p>
     *
     * <p>结果按类缓存：[currentState] 每帧都会调，反射 getMethod 不该反复做。</p>
     */
    private fun canBeBaby(cls: Class<*>): Boolean = babyCache.getOrPut(cls) {
        LivingEntity::class.java.isAssignableFrom(cls) &&
            runCatching { cls.getMethod("isBaby").declaringClass }.getOrNull() != LivingEntity::class.java
    }

    private val babyCache = HashMap<Class<*>, Boolean>()

    /** 拿 EntityType 对应的实体类；创建不出来（玩家、掉落物之类）就当没有。 */
    private fun entityClassOf(type: EntityType<*>): Class<*>? {
        // 1) 正常路径：世界已加载，直接创建一个实例拿类。
        runCatching { type.create(Minecraft.getInstance().level) }.getOrNull()?.let { return it.javaClass }
        // 2) 主界面（还没有 level）：先用加载器的 base class——不需要实例化，
        //    反射调用是为了在没有 Forge 的环境里也能编译、没有这个方法就自动跳过。
        runCatching {
            type.javaClass.methods
                .firstOrNull { it.name == "getBaseClass" && it.parameterCount == 0 }
                ?.invoke(type) as? Class<*>
        }.getOrNull()?.let { return it }
        // 3) 最后退回 create(null)：多数实体构造里只是把 level 存起来，不会真的用它。
        return runCatching { type.create(null) }.getOrNull()?.javaClass
    }

    /** 实例的原生状态（不含幼年这一维）。 */
    private fun baseStatesOf(entity: Entity): String? = when (entity) {
        is Wolf -> when {
            !entity.isTame -> "wild"
            entity.isInSittingPose -> "tamed_sitting"
            else -> "tamed_standing"
        }

        is Cat -> when {
            !entity.isTame -> "wild"
            entity.isInSittingPose -> "tamed_sitting"
            entity.isLying -> "tamed_lying"
            else -> "tamed_standing"
        }

        is Parrot -> if (entity.isTame) "tamed" else "wild"
        is AbstractHorse -> if (entity.isTamed) "tamed" else "wild"
        is Villager -> if (entity.isSleeping) "sleeping" else "standing"
        is Sheep -> "color:${entity.color.serializedName}"
        else -> null
    }

    /** 类型层面的原生状态表（不含幼年这一维）。 */
    private fun baseStatesFor(type: EntityType<*>): List<Pair<String, String>> = when (type) {
        EntityType.WOLF -> listOf(
            "wild" to "野生",
            "tamed_standing" to "驯服·站着",
            "tamed_sitting" to "驯服·坐着",
        )

        EntityType.CAT -> listOf(
            "wild" to "野生",
            "tamed_standing" to "驯服·站着",
            "tamed_sitting" to "驯服·坐着",
            "tamed_lying" to "驯服·趴着",
        )

        EntityType.PARROT,
        EntityType.HORSE,
        EntityType.DONKEY,
        EntityType.MULE,
        EntityType.SKELETON_HORSE,
        EntityType.ZOMBIE_HORSE,
        -> listOf("wild" to "野生", "tamed" to "驯服")

        EntityType.VILLAGER -> listOf("standing" to "站着", "sleeping" to "躺着")

        EntityType.SHEEP -> DyeColor.entries.map { color ->
            "color:${color.serializedName}" to colorName(color)
        }

        else -> emptyList()
    }

    private fun colorName(color: DyeColor): String = when (color) {
        DyeColor.WHITE -> "白"
        DyeColor.ORANGE -> "橙"
        DyeColor.MAGENTA -> "品红"
        DyeColor.LIGHT_BLUE -> "淡蓝"
        DyeColor.YELLOW -> "黄"
        DyeColor.LIME -> "黄绿"
        DyeColor.PINK -> "粉"
        DyeColor.GRAY -> "灰"
        DyeColor.LIGHT_GRAY -> "淡灰"
        DyeColor.CYAN -> "青"
        DyeColor.PURPLE -> "紫"
        DyeColor.BLUE -> "蓝"
        DyeColor.BROWN -> "棕"
        DyeColor.GREEN -> "绿"
        DyeColor.RED -> "红"
        DyeColor.BLACK -> "黑"
    }
}
