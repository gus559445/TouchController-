package top.fifthlight.touchcontroller

import org.koin.dsl.module
import top.fifthlight.touchcontroller.common.config.ConfigDirectoryProvider
import top.fifthlight.touchcontroller.common.gal.KeyBindingHandler
import top.fifthlight.touchcontroller.common.gal.NativeLibraryPathGetter
import top.fifthlight.touchcontroller.common.gal.TweakAction
import top.fifthlight.touchcontroller.gal.KeyBindingHandlerImpl
import top.fifthlight.touchcontroller.gal.NativeLibraryPathGetterImpl
import top.fifthlight.touchcontroller.gal.TweakActionProviderImpl

val loaderModule = module {
    single<KeyBindingHandler> { KeyBindingHandlerImpl }
    single<ConfigDirectoryProvider> { ConfigDirectoryProviderImpl }
    single<NativeLibraryPathGetter> { NativeLibraryPathGetterImpl }
    // 1.20.1 Forge 上把「增强功能」要执行的动作（代执行点击、四周放置、开始吃/蓄力…）换成真实实现。
    // 和 TweakState 一样，模块加载时执行一次；没注册的版本一直是空实现，功能自然关闭。
    TweakAction.provider = TweakActionProviderImpl
}
