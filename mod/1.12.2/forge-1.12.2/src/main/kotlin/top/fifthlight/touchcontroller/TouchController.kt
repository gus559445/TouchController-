package top.fifthlight.touchcontroller

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.world.BlockEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.PlayerEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.startKoin
import org.slf4j.LoggerFactory
import top.fifthlight.combine.platform.CanvasImpl
import top.fifthlight.touchcontroller.buildinfo.BuildInfo
import top.fifthlight.touchcontroller.common.config.GlobalConfigHolder
import top.fifthlight.touchcontroller.common.di.appModule
import top.fifthlight.touchcontroller.common.event.*
import top.fifthlight.touchcontroller.gal.PlatformWindowProviderImpl

@Mod(
    modid = BuildInfo.MOD_ID,
    name = BuildInfo.MOD_NAME,
    version = BuildInfo.MOD_VERSION,
    clientSideOnly = true,
    acceptedMinecraftVersions = "1.12.2",
    acceptableRemoteVersions = "*",
    canBeDeactivated = false,
    guiFactory = "top.fifthlight.touchcontroller.ForgeGuiFactoryImpl"
)
class TouchController : KoinComponent {
    private val logger = LoggerFactory.getLogger(TouchController::class.java)

    @Mod.EventHandler
    fun onClientSetup(event: FMLInitializationEvent) {
        logger.info("Loading TouchController…")

        startKoin {
            modules(
                platformModule,
                appModule,
            )
        }

        initialize()

        val client = Minecraft.getMinecraft()
        // MUST RUN ON RENDER THREAD
        // Because Forge load mods in parallel, mods don't load on main render thread,
        // which is ok for most cases, but RegisterTouchWindow() and other Win32 API
        // requires caller on the thread created window. We post an event to render
        // thread here, to solve this problem.
        client.addScheduledTask {
            WindowEvents.onWindowCreated(PlatformWindowProviderImpl)
        }
    }

    @Mod.EventHandler
    fun onLoadComplete(event: FMLLoadCompleteEvent) {
        GameConfigEditorImpl.executePendingCallback()
    }

    private fun initialize() {
        val configHolder: GlobalConfigHolder = get()
        configHolder.load()

        MinecraftForge.EVENT_BUS.register(object {
            @SubscribeEvent
            fun hudRender(event: RenderGameOverlayEvent.Post) {
                if (event.type == ElementType.ALL) {
                    val canvas = CanvasImpl()
                    GlStateManager.pushMatrix()
                    GlStateManager.disableLighting()
                    GlStateManager.enableAlpha()
                    GlStateManager.enableBlend()
                    RenderEvents.onHudRender(canvas)
                    GlStateManager.enableAlpha()
                    GlStateManager.enableBlend()
                    GlStateManager.popMatrix()
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
            fun joinWorld(event: PlayerEvent.PlayerLoggedInEvent) {
                ConnectionEvents.onJoinedWorld()
            }
        })
    }
}
