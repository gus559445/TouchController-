package top.fifthlight.touchcontroller.common.ui.tab.layercondition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.koin.compose.koinInject
import top.fifthlight.combine.data.Identifier
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.layout.Arrangement
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.placement.fillMaxWidth
import top.fifthlight.combine.modifier.placement.padding
import top.fifthlight.combine.modifier.scroll.verticalScroll
import top.fifthlight.combine.widget.base.layout.Column
import top.fifthlight.combine.widget.base.layout.Row
import top.fifthlight.combine.widget.ui.Button
import top.fifthlight.combine.widget.ui.EditText
import top.fifthlight.combine.widget.ui.Icon
import top.fifthlight.combine.widget.ui.Text
import top.fifthlight.touchcontroller.assets.Texts
import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.common.config.condition.TargetedEntityConditions
import top.fifthlight.touchcontroller.common.gal.EntityListProvider
import top.fifthlight.touchcontroller.common.ui.component.ListButton

/**
 * 「瞄准指定实体」条件：准星（触控圈）瞄准到这类实体时图层启用。
 *
 * 布局与「骑乘实体」完全一致：搜索框 + 计数 + 可滚动列表（左 id 右名字）+ 手动输入兜底。
 * 瞄一下再松手不会立刻失效——平台层会锁住那个实体，直到它离开被瞄的位置。
 */
object TargetedEntityTab : LayerConditionTab() {
    @Composable
    override fun Icon() {
        Icon(Textures.ICON_ENTITY)
    }

    override val name: Identifier
        get() = Texts.SCREEN_LAYER_EDITOR_TARGETED_ENTITY

    @Composable
    override fun Content() {
        val layerConditionTabContext = LocalLayerConditionTabContext.current
        val entityListProvider: EntityListProvider = koinInject()
        // 一次性取全部已注册实体（ID + 游戏内显示名）
        val allEntities = remember { entityListProvider.getAllEntities() }
        var input by remember { mutableStateOf("") }

        // 搜索：同时匹配注册ID 和 游戏内显示名（大小写不敏感）
        val matched = remember(input, allEntities) {
            val keyword = input.trim().lowercase()
            if (keyword.isEmpty()) {
                allEntities
            } else {
                allEntities.filter { entry ->
                    entry.id.toString().lowercase().contains(keyword) ||
                        entry.displayName.lowercase().contains(keyword)
                }
            }
        }

        Column(
            modifier = Modifier
                .padding(8)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4),
        ) {
            EditText(
                modifier = Modifier.fillMaxWidth(),
                value = input,
                onValueChanged = { input = it },
                placeholder = Text.literal("按 ID 或游戏内名字搜索，如 zombie / 僵尸"),
            )
            Text(
                Text.literal(
                    "共 ${allEntities.size} 个已注册实体，当前显示 ${matched.size} 个。点选即添加，" +
                        "添加后在左边那条条件上点「状态」选择（只列出当前世界里实际出现过的状态）"
                )
            )

            Column(
                modifier = Modifier
                    .verticalScroll()
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(2),
            ) {
                if (matched.isEmpty()) {
                    Text(Text.literal("未找到匹配的实体"))
                } else {
                    matched.forEach { entry ->
                        ListButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                layerConditionTabContext.onConditionAdded(
                                    TargetedEntityConditions(entry.id)
                                )
                            },
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                // 左侧：注册 ID
                                Text(
                                    modifier = Modifier.weight(1f),
                                    text = Text.literal(entry.id.toString()),
                                )
                                // 右侧：游戏内实际名字
                                if (entry.displayName.isNotBlank()) {
                                    Text(text = Text.literal(entry.displayName))
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val trimmed = input.trim()
                    if (trimmed.isNotEmpty()) {
                        layerConditionTabContext.onConditionAdded(
                            TargetedEntityConditions(Identifier(trimmed))
                        )
                    }
                }
            ) {
                Text(Text.literal("按输入文本作为实体ID添加"))
            }
        }
    }
}
