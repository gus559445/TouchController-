package top.fifthlight.touchcontroller.common.layout

import org.koin.core.component.get
import top.fifthlight.data.Offset
import top.fifthlight.touchcontroller.common.ext.fastRandomUuid
import top.fifthlight.touchcontroller.common.gal.CrosshairLock
import top.fifthlight.touchcontroller.common.gal.CrosshairTarget
import top.fifthlight.touchcontroller.common.gal.DefaultKeyBindingType
import top.fifthlight.touchcontroller.common.gal.UseSuppress
import top.fifthlight.touchcontroller.common.gal.PlayerHandleFactory
import top.fifthlight.touchcontroller.common.gal.TweakAction
import top.fifthlight.touchcontroller.common.gal.TweakState
import top.fifthlight.touchcontroller.common.gal.ViewActionProvider
import top.fifthlight.touchcontroller.common.helper.EatStopHandler
import top.fifthlight.touchcontroller.common.helper.SneakShieldHandler
import top.fifthlight.touchcontroller.common.helper.fixAspectRadio
import top.fifthlight.touchcontroller.common.state.PointerState

private val viewUuid = fastRandomUuid()


/**
 * 长按「清单里的物品」后，再等这么多刻（0.3 秒）还没真正开始使用，就认定它此刻用不出去，
 * 改判成挖方块。用于原版弓弩之外、没法提前预判的模组物品（典型：没子弹的枪）。
 */
private const val STALE_USE_TICKS = 6

/**
 * 这一下按住的是「点一下就生效、没有蓄力状态」的物品（投掷物之类）。
 *
 * <p>它们的用法是把右键交给原版、让原版按自己的节奏（4 tick 一次）连续触发，
 * 所以不能用「按住几帧还没开始使用 → 改判挖方块」那条兜底去打断它。</p>
 */
private var clickOnlyUse = false

/**
 * 这一段「按住使用」是不是真的开始用过。
 *
 * <p>用来区分两种「现在是 USING、但玩家没在用物品」：</p>
 * <ul>
 *   <li>用过之后停下了（吃完最后一口、物品用完了）→ 要立刻撤掉 USE 键的锁；</li>
 *   <li>压根没开始用（没子弹的模组枪）→ 交给 [STALE_USE_TICKS] 的兜底改判挖方块。</li>
 * </ul>
 */
private var usingEverStarted = false

