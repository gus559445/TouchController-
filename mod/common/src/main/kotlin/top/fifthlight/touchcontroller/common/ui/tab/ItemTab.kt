package top.fifthlight.touchcontroller.common.ui.tab

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.navigator.LocalNavigator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import top.fifthlight.combine.data.LocalItemFactory
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.layout.Arrangement
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.drawing.background
import top.fifthlight.combine.modifier.placement.fillMaxSize
import top.fifthlight.combine.modifier.placement.padding
import top.fifthlight.combine.modifier.scroll.verticalScroll
import top.fifthlight.combine.widget.base.layout.Column
import top.fifthlight.combine.widget.ui.Button
import top.fifthlight.combine.widget.ui.Switch
import top.fifthlight.combine.widget.ui.Text
import top.fifthlight.touchcontroller.assets.BackgroundTextures
import top.fifthlight.touchcontroller.assets.Texts
import top.fifthlight.touchcontroller.common.config.ItemList
import top.fifthlight.touchcontroller.common.gal.DefaultItemListProvider
import top.fifthlight.touchcontroller.common.ui.component.HorizontalPreferenceItem
import top.fifthlight.touchcontroller.common.ui.component.SwitchPreferenceItem
import top.fifthlight.touchcontroller.common.ui.model.ConfigScreenModel
import top.fifthlight.touchcontroller.common.ui.screen.ComponentScreen
import top.fifthlight.touchcontroller.common.ui.screen.ItemListScreen

class ItemTabs(
    private val configScreenModel: ConfigScreenModel,
) : KoinComponent {
    private val itemListProvider: DefaultItemListProvider by inject()

    val usableItemsTab = ItemTab(
        options = TabOptions(
            titleId = Texts.SCREEN_CONFIG_ITEM_USABLE_ITEMS_TITLE,
            group = TabGroup.ItemGroup,
            index = 0,
            onReset = { copy(item = item.copy(usableItems = itemListProvider.usableItems)) },
        ),
        value = configScreenModel.uiState.map { it.config.item.usableItems },
        onValueChanged = {
            configScreenModel.updateConfig { copy(item = item.copy(usableItems = it)) }
        }
    )

    val showCrosshairItemsTab = ItemTab(
        options = TabOptions(
            titleId = Texts.SCREEN_CONFIG_ITEM_SHOW_CROSSHAIR_ITEMS_TITLE,
            group = TabGroup.ItemGroup,
            index = 1,
            onReset = { copy(item = item.copy(showCrosshairItems = itemListProvider.showCrosshairItems)) },
        ),
        value = configScreenModel.uiState.map { it.config.item.showCrosshairItems },
        onValueChanged = {
            configScreenModel.updateConfig { copy(item = item.copy(showCrosshairItems = it)) }
        }
    )

    val shieldItemsTab = ShieldItemTab(
        options = TabOptions(
            titleId = Texts.SCREEN_CONFIG_ITEM_SHIELD_ITEMS_TITLE,
            group = TabGroup.ItemGroup,
            index = 3,
            onReset = {
                copy(
                    tweaks = tweaks.copy(sneakShield = true, shieldKnockback = true),
                    item = item.copy(shieldItems = ItemList()),
                )
            },
        ),
        configScreenModel = configScreenModel,
    )

    val crosshairAimingItemsTab = ItemTab(
        options = TabOptions(
            titleId = Texts.SCREEN_CONFIG_ITEM_CROSSHAIR_AIMING_ITEMS_TITLE,
            group = TabGroup.ItemGroup,
            index = 2,
            onReset = { copy(item = item.copy(crosshairAimingItems = itemListProvider.crosshairAimingItems)) },
        ),
        value = configScreenModel.uiState.map { it.config.item.crosshairAimingItems },
        onValueChanged = {
            configScreenModel.updateConfig { copy(item = item.copy(crosshairAimingItems = it)) }
        }
    )
}

