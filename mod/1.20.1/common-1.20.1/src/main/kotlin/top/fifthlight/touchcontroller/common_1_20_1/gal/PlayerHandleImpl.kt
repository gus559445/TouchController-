package top.fifthlight.touchcontroller.common_1_20_1.gal

import kotlinx.collections.immutable.toPersistentList
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.animal.Pig
import net.minecraft.world.entity.animal.camel.Camel
import net.minecraft.world.entity.animal.horse.*
import net.minecraft.world.entity.monster.Strider
import net.minecraft.world.entity.vehicle.Boat
import net.minecraft.world.entity.vehicle.Minecart
import top.fifthlight.combine.data.Identifier
import top.fifthlight.combine.data.Item
import top.fifthlight.combine.data.ItemStack
import top.fifthlight.combine.platform_1_20_1.ItemStackImpl
import top.fifthlight.combine.platform_1_20_1.toCombine
import top.fifthlight.touchcontroller.common.config.ItemList
import top.fifthlight.touchcontroller.common.gal.PlayerHandle
import top.fifthlight.touchcontroller.common.gal.PlayerHandleFactory
import top.fifthlight.touchcontroller.common.gal.PlayerInventory
import top.fifthlight.touchcontroller.common.gal.RidingEntityType
import top.fifthlight.touchcontroller.common_1_20_x.gal.AbstractPlayerHandleImpl
import net.minecraft.core.registries.BuiltInRegistries
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import java.util.Optional

/** 锁定目标「离开原位」的判定半径（格）：超过这个距离就算它跑了，条件失效。 */
internal const val TARGET_LOCK_RADIUS = 1.5

/** 瞄准锁的最大作用距离（格）：超过这个距离瞄到实体也不算数，图层不启用。 */
internal const val TARGET_MAX_DISTANCE = 6.0f

/**
 * 视角偏离锁定目标多少度就算「不再瞄着它」（图层失效）。
 *
 * <p>60° = 视线前方 120° 的锥体：手抖、轻微滑动不会丢锁，
 * 但转过半个身子（背对、侧背对）时图层就该消失——那时准星根本不可能压在它身上。</p>
 */
internal const val TARGET_LOCK_MAX_ANGLE_DEG = 60.0

/** 上面那个角度换成余弦值，判定时直接和点积比，省掉每次 acos。 */
private val TARGET_LOCK_MIN_DOT = kotlin.math.cos(Math.toRadians(TARGET_LOCK_MAX_ANGLE_DEG))

/**
 * 瞄准锁：记住最后一次被准星瞄到的实体、它当时的位置，以及那一下的命中结果。
 *
 * <p>必须是单例——{@link PlayerHandleImpl} 每次 {@code getPlayerHandle()} 都新建一个，
 * 状态放实例字段里的话松手/下一帧就丢了，图层会立刻失效。</p>
 *
 * <p>除了给图层条件用，{@code TweakUseExecutor} 执行点击时也调用 [effectiveHit]：
 * 松手后准星（触摸圈）已经消失，如果还用实时的 {@code minecraft.hitResult}，
 * 图层的按钮就会作用在当前准星位置，而不是松手前瞄到的那个目标。</p>
 */
internal object TargetLock {
    var uuid: java.util.UUID? = null
    var pos: net.minecraft.world.phys.Vec3? = null
    var id: Identifier? = null

    /** 锁定时那一下的命中结果（实体或方块），松手后继续用它。 */
    var hit: net.minecraft.world.phys.HitResult? = null

    /**
     * 目标还在不在：实体还活着、没离开被瞄时的位置、离玩家没超过 6 格。
     *
     * <p>跟视角无关。只是把视线转开（背对）时不该丢掉这把锁——
     * 转回来图层要自己回来，不该逼玩家重新瞄一次。</p>
     */
    fun stillThere(): Boolean {
        val uuid = uuid ?: return false
        val lockedPos = pos ?: return false
        val client = net.minecraft.client.Minecraft.getInstance()
        val level = client.level ?: return false
        val entity = level.entitiesForRendering().firstOrNull { it.uuid == uuid } ?: return false
        if (entity.position().distanceTo(lockedPos) > TARGET_LOCK_RADIUS) {
            return false
        }
        // 离玩家太远（超过 6 格）不算瞄准，图层不启用。
        val player = client.player ?: return false
        return player.distanceTo(entity) <= TARGET_MAX_DISTANCE
    }

    /**
     * 锁定此刻能不能真的启用图层：目标还在（[stillThere]），**并且大致还在视线前方**。
     *
     * <p>背对 / 侧背对目标时返回 false，图层随之消失；锁本身保留，
     * 视角转回来就自动恢复。</p>
     */
    fun valid(): Boolean {
        if (!stillThere()) {
            return false
        }
        val client = net.minecraft.client.Minecraft.getInstance()
        val level = client.level ?: return false
        val entity = level.entitiesForRendering().firstOrNull { it.uuid == uuid } ?: return false
        val player = client.player ?: return false
        // 视线方向与「眼睛→实体」的夹角超过阈值就不算还在瞄准。
        val toEntity = entity.position().add(0.0, entity.bbHeight * 0.5, 0.0).subtract(player.eyePosition)
        if (toEntity.lengthSqr() < 1.0E-6) {
            return true
        }
        return player.lookAngle.dot(toEntity.normalize()) >= TARGET_LOCK_MIN_DOT
    }

