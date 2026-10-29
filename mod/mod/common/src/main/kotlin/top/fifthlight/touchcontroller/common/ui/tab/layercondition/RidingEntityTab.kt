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
import top.fifthlight.touchcontroller.common.config.condition.RidingEntityConditions
import top.fifthlight.touchcontroller.common.gal.EntityListProvider
import top.fifthlight.touchcontroller.common.ui.component.ListButton

object RidingEntityTab : LayerConditionTab() {
    @Composable
    override fun Icon() {
        Icon(Textures.ICON_MOTION)
    }

    override val name: Identifier
        get() = Texts.SCREEN_LAYER_EDITOR_RIDING_ENTITY

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
                placeholder = Text.literal("按 ID 或游戏内名字搜索，如 horse / 马 / dragon"),
            )
            Text(Text.literal("共 ${allEntities.size} 个已注册实体，当前显示 ${matched.size} 个，点选即添加"))

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
                                    RidingEntityConditions(entry.id)
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
                            RidingEntityConditions(Identifier(trimmed))
                        )
                    }
                }
            ) {
                Text(Text.literal("按输入文本作为实体ID添加"))
            }
        }
    }
}
