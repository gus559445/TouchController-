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
import top.fifthlight.touchcontroller.common.ui.component.HorizontalPreferenceItem
import top.fifthlight.touchcontroller.common.ui.component.SwitchPreferenceItem
import top.fifthlight.touchcontroller.common.ui.model.ConfigScreenModel
import top.fifthlight.touchcontroller.common.ui.screen.ItemListScreen

/**
 * 「物品」分栏里的「长按用不了改挖方块」页：开关 + 例外清单。
 *
 * <p>这条功能管的是「长按手上的东西，但它此刻根本用不出去」（吃饱了还拿着食物、
 * 弓弩没弹药、没子弹的模组枪）——这时候把这一下改判成挖方块，手指不至于一直空按。</p>
 *
 * <p>清单是「例外」：命中的物品长按用不出去时**不**改判挖方块，继续保持「按着用」。
 * 默认放了金苹果、附魔金苹果、三种药水——它们本来就该「吃饱了也按着用」。</p>
 *
 * <p>为什么放在物品分栏：它判断的对象是「手上这件物品能不能用」，
 * 和「可长按使用的物品」「吃饱收手」是同一类设置。</p>
 */
class LongPressDigTab(
    private val configScreenModel: ConfigScreenModel,
) : Tab() {
    override val options = TabOptions(
        titleId = Texts.SCREEN_CONFIG_ITEM_LONG_PRESS_DIG_TITLE,
        group = TabGroup.ItemGroup,
        index = 4,
        onReset = {
            copy(
                item = item.copy(longPressDigItems = ItemList()),
                tweaks = tweaks.copy(longPressDig = true),
            )
        },
    )

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val uiState by configScreenModel.uiState.collectAsState()
        val config = uiState.config
        val tweaks = config.tweaks
        Column(
            modifier = Modifier
                .padding(8)
                .verticalScroll(background = BackgroundTextures.BRICK_BACKGROUND)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8),
        ) {
            SwitchPreferenceItem(
                title = Text.translatable(Texts.SCREEN_CONFIG_ITEM_LONG_PRESS_DIG_ENABLE_TITLE),
                description = Text.translatable(Texts.SCREEN_CONFIG_ITEM_LONG_PRESS_DIG_ENABLE_DESCRIPTION),
                value = tweaks.longPressDig,
                onValueChanged = { value ->
                    configScreenModel.updateConfig { copy(tweaks = tweaks.copy(longPressDig = value)) }
                }
            )
            HorizontalPreferenceItem(
                title = Text.translatable(Texts.SCREEN_CONFIG_ITEM_LONG_PRESS_DIG_EXEMPT_TITLE),
                description = Text.translatable(Texts.SCREEN_CONFIG_ITEM_LONG_PRESS_DIG_EXEMPT_DESCRIPTION),
            ) {
                Button(
                    onClick = {
                        navigator?.push(
                            ItemListScreen(
                                // 这里编辑的就是清单真正的 blacklist 字段：
                                // 黑名单里的物品长按用不出去时保持空按，不改判挖方块。
                                initialValue = config.item.longPressDigItems.blacklist,
                                onValueChanged = { items ->
                                    configScreenModel.updateConfig {
                                        copy(
                                            item = item.copy(
                                                longPressDigItems = item.longPressDigItems.copy(blacklist = items)
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
        }
    }
}
