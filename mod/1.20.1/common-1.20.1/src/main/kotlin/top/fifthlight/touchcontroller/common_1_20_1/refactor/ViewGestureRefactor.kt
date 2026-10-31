// 本模块（common-1.20.1）的 build.gradle.kts 没有 mod/common 那样的
// compilerOptions.optIn，所以用到 Uuid 的地方要在这里显式 opt-in。
@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package top.fifthlight.touchcontroller.common_1_20_1.refactor

import org.koin.core.component.get
import org.slf4j.LoggerFactory
import top.fifthlight.data.Offset
import top.fifthlight.touchcontroller.common.ext.fastRandomUuid
import top.fifthlight.touchcontroller.common.gal.CrosshairTarget
import top.fifthlight.touchcontroller.common.gal.DefaultKeyBindingType
import top.fifthlight.touchcontroller.common.gal.KeyBindingHandler
import top.fifthlight.touchcontroller.common.gal.KeyBindingState
import top.fifthlight.touchcontroller.common.gal.PlayerHandleFactory
import top.fifthlight.touchcontroller.common.gal.ViewActionProvider
import top.fifthlight.touchcontroller.common.helper.fixAspectRadio
import top.fifthlight.touchcontroller.common.layout.Context
import top.fifthlight.touchcontroller.common.layout.CrosshairStatus
import top.fifthlight.touchcontroller.common.layout.ViewGestureHook
import top.fifthlight.touchcontroller.common.state.PointerState
import top.fifthlight.touchcontroller.common.state.PointerState.View.ViewPointerState
import top.fifthlight.touchcontroller.helper.EatGuard
import kotlin.uuid.Uuid

/*
 * 1.20.1 专用的视野区手势状态机重构。
 *
 * 只落在 1.20.1 模块里：共享实现 mod/common 的 View() 保持原样，通过
 * ViewGestureHook 由这里在启动时接管（见文件末尾的 Bootstrap）。
 * 其它 MC 版本不安装钩子，行为完全不变。
 */

private val viewUuid = fastRandomUuid()

/** 视野区手势能做的全部动作。判定只产出它，执行只在 [ViewKeyGate.apply] 里发生。 */
internal sealed interface ViewEffect {
    data object LockAttack : ViewEffect
    data object LockUse : ViewEffect
    data object UnlockAttack : ViewEffect
    data object UnlockUse : ViewEffect
    data object TapAttack : ViewEffect
    data object TapUse : ViewEffect

    /** 主动开始使用手上物品（吃第一口 / 拉弓），抢在原版拿这一下右键去放方块之前 */
    data object StartUseItem : ViewEffect

    /** 补一次挥手：点方块时即使原版没做成任何事，也要有可见反馈 */
    data object SwingArm : ViewEffect
}

/**
 * 视野区手势状态机（纯函数，无副作用、不依赖 Minecraft）。
 *
 * 对应基岩版 `TouchTurnInteractControl` 的结构：判定只在少数几个迁移点发生，
 * 每次迁移把「新状态 + 要发的动作」一起交出去，动作不会散落在判定分支里。
 * 转视角不在这里——它不属于状态迁移，`viewGestureRefactored()` 每帧无条件累加。
 */
internal object ViewGestureMachine {

    data class ReleaseInput(
        val viewState: ViewPointerState,
        val pressTimeTicks: Int,
        val moving: Boolean,
        val viewAlreadyReleased: Boolean,
        val holdDetectTicks: Int,
        /** 惰性求值：原实现只在「可能是短按」时才查一次准星 */
        val crosshairTarget: () -> CrosshairTarget?,
    )

    data class ReleaseDecision(
        val effects: List<ViewEffect>,
        /** 原实现里拿不到准星目标会跳出整个 released 循环，这里显式表达 */
        val abortRemainingReleases: Boolean = false,
    )

    data class HoldInput(
        val viewState: ViewPointerState,
        val pressTimeTicks: Int,
        val moving: Boolean,
        val crosshairTarget: CrosshairTarget?,
        val itemUsable: Boolean,
        val holdDetectTicks: Int,
    )

    data class HoldDecision(
        val newViewState: ViewPointerState,
        val effects: List<ViewEffect>,
    )

