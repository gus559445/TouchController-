-dontobfuscate
-dontoptimize

-dontwarn android.annotation.*

-keep class top.fifthlight.touchcontroller.TouchController
-keep class top.fifthlight.touchcontroller.mixin.* { *; }
-keep class top.fifthlight.touchcontroller.ForgeGuiFactoryImpl
-keep class top.fifthlight.touchcontroller.TouchControllerCorePlugin
-keep class top.fifthlight.touchcontroller.TouchControllerTransformer { *; }
-keep class top.fifthlight.combine.*.CombineScreen { *; }
-keep class top.fifthlight.touchcontroller.helper.* { *; }
-keep @net.minecraftforge.fml.common.Mod class * { *; }
# 事件订阅类只有 @Mod.EventBusSubscriber 这一个「引用」，没有任何代码调用它们，
# R8 会判成死代码整类删掉（jar 里直接没有这个 .class），功能就永远不会生效。
# 这里逐个点名保留：不要用通配符，否则会把旧的 ShieldKnockback / ShieldSpeed
# 也一起留下来，和 Kotlin 版 ShieldTweaks 重复生效（减速会被乘两次）。
-keep class top.fifthlight.touchcontroller.tweak.CrossbowOffhandHand { *; }
-keep class top.fifthlight.touchcontroller.tweak.ShieldKnockback { *; }
-keep class top.fifthlight.touchcontroller.tweak.ExtraReach { *; }
# ShieldSpeed 故意不 keep：Kotlin 版 ShieldTweaks 已经做了「举盾减速不叠加」的 ×5 补偿，
# 这份旧的 Java 实现再跑一遍会变成 ×25，人物直接飞出去。
-keepclassmembers class * {
    @net.minecraftforge.fml.common.Mod$EventHandler *;
    @net.minecraftforge.eventbus.api.SubscribeEvent *;
    @net.minecraftforge.fml.common.eventhandler.SubscribeEvent *;
}
-keep class top.fifthlight.touchcontroller.platform.win32.Interface { *; }
-keep class top.fifthlight.touchcontroller.platform.wayland.Interface { *; }
-keep class top.fifthlight.touchcontroller.platform.android.Transport { *; }

-keeppackagenames top.fifthlight.touchcontroller.**
-repackageclasses top.fifthlight.touchcontroller.relocated

-allowaccessmodification

-keepattributes Signature,Exceptions,*Annotation*,InnerClasses,PermittedSubclasses,EnclosingMethod,Deprecated,SourceFile,LineNumberTable
