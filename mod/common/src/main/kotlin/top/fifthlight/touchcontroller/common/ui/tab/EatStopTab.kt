package top.fifthlight.touchcontroller.common.ui.tab

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.navigator.LocalNavigator
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.layout.Arrangement
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.placement.fillMaxSize
import top.fifthlight.combine.modifier.placement.padding
import top.fifthlight.combine.modifier.scroll.verticalScroll
import top.fifthlight.combine.widget.base.layout.Column
import top.fifthlight.combine.widget.ui.Button
import top.fifthlight.combine.widget.ui.Text
import top.fifthlight.touchcontroller.assets.BackgroundTextures
import top.fifthlight.touchcontroller.assets.Texts
import top.fifthlight.touchcontroller.common.config.ItemList
import top.fifthlight.touchcontroller.common.config.MAX_EAT_STOP_DURATION
import top.fifthlight.touchcontroller.common.ui.component.HorizontalPreferenceItem
import top.fifthlight.touchcontroller.common.ui.component.IntSliderPreferenceItem
import top.fifthlight.touchcontroller.common.ui.component.SwitchPreferenceItem
import top.fifthlight.touchcontroller.common.ui.model.ConfigScreenModel
import top.fifthlight.touchcontroller.common.ui.screen.ItemListScreen

/**
 * 「物品」分栏里的「吃饱收手」页：开关 + 收手黑名单。
 *
 * <p>为什么放在物品分栏：这条功能管的是「手上的食物」，和「可长按使用的物品」是同一类设置，
 * 所以从「通用 → 常规」搬到这里，顺便把例外物品清单也放在一起。</p>
 */
class EatStopTab(
    private val configScreenModel: ConfigScreenModel,
) : Tab() {
    override val options = TabOptions(
        titleId = Texts.SCREEN_CONFIG_ITEM_EAT_STOP_TITLE,
        group = TabGroup.ItemGroup,
        index = 3,
        onReset = {
            copy(
                tweaks = tweaks.copy(
                    eatStop = true,
                    eatStopKeepItems = ItemList(),
                    eatStopDuration = 0,
                )
            )
        },
    )

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val uiState by configScreenModel.uiState.collectAsState()
        val tweaks = uiState.config.tweaks
        Column(
            modifier = Modifier
                .padding(8)
                .verticalScroll(background = BackgroundTextures.BRICK_BACKGROUND)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8),
        ) {
            SwitchPreferenceItem(
                title = Text.translatable(Texts.SCREEN_CONFIG_ITEM_EAT_STOP_ENABLE_TITLE),
                description = Text.translatable(Texts.SCREEN_CONFIG_ITEM_EAT_STOP_ENABLE_DESCRIPTION),
                value = tweaks.eatStop,
                onValueChanged = { value ->
                    configScreenModel.updateConfig { copy(tweaks = tweaks.copy(eatStop = value)) }
                }
            )
            HorizontalPreferenceItem(
                title = Text.translatable(Texts.SCREEN_CONFIG_ITEM_EAT_STOP_KEEP_TITLE),
                description = Text.translatable(Texts.SCREEN_CONFIG_ITEM_EAT_STOP_KEEP_DESCRIPTION),
            ) {
                Button(
                    onClick = {
                        navigator?.push(
                            ItemListScreen(
                                initialValue = tweaks.eatStopKeepItems.whitelist,
                                onValueChanged = { items ->
                                    configScreenModel.updateConfig {
                                        copy(
                                            tweaks = tweaks.copy(
                                                eatStopKeepItems = tweaks.eatStopKeepItems.copy(whitelist = items)
                                            )
                                        )
                                    }
                                }
                            )
                        )
                    }
                ) {
                    Text(Text.translatable(Texts.SCREEN_CONFIG_ITEM_EDIT_TITLE))
                }
            }
            IntSliderPreferenceItem(
                title = Text.translatable(Texts.SCREEN_CONFIG_ITEM_EAT_STOP_DURATION_TITLE),
                description = Text.translatable(Texts.SCREEN_CONFIG_ITEM_EAT_STOP_DURATION_DESCRIPTION),
                range = 0..MAX_EAT_STOP_DURATION,
                value = tweaks.eatStopDuration.coerceIn(0, MAX_EAT_STOP_DURATION),
                onValueChanged = { value ->
                    configScreenModel.updateConfig { copy(tweaks = tweaks.copy(eatStopDuration = value)) }
                }
            )
        }
    }
}
