package top.fifthlight.touchcontroller

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory
import net.minecraftforge.client.event.ClientPlayerNetworkEvent
import net.minecraftforge.client.event.MovementInputUpdateEvent
import net.minecraftforge.client.event.InputEvent
import net.minecraftforge.client.event.RenderGuiEvent
import net.minecraftforge.client.event.RenderHighlightEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.entity.living.LivingKnockBackEvent
import net.minecraftforge.event.entity.living.ShieldBlockEvent
import net.minecraftforge.event.level.BlockEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.ModLoadingContext
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.startKoin
import org.koin.logger.slf4jLogger
import org.slf4j.LoggerFactory
import top.fifthlight.combine.platform_1_20_x.CanvasImpl
import top.fifthlight.touchcontroller.buildinfo.BuildInfo
import top.fifthlight.touchcontroller.common.config.GlobalConfigHolder
import top.fifthlight.touchcontroller.common.event.*
import top.fifthlight.touchcontroller.common.gal.UseSuppress
import top.fifthlight.touchcontroller.common.model.ControllerHudModel
import top.fifthlight.touchcontroller.common.ui.screen.getConfigScreen
import top.fifthlight.touchcontroller.common_1_20_1.gal.ShieldWhitelist
import top.fifthlight.touchcontroller.common_1_20_1.refactor.ViewGestureRefactorBootstrap
import top.fifthlight.touchcontroller.common_1_20_1.versionModule
import top.fifthlight.touchcontroller.tweak.ShieldTweaks
import top.fifthlight.touchcontroller.common_1_20_x.GameConfigEditorImpl
import top.fifthlight.touchcontroller.common_1_20_x.gal.PlatformWindowProviderImpl
import java.util.function.BiFunction

@Mod(BuildInfo.MOD_ID)
class TouchController : KoinComponent {
    private val logger = LoggerFactory.getLogger(TouchController::class.java)
    
    companion object {
        var loaded = false
    }

    init {
        FMLJavaModLoadingContext.get().modEventBus.apply {
            addListener(::onClientSetup)
            addListener(::onInterModProcess)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onClientSetup(event: FMLClientSetupEvent) {
        logger.info("Loading TouchController…")

        startKoin {
            slf4jLogger()
            modules(
                loaderModule,
                versionModule,
            )
        }

        // 用状态机版本接管视野区手势
        ViewGestureRefactorBootstrap.install()

        initialize()

        val client = Minecraft.getInstance()
        // MUST RUN ON RENDER THREAD
        // Because Forge load mods in parallel, mods don't load on main render thread,
        // which is ok for most cases, but RegisterTouchWindow() and other Win32 API
        // requires caller on the thread created window. We post an event to render
        // thread here, to solve this problem.
        client.tell {
            WindowEvents.onWindowCreated(PlatformWindowProviderImpl(client.window))
        }

        loaded = true
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onInterModProcess(event: InterModProcessEvent) {
        GameConfigEditorImpl.executePendingCallback()
    }

    private fun initialize() {
        val configHolder: GlobalConfigHolder = get()
        configHolder.load()

        ModLoadingContext.get().activeContainer.registerExtensionPoint(ConfigScreenFactory::class.java) {
            ConfigScreenFactory(
                BiFunction<Minecraft, Screen, Screen> { client, parent ->
                    getConfigScreen(parent) as Screen
                }
            )
        }

        val controllerHudModel: ControllerHudModel = get()
        // 加载器侧的盾牌判定：forge 用标准的 SHIELD_BLOCK 动作，模组盾只要声明了就认得出来。
        ShieldWhitelist.extraCheck = { stack -> stack.canPerformAction(net.minecraftforge.common.ToolActions.SHIELD_BLOCK) }
        MinecraftForge.EVENT_BUS.register(object {
            @SubscribeEvent
            fun hudRender(event: RenderGuiEvent.Post) {
                // 「物品 → 盾牌」的白名单每帧推给判定单例：Mixin 里判盾拿不到 Koin，只能读快照。
                ShieldWhitelist.update(configHolder.config.value.item.shieldItems)
                val canvas = CanvasImpl(event.guiGraphics)
                RenderSystem.enableBlend()
                RenderEvents.onHudRender(canvas)
            }

            /**
             * 长按方块、手上是能吃/能蓄力的东西时，把原版这一下「右键使用」整个拦掉：
             * 不拦的话原版会先拿它去开箱子/按按钮，吃和蓄力都轮不上。
             * 窗口由触控层在长按判定成立时打开（{@code UseSuppress}），短按不会进来。
             */
            @SubscribeEvent
            fun suppressUseOnLongPress(event: InputEvent.InteractionKeyMappingTriggered) {
                if (event.isUseItem && UseSuppress.isSuppressed()) {
                    event.isCanceled = true
                }
            }


            @SubscribeEvent
            fun blockOutlineEvent(event: RenderHighlightEvent.Block) {
                if (!controllerHudModel.result.showBlockOutline) {
                    event.isCanceled = true
                }
            }

            @SubscribeEvent
            fun blockBroken(event: BlockEvent.BreakEvent) {
                BlockBreakEvents.afterBlockBreak()
            }

            @SubscribeEvent
            fun clientTick(event: TickEvent.ClientTickEvent) {
                if (event.phase == TickEvent.Phase.END) {
                    TickEvents.clientTick()
                }
            }

            @SubscribeEvent
            fun joinWorld(event: ClientPlayerNetworkEvent.LoggingIn) {
                ConnectionEvents.onJoinedWorld()
            }

            // 盾牌手感修正（设置里「盾牌击退」「举盾减速」两个开关）：
            // ① 挡下近战后把攻击者弹开的距离削弱到约一格；
            // ② 举盾减速不与潜行减速叠加（只改输入，不碰属性，视角不变）。
            @SubscribeEvent
            fun shieldBlock(event: ShieldBlockEvent) {
                ShieldTweaks.onShieldBlock(event, configHolder.config.value.tweaks.shieldKnockback)
            }

            @SubscribeEvent
            fun knockBack(event: LivingKnockBackEvent) {
                ShieldTweaks.onKnockBack(event)
            }

            @SubscribeEvent
            fun movementInput(event: MovementInputUpdateEvent) {
                ShieldTweaks.onMovementInput(event, configHolder.config.value.tweaks.shieldSpeed)
            }

            // 格挡弹开：推到 tick 末尾再施加，保证是这一 tick 最后一次写速度。
            @SubscribeEvent
            fun serverTick(event: TickEvent.ServerTickEvent) {
                if (event.phase == TickEvent.Phase.END) {
                    ShieldTweaks.onServerTick()
                }
            }
        })
    }
}