    fun onRelease(input: ReleaseInput): ReleaseDecision {
        if (input.viewAlreadyReleased) return ReleaseDecision(emptyList())
        return when (input.viewState) {
            ViewPointerState.CONSUMED -> ReleaseDecision(emptyList())
            ViewPointerState.BREAKING -> ReleaseDecision(listOf(ViewEffect.UnlockAttack))
            ViewPointerState.USING -> ReleaseDecision(listOf(ViewEffect.UnlockUse))

            ViewPointerState.INITIAL -> {
                val isTap = input.pressTimeTicks < input.holdDetectTicks && !input.moving
                if (!isTap) return ReleaseDecision(emptyList())
                when (input.crosshairTarget()) {
                    null -> ReleaseDecision(emptyList(), abortRemainingReleases = true)
                    // 点方块：补一次挥手。原版只在 useItemOn 真的做成时才挥，
                    // 拿用不出去的东西点方块会一点反馈都没有。
                    CrosshairTarget.BLOCK -> ReleaseDecision(listOf(ViewEffect.SwingArm, ViewEffect.TapUse))
                    CrosshairTarget.MISS -> ReleaseDecision(listOf(ViewEffect.TapUse))
                    CrosshairTarget.ENTITY -> ReleaseDecision(listOf(ViewEffect.TapAttack))
                }
            }
        }
    }

    fun onHold(input: HoldInput): HoldDecision {
        val shouldDecide = input.viewState == ViewPointerState.INITIAL &&
            input.pressTimeTicks >= input.holdDetectTicks &&
            !input.moving
        if (!shouldDecide) return HoldDecision(input.viewState, emptyList())

        if (input.itemUsable) {
            // 对着方块时必须先主动「开始使用」：甜浆果 / 发光浆果这类既是食物又是 BlockItem 的东西，
            // 否则原版会先走 useItemOn，把浆果放到方块上，根本吃不到。
            val effects = if (input.crosshairTarget == CrosshairTarget.BLOCK) {
                listOf(ViewEffect.StartUseItem, ViewEffect.LockUse)
            } else {
                listOf(ViewEffect.LockUse)
            }
            return HoldDecision(ViewPointerState.USING, effects)
        }
        return when (input.crosshairTarget) {
            CrosshairTarget.BLOCK -> HoldDecision(ViewPointerState.BREAKING, listOf(ViewEffect.LockAttack))
            CrosshairTarget.ENTITY -> HoldDecision(ViewPointerState.CONSUMED, listOf(ViewEffect.TapUse))
            CrosshairTarget.MISS, null -> HoldDecision(ViewPointerState.CONSUMED, emptyList())
        }
    }
}

/**
 * ATTACK / USE 按键状态的**唯一写入口**。
 *
 * 重构前这把锁在 `View()` 里被写了十几处，谁都能改；现在迁移点只调 [apply]，
 * 续期只调 [refresh]，判定逻辑在 [ViewGestureMachine] 里且不持有任何按键对象。
 *
 * [apply] 末尾做一次不变式检查：同一个 owner 不允许同时持 ATTACK 与 USE。
 * 默认关闭，排障时加 `-Dtouchcontroller.debug.assertLocks=true` 打开。
 */
