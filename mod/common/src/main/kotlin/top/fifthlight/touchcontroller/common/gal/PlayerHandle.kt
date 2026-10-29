package top.fifthlight.touchcontroller.common.gal

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import top.fifthlight.combine.data.Identifier
import top.fifthlight.combine.data.Item
import top.fifthlight.combine.data.ItemStack
import top.fifthlight.touchcontroller.common.config.ItemList

enum class RidingEntityType {
    MINECART,
    BOAT,
    PIG,
    HORSE,
    LLAMA,
    CAMEL,
    STRIDER,
    OTHER,
}

data class PlayerInventory(
    val main: PersistentList<ItemStack>,
    val armor: PersistentList<ItemStack>,
    val offHand: ItemStack?,
    // Curios 饰品栏物品（暮色森林/天境等走 Curios API 的模组饰品）。
    // 未安装 Curios 时为空列表；由各平台实现层通过反射软填充，common 层不直接依赖 Curios。
    val curios: PersistentList<ItemStack> = persistentListOf(),
)

interface PlayerHandle {
    fun matchesItemOnHand(item: Item): Boolean
    fun hasItemsOnHand(list: ItemList): Boolean
    fun changeLookDirection(deltaYaw: Double, deltaPitch: Double)
    var currentSelectedSlot: Int
    fun dropSlot(index: Int)
    fun getInventorySlot(index: Int): ItemStack
    fun getInventory(): PlayerInventory
    val isUsingItem: Boolean
    val onGround: Boolean
    var isFlying: Boolean
    val isSubmergedInWater: Boolean
    val isTouchingWater: Boolean
    var isSprinting: Boolean
    val isSneaking: Boolean
    val ridingEntityType: RidingEntityType?
    val ridingEntityId: Identifier?
    val canFly: Boolean

    /**
     * 准星（触控圈）瞄准到的实体 id。
     *
     * <p>松手后不会立刻变空：会一直记着刚才瞄到的那个实体，直到它离开被瞄准时的位置
     * （或消失）为止。图层条件「瞄准指定实体」就靠它。</p>
     */
    val targetedEntityId: Identifier?
        get() = null

    /**
     * 锁定目标的当前状态键：`sitting`（坐着的狼）、`sleeping`（躺着的村民）、
     * `color:white`（白羊）…；没有状态维度或没有锁定时为 null。
     */
    val targetedEntityState: String?
        get() = null
}

interface PlayerHandleFactory {
    fun getPlayerHandle(): PlayerHandle?
}