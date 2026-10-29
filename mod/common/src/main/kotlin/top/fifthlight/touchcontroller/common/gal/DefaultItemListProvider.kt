package top.fifthlight.touchcontroller.common.gal

import top.fifthlight.touchcontroller.common.config.ItemList

interface DefaultItemListProvider {
    val usableItems: ItemList
    val showCrosshairItems: ItemList
    val crosshairAimingItems: ItemList

    /** 「长按用不了改挖方块」的例外清单（金苹果、附魔金苹果、药水）。没实现的版本返回空清单。 */
    val longPressDigItems: ItemList get() = ItemList()
}