class ItemTab(
    override val options: TabOptions,
    val value: Flow<ItemList>,
    val onValueChanged: (ItemList) -> Unit,
) : Tab() {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val value by value.collectAsState(null)
        Column(
            modifier = Modifier
                .padding(8)
                .verticalScroll(background = BackgroundTextures.BRICK_BACKGROUND)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8),
        ) {
            value?.let { value ->
                HorizontalPreferenceItem(
                    title = Text.translatable(Texts.SCREEN_CONFIG_ITEM_WHITELIST_TITLE),
                    description = Text.translatable(Texts.SCREEN_CONFIG_ITEM_WHITELIST_DESCRIPTION),
                ) {
                    Button(
                        onClick = {
                            navigator?.push(
                                ItemListScreen(
                                    initialValue = value.whitelist,
                                    onValueChanged = { onValueChanged(value.copy(whitelist = it)) },
                                )
                            )
                        }
                    ) {
                        Text(Text.translatable(Texts.SCREEN_CONFIG_ITEM_EDIT_TITLE))
                    }
                }
                HorizontalPreferenceItem(
                    title = Text.translatable(Texts.SCREEN_CONFIG_ITEM_BLACKLIST_TITLE),
                    description = Text.translatable(Texts.SCREEN_CONFIG_ITEM_BLACKLIST_DESCRIPTION),
                ) {
                    Button(
                        onClick = {
                            navigator?.push(
                                ItemListScreen(
                                    initialValue = value.blacklist,
                                    onValueChanged = { onValueChanged(value.copy(blacklist = it)) },
                                )
                            )
                        }
                    ) {
                        Text(Text.translatable(Texts.SCREEN_CONFIG_ITEM_EDIT_TITLE))
                    }
                }
                HorizontalPreferenceItem(
                    title = Text.translatable(Texts.SCREEN_CONFIG_ITEM_COMPONENT_TITLE),
                    description = Text.translatable(Texts.SCREEN_CONFIG_ITEM_COMPONENT_DESCRIPTION),
                ) {
                    Button(
                        onClick = {
                            navigator?.push(
                                ComponentScreen(
                                    initialValue = value.components,
                                    onValueChanged = { onValueChanged(value.copy(components = it)) }
                                )
                            )
                        }
                    ) {
                        Text(Text.translatable(Texts.SCREEN_CONFIG_ITEM_EDIT_TITLE))
                    }
                }
                val itemFactory = LocalItemFactory.current
                for (subclass in itemFactory.subclasses) {
                    HorizontalPreferenceItem(
                        title = subclass.name,
                    ) {
                        Switch(
                            value = value.subclasses.contains(subclass),
                            onValueChanged = {
                                if (it) {
                                    onValueChanged(value.copy(subclasses = value.subclasses.add(subclass)))
                                } else {
                                    onValueChanged(value.copy(subclasses = value.subclasses.remove(subclass)))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
/**
 * 「物品 → 盾牌」分栏：潜行举盾、举盾弹开攻击者、以及自定义盾牌白名单。
 *
 * <p>这三件事是一套：白名单决定「什么算盾」，潜行举盾决定「什么时候举」，
 * 击退系数决定「举着挡下近战之后把对方弹多远」。放同一页比散在常规设置里好找。</p>
 *
 * <p>右键永远不举盾：举盾只由潜行接管（白名单里的物品也一样）。</p>
 */
class ShieldItemTab(
    override val options: TabOptions,
    private val configScreenModel: ConfigScreenModel,
) : Tab() {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        Column(
            modifier = Modifier
                .padding(8)
                .verticalScroll(background = BackgroundTextures.BRICK_BACKGROUND)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8),
        ) {
            val uiState by configScreenModel.uiState.collectAsState()
            val globalConfig = uiState.config
            SwitchPreferenceItem(
                title = Text.translatable(Texts.SCREEN_CONFIG_GENERAL_REGULAR_SNEAK_SHIELD_TITLE),
                description = Text.translatable(Texts.SCREEN_CONFIG_GENERAL_REGULAR_SNEAK_SHIELD_DESCRIPTION),
                value = globalConfig.tweaks.sneakShield,
                onValueChanged = { value ->
                    configScreenModel.updateConfig { copy(tweaks = tweaks.copy(sneakShield = value)) }
                }
            )
            SwitchPreferenceItem(
                title = Text.translatable(Texts.SCREEN_CONFIG_GENERAL_REGULAR_SHIELD_KNOCKBACK_TITLE),
                description = Text.translatable(Texts.SCREEN_CONFIG_GENERAL_REGULAR_SHIELD_KNOCKBACK_DESCRIPTION),
                value = globalConfig.tweaks.shieldKnockback,
                onValueChanged = { value ->
                    configScreenModel.updateConfig { copy(tweaks = tweaks.copy(shieldKnockback = value)) }
                }
            )
            SwitchPreferenceItem(
                title = Text.translatable(Texts.SCREEN_CONFIG_GENERAL_REGULAR_SHIELD_SPEED_TITLE),
                description = Text.translatable(Texts.SCREEN_CONFIG_GENERAL_REGULAR_SHIELD_SPEED_DESCRIPTION),
                value = globalConfig.tweaks.shieldSpeed,
                onValueChanged = { value ->
                    configScreenModel.updateConfig { copy(tweaks = tweaks.copy(shieldSpeed = value)) }
                }
            )
            HorizontalPreferenceItem(
                title = Text.translatable(Texts.SCREEN_CONFIG_ITEM_SHIELD_WHITELIST_TITLE),
                description = Text.translatable(Texts.SCREEN_CONFIG_ITEM_SHIELD_WHITELIST_DESCRIPTION),
            ) {
                Button(
                    onClick = {
                        navigator?.push(
                            ItemListScreen(
                                initialValue = globalConfig.item.shieldItems.whitelist,
                                onValueChanged = { list ->
                                    configScreenModel.updateConfig {
                                        copy(item = item.copy(shieldItems = item.shieldItems.copy(whitelist = list)))
                                    }
                                },
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
