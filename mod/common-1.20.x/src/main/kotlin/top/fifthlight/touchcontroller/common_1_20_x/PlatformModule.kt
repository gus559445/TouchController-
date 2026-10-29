package top.fifthlight.touchcontroller.common_1_20_x

import net.minecraft.client.Minecraft
import org.koin.dsl.module
import top.fifthlight.combine.paint.Canvas
import top.fifthlight.combine.paint.TextMeasurer
import top.fifthlight.combine.platform_1_20_x.CanvasImpl
import top.fifthlight.combine.platform_1_20_x.SoundManagerImpl
import top.fifthlight.combine.platform_1_20_x.TextMeasurerImpl
import top.fifthlight.combine.sound.SoundManager
import top.fifthlight.touchcontroller.common.config.GameConfigEditor
import top.fifthlight.touchcontroller.common.gal.*
import top.fifthlight.touchcontroller.common_1_20_x.gal.*

val platformModule = module {
    val client = Minecraft.getInstance()
    single<SoundManager> { SoundManagerImpl(client.soundManager) }
    single<GameConfigEditor> { GameConfigEditorImpl }
    single<CrosshairRenderer> { CrosshairRendererImpl }
    single<ViewActionProvider> { ViewActionProviderImpl }
    single<GameAction> { GameActionImpl }
    single<GameFeatures> { gameFeatures }
    single<GameStateProvider> { GameStateProviderImpl }
    single<WindowHandle> { WindowHandleImpl }
    single<GameDispatcher> { GameDispatcherImpl }
    single<TextMeasurer> { TextMeasurerImpl }
    factory<Canvas> { params -> CanvasImpl(params.get()) }
}