fun Context.View() {
    val viewActionProvider: ViewActionProvider = get()

    val attackKeyState = keyBindingHandler.getState(DefaultKeyBindingType.ATTACK)
    val useKeyState = keyBindingHandler.getState(DefaultKeyBindingType.USE)

    attackKeyState.refreshLock(viewUuid, timer.renderTick)
    useKeyState.refreshLock(viewUuid, timer.renderTick)

    // ------------------------------------------------------------------
    // 盾牌总规则（放在最前面，所有点击路径共用）：
    //   ① 举盾只由「潜行」接管，右键 / 点击永远不举盾；
    //   ② 潜行举盾期间要动手（挖方块 / 吃 / 用物品 / 开箱子）先收盾让路，动作结束后自动重举；
    //   ③ 手上有能用的弓弩时让位给拉弓；
    //   ④ 长按拿盾（非潜行）改判挖方块。
    // ------------------------------------------------------------------
    val shieldPlayer = get<PlayerHandleFactory>().getPlayerHandle()
    // 手上有「现在就能拉/射」的弓弩：换弓就是换战斗姿态，弓优先——不举盾、不挖方块。
    val bowReady = shieldPlayer != null && TweakState.provider.hasUsableChargeableWeapon()
    // 潜行举盾正在生效（蹲着 + 手上有盾 + 手上没拿能用的弓弩）。
    // 它只表示「盾是自动举着的」，要动手时先收盾（见下面的 release），不再屏蔽任何动作。
    val autoShieldActive = config.tweaks.sneakShield &&
        shieldPlayer != null &&
        shieldPlayer.isSneaking &&
        !bowReady &&
        TweakState.provider.hasShieldOnHand()
    // 主手拿着盾牌：长按这一下走了「用物品」就会去举盾（长按不该举盾，举盾归潜行自动接管）。
    // 只看**主手**——副手放盾不该影响主手正常吃东西 / 用东西。
    // 手上有能用的弓弩时也让位（弓优先，不然会被盾拦住拉不了弓）。
    val holdShield = config.tweaks.sneakShield &&
        !bowReady &&
        TweakState.provider.hasShieldInMainHand()

    // 吃饱收手：合锁期间「长按」只剩「看四周」——视角照常转动，但挖方块 / 攻击 / 用物品全部不发。
    // 这里只负责「维持」：锁是 EatStopHandler 在检测到吃饱那一刻合上的，
    // 到点由它自己按持续时间解除；手都离开屏幕就立刻开锁，下一次按下才是全新的一下。
    val handsLocked = EatStopHandler.isHandsLocked()
    if (handsLocked) {
        attackKeyState.clearLock(viewUuid)
        useKeyState.clearLock(viewUuid)
        // 只看「触控圈上的手指」：边吃边用摇杆走路时，摇杆那根手指不算，不然锁永远解不开。
        if (pointers.values.none { it.state is PointerState.View }) {
            EatStopHandler.unlock()
        }
    }

    var releasedView = false
    for (key in pointers.keys.toList()) {
        val state = pointers[key]!!.state
        if (state is PointerState.Released) {
            if (state.previousState is PointerState.View) {
                val previousState = state.previousState
                when (previousState.viewState) {
                    PointerState.View.ViewPointerState.CONSUMED -> {}
                    PointerState.View.ViewPointerState.BREAKING -> {
                        attackKeyState.clearLock(viewUuid)
                    }

                    PointerState.View.ViewPointerState.USING -> {
                        useKeyState.clearLock(viewUuid)
                    }

                    PointerState.View.ViewPointerState.INITIAL -> {
                        if (!releasedView) {
                            val pressTime = timer.clientTick - previousState.pressTime
                            // Pressed less than time threshold and not moving, recognized as short click
                            if (pressTime < config.control.viewHoldDetectTicks && !previousState.moving) {
                                val crosshairTarget = viewActionProvider.getCrosshairTarget() ?: break
                                when (crosshairTarget) {
                                    CrosshairTarget.BLOCK, CrosshairTarget.MISS -> {
                                        // 这一下是「用」，不是「打」：把攻击键可能残留的点击清掉，
                                        // 否则原版会先把那条左键消费掉——创造模式下直接把方块打掉，
                                        // 表现就是「拿用不了的物品点一下方块反而挖了」。
                                        attackKeyState.clearLock(viewUuid)
                                        TweakAction.provider.clearAttackClicks()
                                        // 点方块统一补一次挥手动画：原版只在「这次点击真的做了什么」时才挥手，
                                        // 拿着用不出去的东西点方块是「什么都没发生」的分支，一点反馈都没有。
                                        // （真的做成了什么的时候，原版自己也会挥手，同一 tick 挥手两次不会看出区别。）
                                        if (crosshairTarget == CrosshairTarget.BLOCK) {
                                            TweakAction.provider.swingArm()
                                        }
                                        if (autoShieldActive) {
                                            // 潜行举盾期间要动手（开箱子、放方块、吃东西）：先收盾，
                                            // 再自己把这一下代执行掉。**不能**走原版点击队列——
                                            // 下一帧盾就会重新举起，原版看到「正在使用物品」会把这条
                                            // 点击整批丢掉，表现就是方块怎么也放不出去。
                                            SneakShieldHandler.release(keyBindingHandler)
                                            TweakAction.provider.performUseClick(
                                                reachAround = config.tweaks.reachAround
                                            )
                                        } else if (SneakShieldHandler.isShieldUp()) {
                                            // 盾还举着但已经不该举了（例如刚站起来）：先收盾，再代执行点击。
                                            SneakShieldHandler.release(keyBindingHandler)
                                            TweakAction.provider.performUseClick(
                                                reachAround = config.tweaks.reachAround
                                            )
                                        } else if (crosshairTarget == CrosshairTarget.MISS
                                            && config.tweaks.reachAround
                                            && TweakAction.provider.canReachAroundPlace()
                                        ) {
                                            // 四周放置：准星对着空气、手里拿方块、人贴在方块边上低头——
                                            // 这一下原版自己放不出去，由这里贴着脚边放一块。
                                            TweakAction.provider.performUseClick(reachAround = true)
                                        } else if (crosshairTarget == CrosshairTarget.BLOCK) {
                                            // 点方块：自己代执行一次使用。原版那条按键路径已经被
                                            // UseSuppress 拦掉了（为了不误触），所以不能再 click() 排队。
                                            TweakAction.provider.performUseClick(
                                                reachAround = config.tweaks.reachAround
                                            )
                                        } else {
                                            // 点空气：照旧走原版点击队列
                                            useKeyState.click()
                                        }
                                    }

                                    CrosshairTarget.ENTITY -> {
                                        if (SneakShieldHandler.isShieldUp()) {
                                            // 举盾时点生物：同样会被原版丢掉，所以先收盾、再自己打这一下。
                                            SneakShieldHandler.release(keyBindingHandler)
                                            TweakAction.provider.performAttackClick()
                                        } else {
                                            // Short click on entity: attack the entity
                                            attackKeyState.click()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                releasedView = true
            }
            // Remove all released pointers, because View is the last layout
            pointers.remove(key)
        }
    }

    var currentViewPointer = pointers.values.firstOrNull {
        it.state is PointerState.View
    }

    currentViewPointer?.let { pointer ->
        val state = pointer.state as PointerState.View

        // Drop all unhandled pointers
        pointers.values.forEach {
            when (it.state) {
                PointerState.New -> it.state = PointerState.Invalid
                else -> {}
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
                viewState = PointerState.View.ViewPointerState.CONSUMED
            )
            return@let
        }

        // 收手期间：视角在上面已经按手指位移转动过了，这里只把「动作」封掉——
        // 挖方块 / 攻击 / 用物品统统不发，但指针状态**保持原样**。
        // 不改成 CONSUMED 的原因：CONSUMED 是终态，锁一解（持续时间到了）指针也回不来，
        // 得等手指抬起才恢复，表现就是「收手期间怎么拖都没反应」。
        if (handsLocked) {
            pointer.state = state.copy(
                lastPosition = pointer.position,
                moving = moving,
            )
            return@let
        }

        if (state.viewState == PointerState.View.ViewPointerState.CONSUMED) {
            pointer.state = state.copy(
                lastPosition = pointer.position,
                moving = moving,
                viewState = PointerState.View.ViewPointerState.CONSUMED
            )
            return@let
        }

        // 弓 / 盾的判定已在函数开头统一算好（bowReady / autoShieldActive / holdShield），这里直接复用。

        // 这段「按住使用」到此结束了（刚吃完最后一口 / 物品用完了）：
        // 立刻撤掉 USE 键的锁，并把这一下作废。
        // 不撤锁的话原版看到「右键还按着」，会拿这一下对着准星上的方块做交互——
        // 对准箱子吃东西，吃完那一下就把箱子打开了（这就是误触的来源）。
        if (state.viewState == PointerState.View.ViewPointerState.USING) {
            // 长按期间每帧续一次「别处理右键」的窗口：原版是每 tick 消费一条点击去对方块
            // 做交互（月光蠕虫女王这类物品的 useOn 会直接把东西放出去），
            // 只拦按下那一瞬间不够，得整段按住都盖住。
            if (!clickOnlyUse &&
                viewActionProvider.getCrosshairTarget() == CrosshairTarget.BLOCK &&
                (viewActionProvider.isTargetBlockInteractive() ||
                    TweakState.provider.isLongPressUsableItem())
            ) {
                UseSuppress.suppressFor()
            }
            // 还吃得下：提前把下一口续上，别留出「按住但没在吃东西」的空档——
            // 原版会拿这个空档去对方块做交互（对准箱子吃东西，吃完就把箱子打开了）。
            // 不看 longPressDig 开关：能不能吃下去是客观事实，跟「长按要不要挖方块」无关。
            // 少了这一条，附魔金苹果这类「饱了也能吃」的食物对准方块就只吃一口。
            if (viewActionProvider.getCrosshairTarget() == CrosshairTarget.BLOCK &&
                TweakState.provider.isHeldFoodEdibleNow()
            ) {
                TweakAction.provider.startUsingHeldItem()
            }
            if (player.isUsingItem) {
                usingEverStarted = true
            } else if (usingEverStarted) {
                usingEverStarted = false
                // 这一口结束了。只有这两种情况才撤锁并作废这一下：
                //   ① 准星对着方块 —— 不撤锁的话原版会顺手去开箱子/按按钮（误触来源）；
                //   ② 已经吃饱（手上吃不下去了）。
                // 对着空气、还没吃饱时**保留**右键按住：原版会自己接着吃下一口，
                // 这正是「对着空气长按一路吃到饱」该有的行为。
                val stopUsing = viewActionProvider.getCrosshairTarget() == CrosshairTarget.BLOCK ||
                    !TweakState.provider.isHeldFoodEdibleNow()
                if (stopUsing) {
                    // 撤锁还不算完：addLock 时往原版队列里塞过一条 USE 点击，
                    // 不清掉的话下一帧原版就会拿它去开箱子/按按钮（防误触就是防这个）。
                    useKeyState.clearLock(viewUuid)
                    TweakAction.provider.clearUseClicks()
                    pointer.state = state.copy(
                        lastPosition = pointer.position,
                        moving = moving,
                        viewState = PointerState.View.ViewPointerState.CONSUMED
                    )
                    return@let
                }
            }
        } else {
            usingEverStarted = false
        }

        val pressTime = timer.clientTick - state.pressTime
        var viewState = state.viewState
        val crosshairTarget = viewActionProvider.getCrosshairTarget()
        // 手指按住方块超过 0.3 秒：这一下的右键不该再发给方块，一律拦掉。
        // 不管有没有滑动、也不管有没有走到 TC 自己的长按阈值——基岩版就是这个手感：
        // 快速点一下才是交互（开箱子、按按钮），按住/拖动都算「在挖」或「在转视角」。
        // 这条是兜底：光在长按分支里拦不够，长按判定没成立（手指微动/按得不够久）时会漏。
        // 同样只针对可交互方块：普通方块要让给「对着方块使用」的物品（月光蠕虫女王等），
        // 拦掉的话那些物品就放不出东西了。
        // 手上是「按着能蓄力」的物品（弓、月光蠕虫女王…）：原版对着方块会先把这一记右键
        // 交给 useOn（月光蠕虫女王就是直接放一个月虫出去），根本走不到蓄力。
        // 所以只要手里是这种物品，长按方块时一律把右键拦掉，改由 startUsingHeldItem 蓄力。
        val chargeItem = TweakState.provider.isLongPressUsableItem()
        if (crosshairTarget == CrosshairTarget.BLOCK &&
            (viewActionProvider.isTargetBlockInteractive() || chargeItem)
        ) {
            // 按下即拦：原版在按下的第 1 个 tick 就会把右键用在方块上，
            // 等 0.3 秒再拦已经晚了（箱子早开了）。短按的交互由下面的 performUseClick 代执行，
            // 那条路径不经过原版按键事件，所以不会被这里拦住。
            UseSuppress.suppressFor()
        }
        // 吃饱收手的黑名单物品：玩家点名要它「吃饱了也接着吃」，所以不算用不出去，
        // 长按就一直是「用物品」，不会被改判成挖方块。
        val keepEating = config.tweaks.eatStop &&
            TweakState.provider.isActiveItemIn(config.tweaks.eatStopKeepItems)
        // 「长按用不了改挖方块」的例外物品（默认金苹果 / 附魔金苹果 / 药水）：
        // 命中 = 长按用不出去时**不**改判挖方块，保持「按着用」。
        // 清单留空时由 provider 用内置默认兜底，老配置也能得到同样的保护。
        val digExempt = config.tweaks.longPressDig &&
            TweakState.provider.isLongPressDigExempt(config.item.longPressDigItems)
        // 清单里的物品还要看「此刻能不能真的用出去」：吃饱还拿着食物、弓弩没弹药时，
        // 长按不能一直空按着，交给下面的挖方块判定。拿不准的模组物品返回 true，
        // 由后面 0.3 秒的兜底规则处理。
        // 手上是「现在就能吃下去的食物」（可食用 + 还能吃）：东西明明能用，长按必须是进食，
        // 绝不能被改判挖方块。这一条故意不看清单——老配置的「可长按使用的物品」可能没勾食物组件。
        // 也不看 longPressDig 开关：附魔金苹果这类「饱了也能吃」的食物，开关关着也得吃得上，
        // 否则对准方块长按就变成挖方块，一口都吃不了。
        // 潜行举盾期间同样成立：这一下先收盾，然后照常吃。
        val canEatHeldFood = TweakState.provider.isHeldFoodEdibleNow()
        // holdShield 已在函数开头算好（主手拿盾 + 手上没弓弩）。
        // 清单里的物品还要看「此刻能不能真的用出去」：吃饱还拿着食物、弓弩没弹药时，
        // 长按不能一直空按着，交给下面的挖方块判定。拿不准的模组物品返回 true，
        // 由后面 0.3 秒的兜底规则处理。
        //
        // 注意 keepEating（收手黑名单）和 digExempt（不改挖方块清单）要**优先于 holdShield**：
        // 玩家点名的物品必须照旧能用，不能因为手上拿着盾就被拦掉。
        // 只看主手，而且盾牌内置黑名单：副手拿盾不该让「主手拿方块」被判成用物品，
        // 否则长按会先按右键把方块放出去一次，然后才改判挖方块。
        // 「能长按使用的物品」（弓、望远镜、月光蠕虫女王…）优先级最高：
        // 直接判成用物品，压过挖方块、举盾、清单等等所有条件，
        // 否则它会被原版当成可放置物品，长按先放出去一块。
        //
        // 但对着**可交互方块**（箱子/工作台/熔炉…）时不算：按住右键的那一下
        // 会被原版拿去开箱子，正是要防的误触，所以这时照常走挖方块。
        // 注意：这里**不能**再因为「对着可交互方块」就把蓄力物品排除掉——
        // 防误触已经由 UseSuppress 在事件层拦右键解决了，再排除一次会让
        // 「对着箱子长按拉弓/吃」变成挖方块（一直挥手）。
        // 「能长按使用的物品」= 形状上能蓄力（弓、望远镜、月光蠕虫女王…），
        // 但还得**此刻真能用出去**才算数：弓弩没弹药、蓄力物品被禁用时判成用物品，
        // 长按就会变成「一直空按着、方块也挖不动」的死操作。canUseHeldItemNow 对
        // 拿不准的模组物品返回 true，所以月光蠕虫女王这类照旧走蓄力。
        val itemUsable = (TweakState.provider.isLongPressUsableItem() &&
            TweakState.provider.canUseHeldItemNow()) ||
            canEatHeldFood || (
                TweakState.provider.hasUsableItemOnMainHand(config.item.usableItems) &&
                    (keepEating ||
                        digExempt ||
                        (!holdShield && TweakState.provider.canUseHeldItemNow())))

        // If pointer kept still and held for hold-detecting ticks in config
        // 对着生物时放宽长按判定：手指几乎不可能完全不动（触控视角总会轻微飘），
        // 而「点一下 = 攻击 / 按住 = 喂食驯服」这个区分全看时间，跟有没有微动无关。
        // 不放宽的话对着生物按住也会被判成短按，结果一驯服就变成打了一拳。
        val longPressReached = pressTime >= config.control.viewHoldDetectTicks &&
            (!moving || crosshairTarget == CrosshairTarget.ENTITY)
        if (viewState == PointerState.View.ViewPointerState.INITIAL && longPressReached) {
            // 潜行举盾期间这一下要真正动手（挖方块 / 吃东西 / 用物品）：先收盾让路，
            // 再照常执行。动作结束后 SneakShieldHandler 会自动重新举盾，格挡不会丢。
            // 只是按着转视角（对着空气 / 生物又没东西可用）时不动盾，免得盾一按一抖。
            if (autoShieldActive &&
                (itemUsable || crosshairTarget == CrosshairTarget.ENTITY ||
                    crosshairTarget == CrosshairTarget.BLOCK)
            ) {
                SneakShieldHandler.release(keyBindingHandler)
            }
            // 对着生物长按，什么时候该发右键？
            //   · 空手 / 普通物品：一直是右键（原行为）
            //   · 食物（金羹、苹果…）：右键，让物品的 interactLivingEntity 先跑（驯服 / 喂食），
            //     不能被「自己吃」抢走
            //   · 真·蓄力武器（弓、月光蠕虫女王）：不抢，照旧按住蓄力——
            //     否则拿弓点生物就成了「右键一下」，既打不到也拉不开
            val entityUseInstead = crosshairTarget == CrosshairTarget.ENTITY &&
                !(itemUsable && TweakState.provider.isLongPressUsableItem())
            viewState = if (entityUseInstead) {
                // 直接代执行一次「对生物右键」：走 gameMode.interactAt / interact，
                // 不依赖原版按键队列，物品的 interactLivingEntity（驯服 / 喂食 / 戎卫号角等）
                // 一定会被调用到。
                TweakAction.provider.performUseClick(reachAround = config.tweaks.reachAround)
                PointerState.View.ViewPointerState.CONSUMED
            } else if (itemUsable) {
                // 长按方块时，手里拿的是食物或蓄力武器：先替原版开始吃/拉弓。
                // 这样原版看到「正在使用物品」，就不会先用这一下右键去开箱子、按按钮，
                // 食物的进食/弓的蓄力不会被方块交互抢走。
                // （对着生物、对着空气长按时不用抢：那两种情况本来就不会发生方块交互。）
                // 长按对着方块、手上是能吃/能蓄力的东西：给平台层开一个「别处理右键」的窗口，
                // 让原版这一下不会拿去开箱子/按按钮（短按不进这个窗口，点方块照旧交互）。
                // 先替原版把吃/蓄力启动起来（gameMode.useItem 这条路径不碰方块）。
                // 不看 longPressDig 开关：那个开关只决定「用不出去时改不改挖方块」，
                // 而对着可交互方块长按时「别开箱子」是一条独立的防误触，任何时候都该成立。
                val holdStarted = TweakAction.provider.startUsingHeldItem()
                // 启动得了（弓 / 食物 / 月光蠕虫女王）→ 按住持续使用；
                // 启动不了（投掷物、点一下就生效的模组道具）→ 这一下交给原版右键：
                // 原版有自己的 4 tick 节奏，按住就能连着用（连着丢投掷物），
                // 而且它不会去挖方块——前提是下面那条兜底别把它改判掉。
                clickOnlyUse = !holdStarted
                if (!clickOnlyUse &&
                    crosshairTarget == CrosshairTarget.BLOCK &&
                    (viewActionProvider.isTargetBlockInteractive() || chargeItem)
                ) {
                    UseSuppress.suppressFor()
                }
                // USE 键一律锁住，可交互方块也不例外。原版每 tick 都会检查
                // 「正在使用物品但右键没按住」并立刻 stopUsingItem——不锁的话对着箱子
                // 蓄力只蓄一下就断（普通方块靠这把锁才蓄得住，箱子不能例外）。
                // 锁会持续往原版队列塞右键，但整段按住期间 UseSuppress 每帧续拦，
                // 那些右键全被作废，不会拿去开箱子。
                useKeyState.addLock(viewUuid, timer.renderTick)
                PointerState.View.ViewPointerState.USING
            } else {
                when (crosshairTarget) {
                    CrosshairTarget.BLOCK -> {
                        // 举盾时空手/拿着用不出去的东西长按方块：先收盾让路。
                        // 第一下点击会被原版丢掉，但挖掘本来就是靠「按住」持续的，不受影响。
                        SneakShieldHandler.release(keyBindingHandler)
                        // 挖方块这一下只该按住左键。使用键的锁、以及**原版队列里还没消费的点击**
                        // 都要清掉：刚放完方块 / 刚吃完东西 / 刚举过盾时，队列里那条右键会被原版
                        // 先消费掉，手里的方块、刷怪蛋就先被放出去一次，然后才开始挖。
                        useKeyState.clearLock(viewUuid)
                        TweakAction.provider.clearUseClicks()
                        // Trigger block breaking
                        clickOnlyUse = false
                        attackKeyState.addLock(viewUuid, timer.renderTick)
                        PointerState.View.ViewPointerState.BREAKING
                    }

                    CrosshairTarget.ENTITY -> {
                        // 长按生物、手上东西又谈不上「蓄力/进食」：代执行一次对生物右键。
                        // 交互成功就是喂食/驯服，失败会继续 useItem（自己吃），
                        // 这和用户定的规则一致：可交互的优先交互，不可交互的优先吃。
                        TweakAction.provider.performUseClick(reachAround = config.tweaks.reachAround)
                        PointerState.View.ViewPointerState.CONSUMED
                    }

                    CrosshairTarget.MISS, null -> PointerState.View.ViewPointerState.CONSUMED
                }
            }
        }

        // 兜底：长按判定成了「用物品」，但 0.3 秒过去还没真正开始使用（典型：没子弹的模组枪、
        // 没箭的弓、吃饱了还拿着的食物），说明它此刻用不出去——改判成挖方块，
        // 手指不至于一直空按着什么都干不了。这条不看 longPressDig 开关：
        // 东西明明用不出去又不给挖，这一下就是彻底没反应，任何人都不想要。
        // 例外清单（金苹果 / 附魔金苹果 / 药水）与「吃饱收手黑名单」里的物品不做这个改判：
        // 它们本来就是「吃饱了 / 用不了也接着按着用」。
        // 手上是「还没吃饱就能吃」的食物时也不改判——那东西明明能用，挖方块是误判。
        if (viewState == PointerState.View.ViewPointerState.USING &&
            !clickOnlyUse &&
            !keepEating &&
            !digExempt &&
            !canEatHeldFood &&
            crosshairTarget == CrosshairTarget.BLOCK &&
            pressTime >= config.control.viewHoldDetectTicks + STALE_USE_TICKS &&
            !player.isUsingItem
        ) {
            useKeyState.clearLock(viewUuid)
            // 要是举着盾，就顺手收掉，别让「正在使用物品」把挖掘的第一次点击也吃掉。
            SneakShieldHandler.release(keyBindingHandler)
            attackKeyState.addLock(viewUuid, timer.renderTick)
            viewState = PointerState.View.ViewPointerState.BREAKING
        }

        pointer.state = state.copy(
            lastPosition = pointer.position,
            moving = moving,
            viewState = viewState
        )
    } ?: run {
        pointers.values.forEach {
            when (it.state) {
                PointerState.New -> {
                    if (currentViewPointer != null) {
                        it.state = PointerState.Invalid
                    } else {
                        // 新的触摸 = 玩家要重新瞄准：先解掉「瞄准指定实体」的锁定，
                        // 否则原版射线一直返回旧目标，永远换不到新的。
                        CrosshairLock.clearLock()
                        it.state = PointerState.View(
                            initialPosition = it.position,
                            lastPosition = it.position,
                            pressTime = timer.clientTick,
                            viewState = PointerState.View.ViewPointerState.INITIAL
                        )
                        currentViewPointer = it
                    }
                }

                else -> {}
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
        if (attackKeyState.haveClickCount() || useKeyState.haveClickCount()) {
            // Keep last crosshair status for key handling
            result.crosshairStatus = status.lastCrosshairStatus
        }
    }

    if (presetControlInfo.splitControls) {
        result.showBlockOutline = true
    }

    status.lastCrosshairStatus = result.crosshairStatus
}