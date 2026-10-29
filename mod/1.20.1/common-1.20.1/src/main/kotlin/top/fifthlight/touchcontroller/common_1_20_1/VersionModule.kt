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
import top.fifthlight.touchcontroller.common.gal.CrosshairLock
import top.fifthlight.touchcontroller.common.gal.EntityListProvider
import top.fifthlight.touchcontroller.common.gal.PlayerHandleFactory
import top.fifthlight.touchcontroller.common.gal.TweakState
import top.fifthlight.touchcontroller.common.gal.VanillaItemListProvider
import top.fifthlight.touchcontroller.common_1_20_1.gal.ChatMessageProviderImpl
import top.fifthlight.touchcontroller.common_1_20_1.gal.DefaultItemListProviderImpl
import top.fifthlight.touchcontroller.common_1_20_1.gal.EntityListProviderImpl
import top.fifthlight.touchcontroller.common_1_20_1.gal.TargetLock
import top.fifthlight.touchcontroller.common_1_20_1.gal.PlayerHandleFactoryImpl
import top.fifthlight.touchcontroller.common_1_20_1.gal.TweakStateProviderImpl
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
    // 实体列表（「骑乘 / 瞄准指定实体」条件选择列表用）：ID + 游戏内显示名。
    single<EntityListProvider> { EntityListProviderImpl }
    // 1.20.1 上把「增强功能」需要的玩家状态判定换成真实实现（common 层默认是空实现）。
    // 模块加载时执行一次，比多注册一份 Koin 定义省事，也不用碰同类型定义覆盖的规则。
    TweakState.provider = TweakStateProviderImpl
    // 「瞄准指定实体」锁定时，让准星判定也用锁定目标，而不是松手后的实时准星。
    CrosshairLock.lockedTarget = { TargetLock.lockedCrosshairTarget() }
    // 并且每帧把 hitResult 写回锁定目标：图层按钮走原版按键路径，只改准星判定不够。
    CrosshairLock.applyLockedHit = {
        if (TargetLock.valid()) {
            TargetLock.hit?.let { net.minecraft.client.Minecraft.getInstance().hitResult = it }
        }
    }
    // 在视图区域重新按下 = 要重新瞄准，先解锁；点图层按钮不经过这里，所以按钮仍打锁定目标。
    CrosshairLock.clearLock = { TargetLock.clear() }
}
