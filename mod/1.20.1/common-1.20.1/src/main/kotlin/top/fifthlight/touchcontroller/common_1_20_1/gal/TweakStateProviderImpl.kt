package top.fifthlight.touchcontroller.common_1_20_1.gal

import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BowItem
import net.minecraft.world.item.CrossbowItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.ShieldItem
import net.minecraft.world.item.UseAnim
import top.fifthlight.combine.platform_1_20_1.ItemImpl
import top.fifthlight.touchcontroller.common.config.ItemList
import top.fifthlight.touchcontroller.common.gal.TweakStateProvider

/**
 * 1.20.1 上「增强功能」需要的玩家状态判定（蓄力准星 / 潜行举盾共用）。
 *
 * 只读本地玩家当前的状态，不修改任何东西，每渲染帧被问一次，开销可以忽略。
 */
object TweakStateProviderImpl : TweakStateProvider {
    private val client
        get() = Minecraft.getInstance()

    /**
     * 主手或副手拿着可蓄力的武器：弓（BOW）、弩（CROSSBOW）、三叉戟这类投掷（SPEAR）。
     * 用使用动画判断而不是逐个登记物品 id，模组武器只要照抄原版机制就自动生效。
     */
    override fun holdsChargeableItem(): Boolean {
        val player = client.player ?: return false
        return isChargeable(player.mainHandItem) || isChargeable(player.offhandItem)
    }

    /**
     * 正在使用「蓄力类使用动画」的物品：弓（BOW）、弩（CROSSBOW）、三叉戟这类投掷（SPEAR）。
     */
    override fun isChargingChargeableItem(): Boolean {
        val player = client.player ?: return false
        if (!player.isUsingItem) {
            return false
        }
        return isChargeable(player.useItem)
    }

    /**
     * 主手或副手有「装弹完成」的弩：弦已上好、弹药就位，一按就能射。
     * 用原版记在物品上的 Charged 标记判断，照抄这套机制的模组弩也能认出来。
     */
    override fun hasLoadedCrossbow(): Boolean {
        val player = client.player ?: return false
        return isLoadedCrossbow(player.mainHandItem) || isLoadedCrossbow(player.offhandItem)
    }

    /** 主手或副手拿着盾牌。原版盾用物品 id 认，模组盾只要照抄原版的 BLOCK 使用动画也能认出来。 */
    override fun hasShieldOnHand(): Boolean {
        val player = client.player ?: return false
        return isShield(player.mainHandItem) || isShield(player.offhandItem)
    }

    /**
     * 主手或副手有没有「现在就能蓄力 / 射击」的弓弩：
     * 弓要有箭、弩要已上弦或有箭、三叉戟可以直接蓄力投掷（不需要弹药）。
     *
     * <p>拿弓就是换战斗姿态：这时候要让位给拉弓，不能被潜行举盾顶掉。</p>
     */
    override fun hasUsableChargeableWeapon(): Boolean {
        val player = client.player ?: return false
        return isUsableChargeable(player, player.mainHandItem) ||
            isUsableChargeable(player, player.offhandItem)
    }

    private fun isUsableChargeable(player: LocalPlayer, stack: ItemStack): Boolean {
        if (stack.isEmpty) {
            return false
        }
        return when {
            stack.item is CrossbowItem -> CrossbowItem.isCharged(stack) || hasRangedAmmo(player, stack)
            stack.item is BowItem -> hasRangedAmmo(player, stack)
            stack.useAnimation == UseAnim.SPEAR -> true
            else -> false
        }
    }

    /** 主手拿着盾牌：长按「用物品」先打到主手，只有主手是盾才需要拦（副手放盾不影响主手）。 */
    override fun hasShieldInMainHand(): Boolean {
        val player = client.player ?: return false
        return isShield(player.mainHandItem)
    }

