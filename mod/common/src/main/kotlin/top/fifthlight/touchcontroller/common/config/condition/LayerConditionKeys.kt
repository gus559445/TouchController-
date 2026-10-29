package top.fifthlight.touchcontroller.common.config.condition

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import top.fifthlight.combine.data.Identifier
import top.fifthlight.combine.data.Item
import top.fifthlight.touchcontroller.assets.Texts
import top.fifthlight.touchcontroller.common.ext.ItemSerializer
import top.fifthlight.touchcontroller.common.layout.ContextInput
import kotlin.uuid.Uuid

@Serializable
@SerialName("custom")
data class CustomLayerConditionKey(
    val key: Uuid,
) : LayerConditions.Key {
    override fun isFulfilled(input: ContextInput) = key in input.customCondition
}

@Serializable
@SerialName("holding_item")
data class HoldingItemConditions(
    @Serializable(with = ItemSerializer::class)
    val item: Item,
) : LayerConditions.Key {
    override fun isFulfilled(input: ContextInput) = input.playerHandle?.matchesItemOnHand(item) ?: false
}

@Serializable
@SerialName("wearing_item")
data class WearingItemConditions(
    @Serializable(with = ItemSerializer::class)
    val item: Item,
) : LayerConditions.Key {
    override fun isFulfilled(input: ContextInput) =
        input.playerHandle?.getInventory()?.let { inv ->
            inv.armor.any { item.matches(it.item) } || inv.curios.any { item.matches(it.item) }
        } ?: false
}

@Serializable
@SerialName("riding_entity")
data class RidingEntityConditions(
    val entity: Identifier,
) : LayerConditions.Key {
    override fun isFulfilled(input: ContextInput) =
        input.ridingEntityId == entity
}

/**
 * 「瞄准指定实体」：准星（触控圈）瞄到 [entity] 这种实体时就成立。
 *
 * <p>[state] 为空表示不限状态；填了的话还要求实体当前状态对得上
 * （`sitting` 坐着的狼、`sleeping` 躺着的村民、`color:white` 白羊…）。
 * 幼年和基础状态可以叠加，写成 `baby:&lt;基础状态&gt;`
 * （`baby:tamed_sitting` = 幼年且驯服坐着）；单独的 `baby` 表示「只要是幼年就行」，
 * 它会同时匹配所有 `baby:…`。</p>
 *
 * <p>松开手指不会立刻失效——平台层会把瞄到的那个实体锁住，直到它离开被瞄准时的位置
 * （或消失）才放开，所以「瞄一下再松手」这段时间图层一直保持启用。</p>
 */
@Serializable
@SerialName("targeted_entity")
data class TargetedEntityConditions(
    val entity: Identifier,
    val state: String? = null,
) : LayerConditions.Key {
    override fun isFulfilled(input: ContextInput): Boolean {
        if (input.targetedEntityId != entity) {
            return false
        }
        val wanted = state ?: return true
        // 「穿戴指定装备」：状态键写成 armor:<物品注册ID>，按目标实体穿着的盔甲槽匹配。
        if (wanted.startsWith(ARMOR_PREFIX)) {
            val itemId = wanted.removePrefix(ARMOR_PREFIX)
            return input.targetedEntityArmor.any { it.toString() == itemId }
        }
        val current = input.targetedEntityState ?: return false
        if (current == wanted) {
            return true
        }
        // 只选了「幼年」时，幼年狼 / 幼年白羊 / 幼年村民 都算命中。
        return wanted == BABY_STATE && current.startsWith(BABY_STATE)
    }

    companion object {
        /** 不带基础状态的「幼年」状态键；组合状态一律以 [BABY_STATE] + ":" 开头。 */
        const val BABY_STATE = "baby"

        /** 拼出「幼年 + 某个基础状态」的组合键。 */
        fun babyOf(baseState: String): String = "$BABY_STATE:$baseState"

        /** 「穿戴指定装备」的状态前缀；后面接物品注册 ID（如 armor:minecraft:diamond_helmet）。 */
        const val ARMOR_PREFIX = "armor:"

        /** 由物品注册 ID 拼出穿戴状态键。 */
        fun armorOf(itemId: String): String = "$ARMOR_PREFIX$itemId"
    }
}

@Serializable
@SerialName("builtin")
data class BuiltinLayerConditionKey(
    val key: Key,
) : LayerConditions.Key {
    @Serializable
    enum class Key(val text: Identifier) {
        @SerialName("swimming")
        SWIMMING(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_CONDITIONS_SWIMMING),

        @SerialName("underwater")
        UNDERWATER(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_CONDITIONS_UNDERWATER),

        @SerialName("flying")
        FLYING(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_CONDITIONS_FLYING),

        @SerialName("can_fly")
        CAN_FLY(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_CONDITIONS_CAN_FLY),

        @SerialName("sneaking")
        SNEAKING(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_CONDITIONS_SNEAKING),

        @SerialName("sprinting")
        SPRINTING(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_CONDITIONS_SPRINTING),

        @SerialName("on_ground")
        ON_GROUND(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_CONDITIONS_ON_GROUND),

        @SerialName("no_on_ground")
        NOT_ON_GROUND(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_CONDITIONS_NOT_ON_GROUND),

        @SerialName("using_item")
        USING_ITEM(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_CONDITIONS_USING_ITEM),

        @SerialName("on_minecart")
        ON_MINECART(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_CONDITIONS_ON_MINECART),

        @SerialName("on_boat")
        ON_BOAT(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_CONDITIONS_ON_BOAT),

        @SerialName("on_pig")
        ON_PIG(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_CONDITIONS_ON_PIG),

        @SerialName("on_horse")
        ON_HORSE(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_CONDITIONS_ON_HORSE),

        @SerialName("on_camel")
        ON_CAMEL(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_CONDITIONS_ON_CAMEL),

        @SerialName("on_llama")
        ON_LLAMA(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_CONDITIONS_ON_LLAMA),

        @SerialName("on_strider")
        ON_STRIDER(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_CONDITIONS_ON_STRIDER),

        @SerialName("riding")
        RIDING(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_CONDITIONS_RIDING),

        @SerialName("block_selected")
        BLOCK_SELECTED(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_CONDITIONS_BLOCK_SELECTED),
    }

    override fun isFulfilled(input: ContextInput) = key in input.builtInCondition
}
