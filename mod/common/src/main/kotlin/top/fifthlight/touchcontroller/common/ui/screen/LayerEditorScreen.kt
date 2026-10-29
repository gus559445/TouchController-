package top.fifthlight.touchcontroller.common.ui.screen

import androidx.compose.runtime.*
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.StateFlow
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import top.fifthlight.combine.data.Identifier
import top.fifthlight.combine.data.LocalItemFactory
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.layout.Alignment
import top.fifthlight.combine.layout.Arrangement
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.drawing.border
import top.fifthlight.combine.modifier.placement.*
import top.fifthlight.combine.modifier.scroll.verticalScroll
import top.fifthlight.combine.widget.base.layout.Box
import top.fifthlight.combine.widget.base.layout.Column
import top.fifthlight.combine.widget.base.layout.Row
import top.fifthlight.combine.widget.ui.*
import top.fifthlight.touchcontroller.assets.Texts
import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.common.config.LayoutLayer
import top.fifthlight.touchcontroller.common.config.condition.BuiltinLayerConditionKey
import top.fifthlight.touchcontroller.common.config.condition.CustomLayerConditionKey
import top.fifthlight.touchcontroller.common.config.condition.HoldingItemConditions
import top.fifthlight.touchcontroller.common.config.condition.WearingItemConditions
import top.fifthlight.touchcontroller.common.config.condition.RidingEntityConditions
import top.fifthlight.touchcontroller.common.config.condition.TargetedEntityConditions
import top.fifthlight.touchcontroller.common.config.condition.LayerConditions
import top.fifthlight.touchcontroller.common.config.preset.LayerCustomConditions
import top.fifthlight.touchcontroller.common.config.preset.LayoutPreset
import top.fifthlight.touchcontroller.common.gal.EntityListProvider
import top.fifthlight.touchcontroller.common.ui.component.*
import top.fifthlight.touchcontroller.common.ui.model.LayerEditorScreenModel
import top.fifthlight.touchcontroller.common.ui.screen.itemlist.ItemListChooseScreen
import top.fifthlight.touchcontroller.common.ui.tab.layercondition.LayerConditionTabContext
import top.fifthlight.touchcontroller.common.ui.tab.layercondition.LocalLayerConditionTabContext
import top.fifthlight.touchcontroller.common.ui.tab.layercondition.allLayerConditionTabs