    /**
     * 手上（主手或副手）的东西此刻能不能真的用出去。
     *
     * 只有食物和原版弓弩是能提前预判的：吃饱了吃不下、弓弩没弹药也没上弦，就是确定用不出去；
     * 其他物品（模组枪械等）返回 true，交给 View 里「长按 0.3 秒还没开始使用」的兜底规则。
     */
    override fun canUseHeldItemNow(): Boolean {
        val player = client.player ?: return true
        val main = judgeHand(player, player.mainHandItem)
        val off = judgeHand(player, player.offhandItem)
        // 一只手确定用不出去、另一只手也没有「现在就能用」的东西 → 整体算用不出去。
        // 另一只手明确能用（比如副手拿着上膛的弩）时，就不要去打断它。
        if (main == false && off != true) {
            return false
        }
        if (off == false && main != true) {
            return false
        }
        return true
    }

    /** 正在吃（使用动画是 EAT 的物品：面包、熟肉这些）。拿着不算。 */
    override fun isEatingFood(): Boolean {
        val player = client.player ?: return false
        if (!player.isUsingItem) {
            return false
        }
        return player.useItem.useAnimation == UseAnim.EAT
    }

    /** 正在吃的这一口是「靠回饥饿值」的普通食物，而不是金苹果这类满饥饿也能吃的。 */
    override fun isEatingFillingFood(): Boolean {
        val player = client.player ?: return false
        if (!player.isUsingItem) {
            return false
        }
        val stack = player.useItem
        if (stack.isEmpty || stack.useAnimation != UseAnim.EAT) {
            return false
        }
        // 1.20.1 没有 ItemStack#getFoodProperties(...)（那是 1.20.5+ 的 API）。
        // 普通食物只有在饥饿值没满时才吃得下，所以「正在吃 + 饥饿值还没满」
        // 就说明这是靠回饥饿值的普通食物；满饥饿还能吃的（金苹果）此刻 foodLevel 已是 20。
        return player.foodData.foodLevel < 20
    }

    /** 饥饿值是不是已经到顶（10 颗心 = 20 点）。 */
    override fun isFoodLevelFull(): Boolean {
        val player = client.player ?: return false
        return player.foodData.foodLevel >= 20
    }

    /**
     * 手上这件「要用的东西」是不是在黑名单里。
     *
     * 正在使用物品时看「正在用的那件」（玩家真正在吃的东西），否则看主手——
     * 长按要用的是主手这件，副手的东西不该让它逃过收手。
     */
    override fun isActiveItemIn(list: ItemList): Boolean {
        val player = client.player ?: return false
        val stack = if (player.isUsingItem) player.useItem else player.mainHandItem
        if (stack.isEmpty) {
            return false
        }
        return ItemImpl(stack.item) in list
    }

    /**
     * 主手这件「可长按使用的物品」是否在清单里。
     *
     * <p>只看主手：长按方块时原版先动主手，副手那件轮不到。
     * <p>盾牌内置黑名单：右键举盾由潜行接管，盾牌留在清单里会让
     * 「主手方块 + 副手盾」长按被判成用物品，先放方块再挖。
     */
    override fun isHeldFoodItem(): Boolean {
        val player = client.player ?: return false
        return player.mainHandItem.isEdible || player.offhandItem.isEdible
    }

    override fun isHeldItemInWhitelist(list: ItemList): Boolean {
        val player = client.player ?: return false
        val stack = player.mainHandItem
        if (stack.isEmpty) {
            return false
        }
        val item = ItemImpl(stack.item)
        return list.whitelist.any { it.matches(item) }
    }

    override fun hasUsableItemOnMainHand(list: ItemList): Boolean {
        val player = client.player ?: return false
        val stack = player.mainHandItem
        if (stack.isEmpty) {
            return false
        }
        if (isShield(stack)) {
            return false
        }
        // 能蓄力使用的物品（弓、三叉戟、望远镜、月光蠕虫女王这类 useDuration > 0 的），
        // 长按就该「用」而不是挖/放方块——哪怕它不在清单里。
        // 食物不在这里处理：吃饱了要能改判挖方块，那是 judgeHand 那套逻辑管的。
        if (stack.useDuration > 0 && !stack.isEdible) {
            return true
        }
        return ItemImpl(stack.item) in list
    }