    /**
     * 这一刻该用哪个命中结果：锁定有效就用锁定时存下的那个，否则退回实时的准星结果。
     */
    fun effectiveHit(): net.minecraft.world.phys.HitResult? =
        if (valid()) hit else net.minecraft.client.Minecraft.getInstance().hitResult

    /**
     * 锁定中目标的准星类型（给 [top.fifthlight.touchcontroller.common.gal.CrosshairLock] 用）。
     * 没有有效锁定就返回 null，调用方退回实时准星。
     */
    fun lockedCrosshairTarget(): top.fifthlight.touchcontroller.common.gal.CrosshairTarget? {
        if (!valid()) {
            return null
        }
        return when (hit?.type) {
            net.minecraft.world.phys.HitResult.Type.ENTITY ->
                top.fifthlight.touchcontroller.common.gal.CrosshairTarget.ENTITY

            net.minecraft.world.phys.HitResult.Type.BLOCK ->
                top.fifthlight.touchcontroller.common.gal.CrosshairTarget.BLOCK

            net.minecraft.world.phys.HitResult.Type.MISS ->
                top.fifthlight.touchcontroller.common.gal.CrosshairTarget.MISS

            else -> null
        }
    }

    fun clear() {
        uuid = null
        pos = null
        id = null
        hit = null
    }
}

class PlayerHandleImpl(inner: LocalPlayer) : AbstractPlayerHandleImpl(inner) {
    override fun hasItemsOnHand(list: ItemList): Boolean = InteractionHand.entries.any { hand ->
        inner.getItemInHand(hand).toCombine().item in list
    }

    override fun matchesItemOnHand(item: Item): Boolean = InteractionHand.entries.any { hand ->
        item.matches(inner.getItemInHand(hand).toCombine().item)
    }

    override fun getInventorySlot(index: Int): ItemStack = ItemStackImpl(inner.inventory.getItem(index))

    override fun getInventory() = PlayerInventory(
        main = inner.inventory.items.map { it.toCombine() }.toPersistentList(),
        armor = inner.inventory.armor.map { it.toCombine() }.toPersistentList(),
        offHand = inner.inventory.offhand.first().toCombine(),
        curios = collectCuriosItems(),
    )

    /**
     * 反射读取 Curios API 的全部饰品槽物品（软依赖：未安装 Curios 时静默返回空列表）。
     * 覆盖暮色森林（护身符/灯/皇冠等）、天境（戒指/披风/手套等）以及其他任何走 Curios 的模组饰品。
     *
     * 调用链（基于 curios-forge-5.14.1+1.20.1 实际字节码确认）：
     *   CuriosApi.getCuriosInventory(LivingEntity) -> LazyOptional<ICuriosItemHandler>
     *   .resolve() -> Optional<ICuriosItemHandler>
     *   ICuriosItemHandler.getSlots() -> IItemHandlerModifiable  （整个饰品栏）
     *   IItemHandler.getSlots() -> int
     *   IItemHandler.getStackInSlot(i) -> ItemStack
     */
    private fun collectCuriosItems(): PersistentList<ItemStack> {
        return try {
            // 1. CuriosApi.getCuriosInventory(LivingEntity) -> LazyOptional<ICuriosItemHandler>
            val curiosApiCls = Class.forName("top.theillusivec4.curios.api.CuriosApi")
            val getInvMethod = curiosApiCls.getMethod(
                "getCuriosInventory",
                net.minecraft.world.entity.LivingEntity::class.java,
            )
            val lazyOptional = getInvMethod.invoke(null, inner) ?: return persistentListOf()

            // 2. LazyOptional.resolve() -> Optional<ICuriosItemHandler>
            val resolved = if (lazyOptional is Optional<*>) {
                lazyOptional
            } else {
                val resolveMethod = lazyOptional.javaClass.methods.firstOrNull {
                    it.name == "resolve" && it.parameterCount == 0
                } ?: return persistentListOf()
                resolveMethod.invoke(lazyOptional) as? Optional<*> ?: return persistentListOf()
            }
            val handler = resolved.orElse(null) ?: return persistentListOf()

            // 3. ICuriosItemHandler.getEquippedCurios() -> IItemHandlerModifiable（整个饰品栏）
            //    （字节码确认：返回 IItemHandlerModifiable 的方法是 getEquippedCurios，不是 getSlots）
            val getEquipped = handler.javaClass.methods.firstOrNull { m ->
                m.name == "getEquippedCurios" && m.parameterCount == 0
            } ?: return persistentListOf()
            val itemHandler = getEquipped.invoke(handler) ?: return persistentListOf()

            // 4. IItemHandler.getSlots() -> int
            val slotCount = (itemHandler.javaClass.methods.firstOrNull { m ->
                m.name == "getSlots" && m.parameterCount == 0
            }?.invoke(itemHandler) as? Int) ?: return persistentListOf()

            // 5. IItemHandler.getStackInSlot(i) -> ItemStack
            val getStackMethod = itemHandler.javaClass.methods.firstOrNull { m ->
                m.name == "getStackInSlot" && m.parameterCount == 1
            } ?: return persistentListOf()

            val result = ArrayList<ItemStack>(slotCount)
            for (i in 0 until slotCount) {
                val stack = getStackMethod.invoke(itemHandler, i) as? net.minecraft.world.item.ItemStack
                if (stack != null && !stack.isEmpty) {
                    result.add(stack.toCombine())
                }
            }
            result.toPersistentList()
        } catch (t: Throwable) {
            // 未安装 Curios 或 API 变动：完全静默跳过（之前这里会刷日志，太吵了），不影响原版护甲检测
            persistentListOf()
        }
    }

