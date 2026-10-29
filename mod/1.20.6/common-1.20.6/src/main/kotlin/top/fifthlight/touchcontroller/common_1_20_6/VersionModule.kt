package top.fifthlight.touchcontroller.common_1_20_6

import org.koin.dsl.module
import top.fifthlight.combine.data.DataComponentTypeFactory
import top.fifthlight.combine.data.ItemFactory
import top.fifthlight.combine.data.TextFactory
import top.fifthlight.combine.platform_1_20_6.DataComponentTypeFactoryImpl
import top.fifthlight.combine.platform_1_20_6.ItemFactoryImpl
import top.fifthlight.combine.platform_1_20_6.ScreenFactoryImpl
import top.fifthlight.combine.platform_1_20_6.TextFactoryImpl
import top.fifthlight.combine.screen.ScreenFactory
import top.fifthlight.touchcontroller.common.di.appModule
import top.fifthlight.touchcontroller.common.gal.ChatMessageProvider
import top.fifthlight.touchcontroller.common.gal.DefaultItemListProvider
import top.fifthlight.touchcontroller.common.gal.PlayerHandleFactory
import top.fifthlight.touchcontroller.common.gal.VanillaItemListProvider
import top.fifthlight.touchcontroller.common_1_20_6.gal.ChatMessageProviderImpl
import top.fifthlight.touchcontroller.common_1_20_6.gal.DefaultItemListProviderImpl
import top.fifthlight.touchcontroller.common_1_20_6.gal.PlayerHandleFactoryImpl
import top.fifthlight.touchcontroller.common_1_20_6.gal.VanillaItemListProviderImpl
import top.fifthlight.touchcontroller.common_1_20_x.platformModule

val versionModule = module {
    includes(
        platformModule,
        appModule,
    )
    single<ItemFactory> { ItemFactoryImpl }
    single<TextFactory> { TextFactoryImpl }
    single<DataComponentTypeFactory> { DataComponentTypeFactoryImpl }
    single<ScreenFactory> { ScreenFactoryImpl }
    single<PlayerHandleFactory> { PlayerHandleFactoryImpl }
    single<DefaultItemListProvider> { DefaultItemListProviderImpl }
    single<VanillaItemListProvider> { VanillaItemListProviderImpl }
    single<ChatMessageProvider> { ChatMessageProviderImpl }
}
