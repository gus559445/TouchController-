package top.fifthlight.touchcontroller.common_1_20_1.gal

import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ShieldItem
import top.fifthlight.combine.platform_1_20_1.ItemImpl
import top.fifthlight.touchcontroller.common.config.ItemList

/**
 * 「这个物品算不算盾牌」的唯一判定入口。
 *
 * <p>三条来源，命中任意一条就算盾牌：</p>
 * <ol>
 *   <li>原版盾类（{@link ShieldItem}）；</li>
 *   <li>加载器给的标准钩子——由 forge 模块在启动时注册进来（{@code SHIELD_BLOCK} 动作），
 *       这样模组自带的真盾也认得出来；</li>
 *   <li>玩家在「物品 → 盾牌」里的白名单——有些模组道具只是长得像盾、或者想借用潜行举盾的手感，
 *       不一定真的注册了格挡动作，那就由玩家点名。</li>
 * </ol>
 *
 * <p>这个模块是跨加载器共用的，不能直接引用 {@code net.minecraftforge}，
 * 所以第二条走 [extraCheck] 回调，由加载器自己注册。</p>
 */
object ShieldWhitelist {
    @Volatile
    private var items: ItemList = ItemList()

    /** 加载器侧的额外判定（forge 里是 {@code SHIELD_BLOCK} 动作）。没有就是 null。 */
    @Volatile
    var extraCheck: ((ItemStack) -> Boolean)? = null

    /** 配置改动后推一次（每帧推也无所谓，赋值本身很便宜）。 */
    fun update(list: ItemList) {
        items = list
    }

    /** [stack] 算不算盾牌。 */
    fun matches(stack: ItemStack?): Boolean {
        if (stack == null || stack.isEmpty) {
            return false
        }
        if (stack.item is ShieldItem) {
            return true
        }
        if (extraCheck?.invoke(stack) == true) {
            return true
        }
        return ItemImpl(stack.item) in items
    }
}