internal class ViewKeyGate(
    private val handler: KeyBindingHandler,
    private val owner: Uuid,
    /** 执行 [ViewEffect.StartUseItem]：1.20.1 侧调 gameMode.useItem 真正开始吃/拉弓 */
    private val startUsingItem: () -> Unit = {},
    /** 执行 [ViewEffect.SwingArm]：1.20.1 侧调 player.swing */
    private val swingArm: () -> Unit = {},
) {
    fun attack(): KeyBindingState = handler.getState(DefaultKeyBindingType.ATTACK)

    fun use(): KeyBindingState = handler.getState(DefaultKeyBindingType.USE)

    fun refresh(renderTick: Int) {
        attack().refreshLock(owner, renderTick)
        use().refreshLock(owner, renderTick)
    }

    fun apply(effects: List<ViewEffect>, renderTick: Int) {
        for (effect in effects) {
            when (effect) {
                ViewEffect.LockAttack -> attack().addLock(owner, renderTick)
                ViewEffect.LockUse -> use().addLock(owner, renderTick)
                ViewEffect.UnlockAttack -> attack().clearLock(owner)
                ViewEffect.UnlockUse -> use().clearLock(owner)
                ViewEffect.TapAttack -> attack().click()
                ViewEffect.TapUse -> use().click()
                ViewEffect.StartUseItem -> startUsingItem()
                ViewEffect.SwingArm -> swingArm()
            }
        }
        if (assertLocks) checkMutualExclusion()
    }

    private fun checkMutualExclusion() {
        val attackLocked = attack().getLock(owner)
        val useLocked = use().getLock(owner)
        if (attackLocked && useLocked) {
            logger.error(
                "View gesture lock invariant violated: owner={} holds ATTACK and USE at the same time",
                owner,
            )
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(ViewKeyGate::class.java)
        private val assertLocks = System.getProperty("touchcontroller.debug.assertLocks") == "true"
    }
}

/**
 * 状态机版的视野区手势，行为与 `mod/common` 的 `Context.View()` 等价：
 *
 * - 转视角：每帧无条件 `lookDirection += movement * sensitivity`；
 * - 按住不动到阈值：手上有可用物品 → 锁 USE；对上方块 → 锁 ATTACK；对生物 → 一次 USE；
 * - 松开：BREAKING/USING 解锁；短按（未达阈值且未移动）按准星目标补一次点击。
 */
internal fun Context.viewGestureRefactored() {
    val viewActionProvider: ViewActionProvider = get()
    val gate = ViewKeyGate(
        handler = keyBindingHandler,
        owner = viewUuid,
        startUsingItem = { findUsableHand(config.item.usableItems)?.let(::startUsingHeldItem) },
        swingArm = ::swingArm,
    )

    gate.refresh(timer.renderTick)

    // 默认不压制方块交互；下面进入 USING 时再打开
    EatGuard.suppressBlockInteraction = false

    var releasedView = false
    for (key in pointers.keys.toList()) {
        val state = pointers[key]?.state ?: continue
        if (state !is PointerState.Released) continue
        val previousState = state.previousState
        // View 是最后一个布局，原实现在这里把所有 Released 指针统一摘掉。
        // 不是 View 指针的 Released 也必须移除：否则按钮/背包格的残留指针会永远留在
        // pointers 里，它们的 `is Released` 分支每帧被重新匹配一次，等于每帧又点了一下。
        if (previousState !is PointerState.View) {
            pointers.remove(key)
            continue
        }

        val decision = ViewGestureMachine.onRelease(
            ViewGestureMachine.ReleaseInput(
                viewState = previousState.viewState,
                pressTimeTicks = timer.clientTick - previousState.pressTime,
                moving = previousState.moving,
                viewAlreadyReleased = releasedView,
                holdDetectTicks = config.control.viewHoldDetectTicks,
                crosshairTarget = viewActionProvider::getCrosshairTarget,
            )
        )
        gate.apply(decision.effects, timer.renderTick)
        if (decision.abortRemainingReleases) break

        releasedView = true
        pointers.remove(key)
    }

    var currentViewPointer = pointers.values.firstOrNull {
        it.state is PointerState.View
    }

    currentViewPointer?.let { pointer ->
        val state = pointer.state as PointerState.View

        // Drop all unhandled pointers
        pointers.values.forEach {
            if (it.state is PointerState.New) {
                it.state = PointerState.Invalid
            }
        }

        var moving = state.moving
        if (!state.moving) {
            // Move detect
            val delta = (pointer.position - state.initialPosition).fixAspectRadio(windowSize).squaredLength
            val threshold = config.control.viewHoldDetectThreshold * 0.01f
            if (delta > threshold * threshold) {
                moving = true
            }
        }

        val movement = (pointer.position - state.lastPosition).fixAspectRadio(windowSize)
        result.lookDirection = (result.lookDirection ?: Offset.ZERO) + movement * config.control.viewMovementSensitivity

        val playerHandleFactory: PlayerHandleFactory = get()
        val player = playerHandleFactory.getPlayerHandle()
        // Consume the pointer if player is null or touch gesture is disabled
        if (player == null || presetControlInfo.disableTouchGesture) {
            pointer.state = state.copy(
                lastPosition = pointer.position,
                moving = moving,
                viewState = ViewPointerState.CONSUMED
            )
            return@let
        }

        // Early exit for consumed pointer
        if (state.viewState == ViewPointerState.CONSUMED) {
            pointer.state = state.copy(lastPosition = pointer.position, moving = moving)
            return@let
        }

        val crosshairTarget = viewActionProvider.getCrosshairTarget()

        val decision = ViewGestureMachine.onHold(
            ViewGestureMachine.HoldInput(
                viewState = state.viewState,
                pressTimeTicks = timer.clientTick - state.pressTime,
                moving = moving,
                crosshairTarget = crosshairTarget,
                // 上游只看「物品在不在可用清单里」；满饥饿拿食物也会被判成可用，
                // 于是长按方块变成吃东西（吃不下）而不挖。这里换成本刻真的能不能用。
                itemUsable = isHeldItemUsableNow(config.item.usableItems),
                holdDetectTicks = config.control.viewHoldDetectTicks,
            )
        )
        gate.apply(decision.effects, timer.renderTick)

        var nextViewState = decision.newViewState
        if (nextViewState == ViewPointerState.USING) {
            // 吃东西/拉弓期间压掉方块交互，避免吃完那一两个 tick 把箱子打开
            EatGuard.suppressBlockInteraction = true
            if (!isHeldItemUsableNow(config.item.usableItems)) {
                // 吃饱了 / 物品用完：撤掉 USE+ATTACK，并把这一按就地终结（CONSUMED）。
                // 手指继续按着也不再有反应——不挖、不用、不交互；只有松手重按才会重新判定，
                // 那时饥饿值已满，自然就走「挖方块」分支。
                gate.apply(listOf(ViewEffect.UnlockUse, ViewEffect.UnlockAttack), timer.renderTick)
                nextViewState = ViewPointerState.CONSUMED
            } else if (!player.isUsingItem && crosshairTarget != CrosshairTarget.ENTITY) {
                // 上一口吃完了但手指还按着：续上，别让这一下右键落到方块上变成放置。
                // 对着生物时绝不主动 useItem —— 那会把「喂牛/喂羊/驯服」变成自己啃食物，
                // 右键要留给原版的 interactEntity。
                gate.apply(listOf(ViewEffect.StartUseItem), timer.renderTick)
            }
        }

        pointer.state = state.copy(
            lastPosition = pointer.position,
            moving = moving,
            viewState = nextViewState,
        )
    } ?: run {
        pointers.values.forEach {
            if (it.state is PointerState.New) {
                if (currentViewPointer != null) {
                    it.state = PointerState.Invalid
                } else {
                    it.state = PointerState.View(
                        initialPosition = it.position,
                        lastPosition = it.position,
                        pressTime = timer.clientTick,
                        viewState = ViewPointerState.INITIAL
                    )
                    currentViewPointer = it
                }
            }
        }
    }

    currentViewPointer?.let { pointer ->
        result.showBlockOutline = true
        // Update current view pointer
        if (!presetControlInfo.splitControls) {
            result.crosshairStatus = CrosshairStatus(
                position = pointer.position,
                breakPercent = viewActionProvider.getCurrentBreakingProgress(),
            )
        }
    } ?: run {
        if (gate.attack().haveClickCount() || gate.use().haveClickCount()) {
            // Keep last crosshair status for key handling
            result.crosshairStatus = status.lastCrosshairStatus
        }
    }

    if (presetControlInfo.splitControls) {
        result.showBlockOutline = true
    }

    status.lastCrosshairStatus = result.crosshairStatus
}

/**
 * 安装点：1.20.1 的模组入口在 `startKoin` 之后调用 [install]。
 * 不调用 = 保持原实现（正是其它版本的现状）。
 */
object ViewGestureRefactorBootstrap {
    fun install() {
        ViewGestureHook.install { context ->
            context.viewGestureRefactored()
            true
        }
    }

    fun uninstall() {
        ViewGestureHook.uninstall()
    }
}