    /**
     * 主手拿着「能长按使用」的物品：有使用时长（useDuration > 0）或带使用动画。
     *
     * <p>这类物品长按永远最高优先级走「用」——弓、三叉戟、望远镜、月光蠕虫女王。
     * 不看 usableItems 清单、不看 canUseHeldItemNow、也不受「长按用不了改挖方块」影响，
     * 否则它们会被原版当成可放置物品，长按先放出去一块。</p>
     */
    override fun isLongPressUsableItem(): Boolean {
        val player = client.player ?: return false
        val stack = player.mainHandItem
        if (stack.isEmpty) {
            return false
        }
        if (isShield(stack)) {
            return false
        }
        // 食物不归这条管：吃饱了要能改判挖方块，走 canEatHeldFood / judgeHand 那套。
        if (stack.isEdible) {
            return false
        }
        // 只认「真的有蓄力时长」的物品（弓 72000、月光蠕虫女王 FIRING_TIME…）。
        // 以前这里还多一条 `useAnimation != UseAnim.NONE`，把一大堆「点一下就生效」的模组物品
        // （孔雀羽扇这种）也算了进来，结果它们长按会被拦掉右键、又启动不了蓄力，
        // 表现就是「右键物品全失效、图层按钮点了也没反应」。那种物品必须留给原版处理。
        return stack.useDuration > 0
    }

    /**
     * 长按用不出去时「不改判挖方块」的物品。
     *
     * <p>清单完全留空时使用内置默认：金苹果、附魔金苹果、三种药水——
     * 它们本来就该「吃不饱也按着用」，长按用不出去时保持空按，不要变成挖方块。
     * 这样老配置（还没有这个字段）也能得到同样的默认保护。</p>
     */
    override fun isLongPressDigExempt(list: ItemList): Boolean {
        val player = client.player ?: return false
        val stack = if (player.isUsingItem) player.useItem else player.mainHandItem
        if (stack.isEmpty) {
            return false
        }
        val untouched = list.whitelist.isEmpty() &&
            list.blacklist.isEmpty() &&
            list.components.isEmpty() &&
            list.subclasses.isEmpty()
        if (untouched) {
            return stack.item in BUILT_IN_LONG_PRESS_DIG_EXEMPT
        }
        val combined = ItemImpl(stack.item)
        // 黑名单命中也算豁免（界面上的「黑名单」就是这个字段）；
        // 白名单/组件/子成分照旧走 contains。
        return combined in list || list.blacklist.any { it.matches(combined) }
    }

    /**
     * 手上是不是「现在就能吃下去的食物」：可食用 + 此刻真的吃得下。
     *
     * <p>「吃得下」直接问原版的 {@code Player#canEat(canAlwaysEat)}，和物品自己的
     * {@code Item#use} 完全同一套判定，于是这两种情况都自动包含进来：</p>
     * <ul>
     *   <li><b>创造 / 旁观</b>：{@code abilities.invulnerable} 为真，什么食物都吃得下；</li>
     *   <li><b>附魔金苹果这类 {@code canAlwaysEat}</b>：吃饱了也照样能吃。</li>
     * </ul>
     *
     * <p>这条判断故意**不依赖**「可长按使用的物品」清单——
     * 老配置里的那份清单可能没勾食物组件，靠它判断会把「能吃」误当成「用不了」而去挖方块。</p>
     */
    override fun isHeldFoodEdibleNow(): Boolean {
        val player = client.player ?: return false
        return isEdibleNow(player, player.mainHandItem) || isEdibleNow(player, player.offhandItem)
    }

