package top.fifthlight.touchcontroller

import com.mojang.blaze3d.systems.RenderSystem
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.client.Minecraft
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.startKoin
import org.koin.logger.slf4jLogger
import org.slf4j.LoggerFactory
import top.fifthlight.combine.platform_1_20_x.CanvasImpl
import top.fifthlight.touchcontroller.common.config.GlobalConfigHolder
import top.fifthlight.touchcontroller.common.event.ConnectionEvents
import top.fifthlight.touchcontroller.common.event.RenderEvents
import top.fifthlight.touchcontroller.common.event.TickEvents
import top.fifthlight.touchcontroller.common.event.WindowEvents
import top.fifthlight.touchcontroller.common.model.ControllerHudModel
import top.fifthlight.touchcontroller.common_1_20_1.versionModule
import top.fifthlight.touchcontroller.common_1_20_x.GameConfigEditorImpl
import top.fifthlight.touchcontroller.common_1_20_x.gal.PlatformWindowProviderImpl
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback as FabricHudRenderCallback

class TouchController : ClientModInitializer, KoinComponent {
    private val logger = LoggerFactory.getLogger(TouchController::class.java)
    
    companion object {
        var loaded = false
    }

    override fun onInitializeClient() {
        logger.info("Loading TouchController…")

        startKoin {
            slf4jLogger()
            modules(
                mixinModule,
                loaderModule,
                versionModule,
            )
        }

        initialize()

        loaded = true
    }

    private fun initialize() {
        val configHolder: GlobalConfigHolder = get()
        configHolder.load()

        FabricHudRenderCallback.EVENT.register { drawContext, _ ->
            val client = Minecraft.getInstance()
            if (!client.options.hideGui) {
                val canvas = CanvasImpl(drawContext)
                RenderSystem.enableBlend()
                RenderEvents.onHudRender(canvas)
            }
        }

        val controllerHudModel: ControllerHudModel = get()
        WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register { _, _ ->
            controllerHudModel.result.showBlockOutline
        }
        ClientTickEvents.END_CLIENT_TICK.register {
            TickEvents.clientTick()
        }
        ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
            ConnectionEvents.onJoinedWorld()
        }
        ClientLifecycleEvents.CLIENT_STARTED.register {
            val client = Minecraft.getInstance()
            WindowEvents.onWindowCreated(PlatformWindowProviderImpl(client.window))
            GameConfigEditorImpl.executePendingCallback()
        }
    }
}