    override val ridingEntityType: RidingEntityType?
        get() = when (inner.vehicle) {
            null -> null
            is Minecart -> RidingEntityType.MINECART
            is Boat -> RidingEntityType.BOAT
            is Pig -> RidingEntityType.PIG
            is Camel -> RidingEntityType.CAMEL
            is Horse, is Donkey, is Mule, is ZombieHorse, is SkeletonHorse -> RidingEntityType.HORSE
            is Llama -> RidingEntityType.LLAMA
            is Strider -> RidingEntityType.STRIDER
            else -> RidingEntityType.OTHER
        }

    override val ridingEntityId: Identifier?
        get() = inner.vehicle?.let { vehicle ->
            val rl = BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.type)
            Identifier("${rl.namespace}:${rl.path}")
        }

    // ------------------------------------------------------------------
    // 瞄准指定实体（图层条件用）：瞄准时锁住，松手后保持到实体离开原位
    // ------------------------------------------------------------------

    /**
     * 准星瞄准到的实体 id。
     *
     * <p>只要当前有准星实体（{@code crosshairPickEntity}）就重新锁一次；
     * 没瞄到时不清空，而是看锁定中的那个实体还在不在被瞄准时的那一格——
     * 它跑开（超过 [TARGET_LOCK_RADIUS]）或消失才失效。</p>
     *
     * <p>锁定状态放在 [TargetLock] 单例里： {@link PlayerHandleImpl} 每次
     * {@code getPlayerHandle()} 都会新建，实例字段活不过一帧，松手就丢锁。</p>
     */
    override val targetedEntityId: Identifier?
        get() {
            val client = Minecraft.getInstance()
            val pick = client.crosshairPickEntity
            // 只认 6 格内的实体：更远的瞄到也不算瞄准，图层一律不启用。
            if (pick != null && client.player?.distanceTo(pick)?.let { it <= TARGET_MAX_DISTANCE } == true) {
                TargetLock.uuid = pick.uuid
                TargetLock.pos = pick.position()
                // 连那一下的命中结果一起存下来：松手后图层的按钮要打的就是这个目标。
                TargetLock.hit = client.hitResult
                val rl = BuiltInRegistries.ENTITY_TYPE.getKey(pick.type)
                TargetLock.id = Identifier("${rl.namespace}:${rl.path}")
                return TargetLock.id
            }

            // 目标没了（死了 / 跑远 / 超过 6 格）才真正丢锁。
            // 只是把视角转开不算丢：转回来图层要自己回来，不用重新瞄一次。
            if (!TargetLock.stillThere()) {
                TargetLock.clear()
                return null
            }
            // 目标还在，但视线已经偏开（背对 / 侧背对）：图层这一刻不启用，锁保留。
            if (!TargetLock.valid()) {
                return null
            }
            return TargetLock.id
        }

    /**
     * 锁定目标的**当前**状态键（坐着的狼 / 躺着的村民 / 白羊…）。
     *
     * <p>每次读都按实体实时算，不用锁定那一刻的快照：狼坐下、羊被染色之后，
     * 图层条件要立刻跟着变，而不是非得重新瞄一次才更新。</p>
     */
    override val targetedEntityState: String?
        get() {
            if (!TargetLock.valid()) {
                return null
            }
            val uuid = TargetLock.uuid ?: return null
            val level = Minecraft.getInstance().level ?: return null
            val entity = level.entitiesForRendering().firstOrNull { it.uuid == uuid } ?: return null
            return EntityStateHelper.currentState(entity)
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PlayerHandleImpl

        return inner == other.inner
    }

    override fun hashCode(): Int = inner.hashCode()
}

object PlayerHandleFactoryImpl : PlayerHandleFactory {
    private val client = Minecraft.getInstance()

    override fun getPlayerHandle(): PlayerHandle? = client.player?.let(::PlayerHandleImpl)
}