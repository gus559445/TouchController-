package top.fifthlight.touchcontroller

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screen.Screen
import net.minecraftforge.client.event.ClientPlayerNetworkEvent
import net.minecraftforge.client.event.DrawHighlightEvent
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.world.BlockEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.ExtensionPoint
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
import top.fifthlight.combine.platform.CanvasImpl
import top.fifthlight.touchcontroller.buildinfo.BuildInfo
import top.fifthlight.touchcontroller.common.config.GlobalConfigHolder
import top.fifthlight.touchcontroller.common.di.appModule
import top.fifthlight.touchcontroller.common.event.*
import top.fifthlight.touchcontroller.common.model.ControllerHudModel
import top.fifthlight.touchcontroller.common.ui.screen.getConfigScreen
import top.fifthlight.touchcontroller.gal.PlatformWindowProviderImpl
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
                platformModule,
                appModule,
            )
        }

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

        ModLoadingContext.get().activeContainer.registerExtensionPoint(ExtensionPoint.CONFIGGUIFACTORY) {
            BiFunction<Minecraft, Screen, Screen> { client, parent ->
                getConfigScreen(parent) as Screen
            }
        }

        val controllerHudModel: ControllerHudModel = get()
        MinecraftForge.EVENT_BUS.register(object {
            @SubscribeEvent
            fun hudRender(event: RenderGameOverlayEvent.Post) {
                val canvas = CanvasImpl(event.matrixStack)
                RenderSystem.enableBlend()
                RenderEvents.onHudRender(canvas)
                RenderSystem.enableBlend()
            }

            @SubscribeEvent
            fun blockOutlineEvent(event: DrawHighlightEvent.HighlightBlock) {
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
            fun joinWorld(event: ClientPlayerNetworkEvent.LoggedInEvent) {
                ConnectionEvents.onJoinedWorld()
            }
        })
    }
}
