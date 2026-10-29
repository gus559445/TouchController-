package top.fifthlight.touchcontroller.common_1_20_1

import org.koin.dsl.module
import top.fifthlight.combine.data.DataComponentTypeFactory
import top.fifthlight.combine.data.ItemFactory
import top.fifthlight.combine.data.TextFactory
import top.fifthlight.combine.platform_1_20_1.FoodComponentTypeFactoryImpl
import top.fifthlight.combine.platform_1_20_1.ItemFactoryImpl
import top.fifthlight.combine.platform_1_20_1.ScreenFactoryImpl
import top.fifthlight.combine.platform_1_20_1.TextFactoryImpl
import top.fifthlight.combine.screen.ScreenFactory
import top.fifthlight.touchcontroller.common.di.appModule
import top.fifthlight.touchcontroller.common.gal.ChatMessageProvider
import top.fifthlight.touchcontroller.common.gal.DefaultItemListProvider
import top.fifthlight.touchcontroller.common.gal.EntityListProvider
import top.fifthlight.touchcontroller.common.gal.PlayerHandleFactory
import top.fifthlight.touchcontroller.common.gal.VanillaItemListProvider
import top.fifthlight.touchcontroller.common_1_20_1.gal.ChatMessageProviderImpl
import top.fifthlight.touchcontroller.common_1_20_1.gal.DefaultItemListProviderImpl
import top.fifthlight.touchcontroller.common_1_20_1.gal.EntityListProviderImpl
import top.fifthlight.touchcontroller.common_1_20_1.gal.PlayerHandleFactoryImpl
import top.fifthlight.touchcontroller.common_1_20_1.gal.VanillaItemListProviderImpl
import top.fifthlight.touchcontroller.common_1_20_x.platformModule

val versionModule = module {
    includes(
        platformModule,
        appModule,
    )
    single<ItemFactory> { ItemFactoryImpl }
    single<TextFactory> { TextFactoryImpl }
    single<DataComponentTypeFactory> { FoodComponentTypeFactoryImpl }
    single<ScreenFactory> { ScreenFactoryImpl }
    single<PlayerHandleFactory> { PlayerHandleFactoryImpl }
    single<DefaultItemListProvider> { DefaultItemListProviderImpl }
    single<VanillaItemListProvider> { VanillaItemListProviderImpl }
    single<ChatMessageProvider> { ChatMessageProviderImpl }
    single<EntityListProvider> { EntityListProviderImpl }
}
