package top.fifthlight.touchcontroller.common.event

import kotlinx.collections.immutable.toPersistentSet
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.LoggerFactory
import top.fifthlight.combine.input.input.TextInputState
import top.fifthlight.combine.input.input.TextRange
import top.fifthlight.combine.paint.Canvas
import top.fifthlight.data.IntOffset
import top.fifthlight.data.Offset
import top.fifthlight.touchcontroller.common.config.GlobalConfigHolder
import top.fifthlight.touchcontroller.common.config.TweakSwitches
import top.fifthlight.touchcontroller.common.config.condition.BuiltinLayerConditionKey
import top.fifthlight.touchcontroller.common.gal.*
import top.fifthlight.touchcontroller.common.helper.EatStopHandler
import top.fifthlight.touchcontroller.common.helper.SneakShieldHandler
import top.fifthlight.touchcontroller.common.helper.chargeCrosshairOverride
import top.fifthlight.touchcontroller.common.helper.fixAspectRadio
import top.fifthlight.touchcontroller.common.input.InputManager
import top.fifthlight.touchcontroller.common.layout.Context
import top.fifthlight.touchcontroller.common.layout.ContextInput
import top.fifthlight.touchcontroller.common.layout.DrawQueue
import top.fifthlight.touchcontroller.common.layout.Hud
import top.fifthlight.touchcontroller.common.model.ControllerHudModel
import top.fifthlight.touchcontroller.common.model.TouchStateModel
import top.fifthlight.touchcontroller.common.platform.PlatformProvider
import top.fifthlight.touchcontroller.proxy.client.PlatformCapability
import top.fifthlight.touchcontroller.proxy.message.*
import java.util.*

object RenderEvents : KoinComponent {
    private val logger = LoggerFactory.getLogger(RenderEvents::class.java)
    private val window: WindowHandle by inject()
    private val configHolder: GlobalConfigHolder by inject()
    private val controllerHudModel: ControllerHudModel by inject()
    private val touchStateModel: TouchStateModel by inject()
    private val playerHandleFactory: PlayerHandleFactory by inject()
    private val platformProvider: PlatformProvider by inject()
    private val gameStateProvider: GameStateProvider by inject()
    private val keyBindingHandler: KeyBindingHandler by inject()
    private val viewActionProvider: ViewActionProvider by inject()
    private var prevWidth = 0
    private var prevHeight = 0

    private val _platformCapabilities = mutableSetOf<PlatformCapability>()
    val platformCapabilities: Set<PlatformCapability> = Collections.unmodifiableSet(_platformCapabilities)