@Composable
private fun LayerConditionItem(
    preset: LayoutPreset,
    item: LayerConditions.Item,
    onValueChanged: (LayerConditions.Value) -> Unit,
    onKeyChanged: (LayerConditions.Key) -> Unit = {},
    onItemRemoved: () -> Unit = {},
) {
    // 「瞄准指定实体」这一条下面还要挂一排状态选择器，所以整行包进 Column。
    val targetedCondition = item.key as? TargetedEntityConditions
    Column(modifier = Modifier.fillMaxWidth()) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        Row(
            modifier = Modifier
                .border(LocalListButtonDrawable.current.unchecked.normal)
                .fillMaxHeight()
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4),
        ) {
            when (item.key) {
                is BuiltinLayerConditionKey -> {
                    Text(Text.translatable(item.key.key.text))
                }

                is CustomLayerConditionKey -> {
                    preset.controlInfo.customConditions.conditions.firstOrNull { it.uuid == item.key.key }?.let {
                        Text(it.name?.let { Text.literal(it) }
                            ?: Text.translatable(Texts.SCREEN_LAYER_EDITOR_CUSTOM_CONDITION_UNNAMED))
                    } ?: Text(Text.translatable(Texts.SCREEN_LAYER_EDITOR_CUSTOM_CONDITION_UNKNOWN))
                }

                is HoldingItemConditions -> {
                    val itemFactory = LocalItemFactory.current
                    val stack = remember(item.key.item) { itemFactory.createItemStack(item.key.item, 1) }
                    Item(item = item.key.item)
                    Text(stack.name)
                }

                is WearingItemConditions -> {
                    val itemFactory = LocalItemFactory.current
                    val stack = remember(item.key.item) { itemFactory.createItemStack(item.key.item, 1) }
                    Item(item = item.key.item)
                    Text(stack.name)
                }

                is RidingEntityConditions -> {
                    Text(Text.literal("骑乘: ${item.key.entity}"))
                }

                is TargetedEntityConditions -> {
                    Text(Text.literal("瞄准: ${item.key.entity}"))
                }
            }
        }

        var expanded by remember { mutableStateOf(false) }
        Select(
            modifier = Modifier.fillMaxHeight(),
            expanded = expanded,
            onExpandedChanged = { expanded = it },
            dropDownContent = {
                DropdownItemList(
                    modifier = Modifier.verticalScroll(),
                    onItemSelected = { expanded = false },
                    items = LayerConditions.Value.entries.map {
                        Pair(Text.translatable(it.text)) {
                            onValueChanged(it)
                        }
                    }.toPersistentList(),
                )
            }
        ) {
            Text(Text.translatable(item.value.text))
            SelectIcon(expanded = expanded)
        }

        IconButton(
            modifier = Modifier.fillMaxHeight(),
            onClick = onItemRemoved,
        ) {
            Icon(Textures.ICON_DELETE)
        }
    }

    // 状态选择器：挂在「瞄准: xxx」这一条下面，占整行宽度，不会把实体 ID 挤掉。
    // 展开的那一刻才去世界里检测，只列出真的出现过的状态。
    if (targetedCondition != null) {
        val entityListProvider: EntityListProvider = koinInject()
        val itemFactory = LocalItemFactory.current
        val stateLabel = remember(targetedCondition.entity, targetedCondition.state) {
            val stateKey = targetedCondition.state
            when {
                stateKey == null -> null
                stateKey.startsWith(TargetedEntityConditions.ARMOR_PREFIX) -> {
                    val itemId = stateKey.removePrefix(TargetedEntityConditions.ARMOR_PREFIX)
                    itemFactory.createItemStack(Identifier(itemId), 1)?.name?.string ?: itemId
                }
                else -> entityListProvider.getAllStates(targetedCondition.entity)
                    .firstOrNull { it.stateKey == stateKey }?.stateLabel ?: stateKey
            }
        }
        val navigator = LocalNavigator.current
        var stateExpanded by remember(targetedCondition.entity) { mutableStateOf(false) }
        val availableStates = remember(targetedCondition.entity, stateExpanded) {
            if (stateExpanded) entityListProvider.getAvailableStates(targetedCondition.entity) else emptyList()
        }
        val stateOptions: List<Pair<Text, String?>> =
            listOf<Pair<Text, String?>>(Text.literal("任意状态") to null) +
                availableStates.map { entry ->
                    Text.literal(entry.stateLabel ?: entry.stateKey.orEmpty()) to entry.stateKey
                }
        Select(
            modifier = Modifier.fillMaxWidth(),
            expanded = stateExpanded,
            onExpandedChanged = { stateExpanded = it },
            dropDownContent = {
                DropdownItemList(
                    modifier = Modifier.verticalScroll(),
                    onItemSelected = { stateExpanded = false },
                    items = (
                        stateOptions.map { (label, stateKey) ->
                            Pair(label) { onKeyChanged(targetedCondition.copy(state = stateKey)) }
                        } + listOf(
                            Pair(Text.literal("穿戴指定装备…")) {
                                // 直接打开物品列表挑，不用手输 ID
                                navigator?.push(
                                    ItemListChooseScreen { item ->
                                        onKeyChanged(
                                            targetedCondition.copy(
                                                state = TargetedEntityConditions.armorOf(item.id.toString())
                                            )
                                        )
                                    }
                                )
                                // lambda 必须显式返回 Unit：push 的返回值是 Unit?，会让整个 lambda 推断成 () -> Unit?
                                Unit
                            }
                        )
                        ).toPersistentList(),
                )
            }
        ) {
            Text(Text.literal("状态: " + (stateLabel ?: "任意")))
            SelectIcon(expanded = stateExpanded)
        }
    }
    }
}