    /** 这一件「现在就能吃下去」：跟原版 {@code LivingEntity#canEat} 用同一条判定。 */
    private fun isEdibleNow(player: LocalPlayer, stack: ItemStack): Boolean {
        if (stack.isEmpty || !stack.isEdible) {
            return false
        }
        return player.canEat(canAlwaysEat(stack))
    }

    /**
     * 这件食物是不是「吃饱了也能吃」（金苹果 / 附魔金苹果 / 药水…）。
     *
     * <p>1.20.1 里食物属性挂在 {@code Item} 上，{@code ItemStack} 自己没有无参的
     * {@code getFoodProperties()}（带实体参数的那个是 Forge 补丁，common 模块看不见），
     * 所以走 {@code stack.item.foodProperties}。</p>
     */
    private fun canAlwaysEat(stack: ItemStack): Boolean =
        stack.item.foodProperties?.canAlwaysEat() == true

    /**
     * 这只手的物品是不是「现在就能用出去」：
     * `null` 表示不适用（空手、不是食物/弓弩，交给 0.3 秒兜底）。
     */
    private fun judgeHand(player: LocalPlayer, stack: ItemStack): Boolean? {
        if (stack.isEmpty) {
            return null
        }
        if (stack.isEdible) {
            // 能不能吃现在就要给明确的答案，**不能返回 null**：
            // null 会被上层当成「能用」，长按就一直按着 USE 键，
            // 甜浆果 / 发光浆果这类「可放置的食物」就会被原版误当成方块放出去。
            // 判定跟原版 LivingEntity#canEat 完全一致：吃饱了吃不下 → false → 这一下不走「用物品」，
            // 改判挖方块（或在例外清单里保持空按）；创造模式、附魔金苹果这类则永远是 true。
            // （1.20.1 的食物属性在 Item 上，ItemStack 没有无参 getFoodProperties，
            //  带实体参数的那个重载是 Forge 补丁，跨 loader 的 common 模块用不了。）
            return player.canEat(canAlwaysEat(stack))
        }
        if (stack.item is CrossbowItem) {
            return CrossbowItem.isCharged(stack) || hasRangedAmmo(player, stack)
        }
        if (stack.item is BowItem) {
            return hasRangedAmmo(player, stack)
        }
        return null
    }

    /** 背包里有没有能喂给这把弓/弩的弹药；创造模式不用弹药也能拉弓上弦。 */
    private fun hasRangedAmmo(player: Player, stack: ItemStack): Boolean {
        return player.abilities.instabuild || !player.getProjectile(stack).isEmpty
    }

    /** 「长按用不出去也不改判挖方块」的内置默认清单（清单留空时生效）：金苹果、附魔金苹果、三种药水。 */
    private val BUILT_IN_LONG_PRESS_DIG_EXEMPT = setOf(
        Items.GOLDEN_APPLE,
        Items.ENCHANTED_GOLDEN_APPLE,
        Items.POTION,
        Items.SPLASH_POTION,
        Items.LINGERING_POTION,
    )

    private fun isChargeable(stack: ItemStack): Boolean {
        if (stack.isEmpty) {
            return false
        }
        return when (stack.useAnimation) {
            UseAnim.BOW, UseAnim.CROSSBOW, UseAnim.SPEAR -> true
            else -> false
        }
    }

    private fun isLoadedCrossbow(stack: ItemStack): Boolean {
        if (stack.isEmpty) {
            return false
        }
        val crossbowLike = stack.item is CrossbowItem || stack.useAnimation == UseAnim.CROSSBOW
        return crossbowLike && CrossbowItem.isCharged(stack)
    }

    /**
     * 是不是盾牌。
     *
     * <p>只认 {@code ShieldItem}（原版盾 + 继承它的模组盾）。以前还带一条
     * {@code useAnimation == UseAnim.BLOCK}，把用格挡姿势的右键物品（孔雀羽扇之类）
     * 全当成了盾牌，长按直接判成「不适用物品」，表现就是右键没反应。</p>
     */
    private fun isShield(stack: ItemStack): Boolean = ShieldWhitelist.matches(stack)
}