    @JvmStatic
    fun onRenderStart() {
        controllerHudModel.timer.renderTick()
        keyBindingHandler.renderTick(controllerHudModel.timer.renderTick)

        if (controllerHudModel.status.vibrate) {
            platformProvider.platform?.sendEvent(VibrateMessage(VibrateMessage.Kind.BLOCK_BROKEN))
            controllerHudModel.status.vibrate = false
        }

        val config = configHolder.config.value
        // 把「增强功能」开关同步给 Java 侧的事件（交互距离、盾牌弹反击退这些在 Forge 事件里读）。
        TweakSwitches.update(config.tweaks)
        val playerHandle = playerHandleFactory.getPlayerHandle()
        val gameState = gameStateProvider.currentState()
        val platform = platformProvider.platform
        if (platform != null) {
            while (true) {
                val width = WindowEvents.windowWidth
                val height = WindowEvents.windowHeight
                if (width != prevWidth || height != prevHeight) {
                    prevWidth = width
                    prevHeight = height
                    platform.resize(width, height)
                }
                val message = platform.pollEvent() ?: break
                when (message) {
                    is AddPointerMessage -> {
                        touchStateModel.addPointer(
                            index = message.index,
                            position = Offset(
                                x = message.x,
                                y = message.y,
                            )
                        )
                    }

                    is RemovePointerMessage -> {
                        touchStateModel.removePointer(message.index)
                    }

                    ClearPointerMessage -> touchStateModel.clearPointer()

                    is CapabilityMessage -> {
                        PlatformCapability.entries.firstOrNull { it.id == message.capability }?.let {
                            logger.info("TouchController capability ${message.capability} set to ${message.enabled}")
                            if (message.enabled) {
                                _platformCapabilities += it
                            } else {
                                _platformCapabilities -= it
                            }
                        } ?: run {
                            logger.warn("Unknown capability: ${message.capability}, maybe you should update TouchController?")
                        }
                    }

                    is InputStatusMessage -> {
                        message.status?.let { status ->
                            InputManager.updateNativeState(
                                TextInputState(
                                    text = status.text,
                                    composition = TextRange(status.composition.start, status.composition.length),
                                    selection = TextRange(status.selection.start, status.selection.length),
                                    selectionLeft = status.selectionLeft,
                                )
                            )
                        }
                    }

                    is MoveViewMessage -> {
                        playerHandle?.let {
                            val rawDelta = Offset(
                                x = message.deltaYaw,
                                y = message.deltaPitch,
                            )
                            val offset = if (message.screenBased) {
                                rawDelta.fixAspectRadio(window.size) * config.control.viewMovementSensitivity
                            } else {
                                rawDelta
                            }
                            playerHandle.changeLookDirection(
                                deltaYaw = offset.x.toDouble(),
                                deltaPitch = offset.y.toDouble(),
                            )
                        }
                    }

                    else -> {}
                }
            }
        }

        if (config.debug.enableTouchEmulation) {
            val mousePosition = window.mousePosition
            if (window.mouseLeftPressed && mousePosition != null) {
                touchStateModel.addPointer(
                    index = 0,
                    position = mousePosition / window.size.toSize()
                )
            } else {
                touchStateModel.clearPointer()
            }
        }

        if (!gameState.inGame) {
            EatStopHandler.reset()
            // 退出世界：把「我们自己以为盾是举着的」这个状态清掉，否则回到主菜单再进世界会漏一次收盾。
            SneakShieldHandler.reset()
            return
        }
        if (gameState.inGui) {
            touchStateModel.clearPointer()
        }
        val player = playerHandle ?: return
        // 潜行举盾：每渲染帧对一次账——蹲着拿盾就替玩家按住「使用」键，站起来或要动手就先放盾。
        // 用按键锁实现，原版看到的仍是「玩家按着右键」，举盾/收盾全走它自己的流程。
        SneakShieldHandler.update(
            config = config.tweaks,
            player = player,
            keyBindingHandler = keyBindingHandler,
            renderTick = controllerHudModel.timer.renderTick,
            inGui = gameState.inGui,
        )
        // 吃饱收手：每帧看一眼是不是刚吃完一口填肚子的食物、而且已经吃饱。
        EatStopHandler.update(config = config.tweaks, state = TweakState.provider)
        // 瞄准锁定：有锁定时把 hitResult 写回锁定目标。
        // 图层里的按钮走的是原版按键路径，只改 getCrosshairTarget() 它们照样打实时准星。
        CrosshairLock.applyLockedHit()
        if (player.isFlying || player.isSubmergedInWater) {
            keyBindingHandler.getState(DefaultKeyBindingType.SNEAK).clearLock()
        }

        val preset = configHolder.currentPreset.value
        val currentPresetUuid = configHolder.currentPresetUuid.value
        if (controllerHudModel.status.previousPresetUuid != currentPresetUuid) {
            controllerHudModel.status.previousPresetUuid = currentPresetUuid
            controllerHudModel.status.enabledCustomConditions.clear()
        }
        val ridingType = player.ridingEntityType
        val condition = buildSet {
            fun put(key: BuiltinLayerConditionKey.Key, condition: Boolean) {
                if (condition) {
                    add(key)
                }
            }
            put(BuiltinLayerConditionKey.Key.FLYING, player.isFlying)
            put(BuiltinLayerConditionKey.Key.CAN_FLY, player.canFly)
            put(BuiltinLayerConditionKey.Key.SWIMMING, player.isTouchingWater)
            put(BuiltinLayerConditionKey.Key.UNDERWATER, player.isSubmergedInWater)
            put(BuiltinLayerConditionKey.Key.SPRINTING, player.isSprinting)
            put(BuiltinLayerConditionKey.Key.SNEAKING, player.isSneaking)
            put(BuiltinLayerConditionKey.Key.ON_GROUND, player.onGround)
            put(BuiltinLayerConditionKey.Key.NOT_ON_GROUND, !player.onGround)
            put(BuiltinLayerConditionKey.Key.USING_ITEM, player.isUsingItem)
            put(BuiltinLayerConditionKey.Key.RIDING, ridingType != null)
            put(
                BuiltinLayerConditionKey.Key.BLOCK_SELECTED,
                viewActionProvider.getCrosshairTarget() == CrosshairTarget.BLOCK
            )
            // 选中实体：锁着一个实体就算成立（松手后保持，实体跑开/死亡/超距才失效）。
            // 锁本身只会在「触控圈真的在屏幕上指着实体」时建立（见 PlayerHandleImpl），
            // 所以隐藏准星在屏幕中心压到实体不会点亮它。
            put(
                BuiltinLayerConditionKey.Key.ENTITY_SELECTED,
                playerHandle?.targetedEntityId != null
            )
            put(BuiltinLayerConditionKey.Key.ON_MINECART, ridingType == RidingEntityType.MINECART)
            put(BuiltinLayerConditionKey.Key.ON_BOAT, ridingType == RidingEntityType.BOAT)
            put(BuiltinLayerConditionKey.Key.ON_PIG, ridingType == RidingEntityType.PIG)
            put(BuiltinLayerConditionKey.Key.ON_HORSE, ridingType == RidingEntityType.HORSE)
            put(BuiltinLayerConditionKey.Key.ON_CAMEL, ridingType == RidingEntityType.CAMEL)
            put(BuiltinLayerConditionKey.Key.ON_LLAMA, ridingType == RidingEntityType.LLAMA)
            put(BuiltinLayerConditionKey.Key.ON_STRIDER, ridingType == RidingEntityType.STRIDER)
        }.toPersistentSet()

        val drawQueue = DrawQueue()
        val result = Context(
            windowSize = window.size,
            windowScaledSize = window.scaledSize,
            drawQueue = drawQueue,
            size = window.scaledSize,
            screenOffset = IntOffset.ZERO,
            pointers = touchStateModel.pointers,
            input = ContextInput(
                inGui = gameState.inGui,
                builtInCondition = condition,
                customCondition = controllerHudModel.status.enabledCustomConditions.toPersistentSet(),
                perspective = gameState.perspective,
                playerHandle = playerHandle,
                ridingEntityId = playerHandle?.ridingEntityId,
                targetedEntityId = playerHandle?.targetedEntityId,
                targetedEntityState = playerHandle?.targetedEntityState,
                targetedEntityArmor = playerHandle?.targetedEntityArmor ?: emptyList(),
            ),
            status = controllerHudModel.status,
            timer = controllerHudModel.timer,
            keyBindingHandler = keyBindingHandler,
            config = configHolder.config.value,
            presetControlInfo = preset.controlInfo,
        ).run {
            Hud(layers = preset.layout)
            result
        }
        controllerHudModel.result = result
        controllerHudModel.pendingDrawQueue = drawQueue

        val status = controllerHudModel.status
        status.doubleClickCounter.clean(controllerHudModel.timer.clientTick)
        result.pendingAction.forEach { it.invoke(controllerHudModel.timer, player) }
        result.lookDirection?.let { (x, y) ->
            player.changeLookDirection(x.toDouble(), y.toDouble())
        }
        result.inventory.slots.forEachIndexed { index, slot ->
            if (slot.select) {
                player.currentSelectedSlot = index
            }
            if (slot.drop) {
                val stack = player.getInventorySlot(index)
                if (stack.isEmpty) {
                    player.currentSelectedSlot = index
                } else {
                    player.dropSlot(index)
                }
            }
        }
    }

    fun onHudRender(canvas: Canvas) {
        val queue = controllerHudModel.pendingDrawQueue
        queue?.let {
            queue.execute(canvas)
            controllerHudModel.pendingDrawQueue = null
        }
    }

    fun shouldRenderCrosshair(): Boolean {
        val config = configHolder.config.value
        // 蓄力准星：手持弓弩但没蓄力时藏起来，蓄力 / 弩装弹待发时亮起来
        // （这时触控圈会被藏起来，见 layout/Crosshair，两边共用同一份判断）。
        val chargeCrosshair = chargeCrosshairOverride(config.tweaks, TweakState.provider)
        if (chargeCrosshair != null) {
            return chargeCrosshair
        }
        val preset = configHolder.currentPreset.value
        if (!preset.controlInfo.disableCrosshair) {
            return true
        }
        val player = playerHandleFactory.getPlayerHandle() ?: return false
        return player.hasItemsOnHand(config.item.showCrosshairItems)
    }
}