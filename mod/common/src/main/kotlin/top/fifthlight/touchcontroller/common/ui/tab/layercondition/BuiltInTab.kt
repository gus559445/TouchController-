package top.fifthlight.touchcontroller.common.ui.tab.layercondition

import androidx.compose.runtime.Composable
import top.fifthlight.combine.data.Identifier
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.placement.fillMaxSize
import top.fifthlight.combine.modifier.placement.padding
import top.fifthlight.combine.modifier.scroll.verticalScroll
import top.fifthlight.combine.widget.base.layout.FlowRow
import top.fifthlight.combine.widget.ui.Icon
import top.fifthlight.combine.widget.ui.Text
import top.fifthlight.touchcontroller.assets.Texts
import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.common.config.condition.BuiltinLayerConditionKey
import top.fifthlight.touchcontroller.common.config.condition.EntitySelectedConditions
import top.fifthlight.touchcontroller.common.ui.component.ListButton

object BuiltInTab : LayerConditionTab() {
    @Composable
    override fun Icon() {
        Icon(Textures.ICON_MOTION)
    }

    override val name: Identifier
        get() = Texts.SCREEN_LAYER_EDITOR_BUILTIN

    @Composable
    override fun Content() {
        val layerConditionTabContext = LocalLayerConditionTabContext.current
        FlowRow(
            modifier = Modifier
                .padding(4)
                .verticalScroll()
                .fillMaxSize(),
            maxColumns = 2,
            expandColumnWidth = true,
        ) {
            for (key in BuiltinLayerConditionKey.Key.entries) {
                // 「选中实体」换成可带状态的那一条（下面单独画），旧的纯布尔条目不再出现在添加列表里。
                if (key == BuiltinLayerConditionKey.Key.ENTITY_SELECTED) {
                    continue
                }
                ListButton(
                    onClick = {
                        layerConditionTabContext.onConditionAdded(
                            BuiltinLayerConditionKey(key)
                        )
                    }
                ) {
                    Text(Text.translatable(key.text))
                }
            }
            // 选中实体（可指定实体控制器的状态：驯服 / 跟随 / 停留 / 游荡）
            ListButton(
                onClick = {
                    layerConditionTabContext.onConditionAdded(EntitySelectedConditions())
                }
            ) {
                Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_CONDITIONS_ENTITY_SELECTED))
            }
        }
    }
}