class LayerEditorScreen(
    private val screenName: Identifier,
    private val preset: StateFlow<LayoutPreset?>,
    private val onCustomConditionsChanged: (LayerCustomConditions) -> Unit,
    private val initialValue: LayoutLayer,
    private val onValueChanged: (LayoutLayer) -> Unit,
) : Screen {
    @Composable
    override fun Content() {
        val screenModel: LayerEditorScreenModel = koinScreenModel { parametersOf(initialValue, onValueChanged) }
        val uiState by screenModel.uiState.collectAsState()
        val navigator = LocalNavigator.current
        val preset by preset.collectAsState()
        var innerNavigator by remember { mutableStateOf<Navigator?>(null) }
        Scaffold(
            topBar = {
                AppBar(
                    modifier = Modifier.fillMaxWidth(),
                    leading = {
                        BackButton(screenName = Text.translatable(screenName))
                    },
                    trailing = {
                        Button(
                            onClick = {
                                screenModel.applyChanges()
                                navigator?.pop()
                            }
                        ) {
                            Text(Text.translatable(Texts.SCREEN_LAYER_EDITOR_SAVE))
                        }
                    },
                )
            },
        ) { modifier ->
            Column(modifier = modifier) {
                Row(
                    modifier = Modifier.height(IntrinsicSize.Min),
                ) {
                    EditText(
                        modifier = Modifier.weight(1f),
                        value = uiState.name,
                        onValueChanged = screenModel::editName,
                        placeholder = Text.translatable(Texts.SCREEN_LAYER_EDITOR_NAME_PLACEHOLDER),
                    )
                    Row {
                        innerNavigator?.let { innerNavigator ->
                            for (tab in allLayerConditionTabs) {
                                if (tab == innerNavigator.lastItem) {
                                    ListButton(
                                        modifier = Modifier
                                            .fillMaxSize(.2f)
                                            .minWidth(100)
                                            .fillMaxHeight(),
                                        onClick = {},
                                    ) {
                                        Text(Text.translatable(tab.name))
                                    }
                                } else {
                                    IconButton(
                                        modifier = Modifier.fillMaxHeight(),
                                        onClick = {
                                            innerNavigator.replace(tab)
                                        },
                                    ) {
                                        tab.Icon()
                                    }
                                }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.weight(1f),
                ) {
                    if (uiState.conditions.conditions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .border(Textures.WIDGET_BACKGROUND_BACKGROUND_DARK)
                                .weight(.4f)
                                .fillMaxHeight(),
                        ) {
                            Text(
                                modifier = Modifier.alignment(Alignment.Center),
                                text = Text.translatable(Texts.SCREEN_LAYER_EDITOR_CONDITION_EMPTY),
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .padding(4)
                                .verticalScroll()
                                .border(Textures.WIDGET_BACKGROUND_BACKGROUND_DARK)
                                .weight(.4f)
                                .fillMaxHeight(),
                        ) {
                            val preset = preset ?: return@Column
                            for ((index, condition) in uiState.conditions.conditions.withIndex()) {
                                LayerConditionItem(
                                    preset = preset,
                                    item = condition,
                                    onValueChanged = {
                                        screenModel.editCondition(index) {
                                            copy(value = it)
                                        }
                                    },
                                    onKeyChanged = {
                                        screenModel.editCondition(index) {
                                            copy(key = it)
                                        }
                                    },
                                    onItemRemoved = {
                                        screenModel.removeCondition(index)
                                    },
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .border(Textures.WIDGET_BACKGROUND_BACKGROUND_DARK)
                            .weight(.6f)
                            .fillMaxHeight(),
                        alignment = Alignment.Center,
                    ) {
                        val preset = preset ?: return@Box
                        TouchControllerNavigator(allLayerConditionTabs.first()) { navigator ->
                            innerNavigator = navigator
                            val currentLayerConditionTabContext = LayerConditionTabContext(
                                preset = preset,
                                onCustomConditionsChanged = onCustomConditionsChanged,
                                onConditionAdded = {
                                    screenModel.addCondition(
                                        LayerConditions.Item(
                                            key = it,
                                            value = LayerConditions.Value.WANT,
                                        )
                                    )
                                }
                            )
                            CompositionLocalProvider(
                                LocalLayerConditionTabContext provides currentLayerConditionTabContext,
                            ) {
                                CurrentScreen()
                            }
                        }
                    }
                }
            }
        }
    }
}