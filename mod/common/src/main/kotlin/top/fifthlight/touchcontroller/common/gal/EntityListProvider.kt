package top.fifthlight.touchcontroller.common.gal

import top.fifthlight.combine.data.Identifier

/**
 * 一个实体条目：注册 ID + 游戏内实际显示名（当前语言翻译）。
 *
 * @param stateKey   状态键（例如狼的 `sitting`、羊的 `color:white`）；没有状态维度时为 null。
 *                   匹配时用这个键，和实体实例上的实际状态对比。
 * @param stateLabel 状态的中文名（例如「坐着」），只用于列表显示。
 */
data class EntityEntry(
    val id: Identifier,
    val displayName: String,
    val stateKey: String? = null,
    val stateLabel: String? = null,
)

/**
 * 提供游戏内已注册的全部实体类型列表，
 * 供骑乘实体条件等 UI 做列表选择，而不是让用户手输 ID。
 */
interface EntityListProvider {
    /**
     * 游戏内已注册的全部实体类型（含注册 ID 与游戏内显示名），按 ID 排序。
     * 若当前平台未实现该 Provider，调用方应回退到手动输入。
     *
     * <p>**每个实体类型只有一条**（[EntityEntry.stateKey] 为 null）：状态不再是列表里的独立条目，
     * 而是在已添加的条件上用状态选择器挑，避免列表被狼/羊这类多状态实体撑成几十条。</p>
     */
    fun getAllEntities(): List<EntityEntry>

    /**
     * 某个实体类型的**全部**候选状态（不看当前世界有没有）。
     * 只用于把条件里存的 [state] 键翻译回可读名字。
     */
    fun getAllStates(id: Identifier): List<EntityEntry> = emptyList()

    /**
     * 某个实体类型**此刻真的可选**的状态：扫描当前世界，只保留实际出现过的那些。
     *
     * <p>这就是「自动检测」——世界里没有驯服的狼，就不会给出「驯服·坐着」这种
     * 需要前置条件的状态；羊没被染过色就不会冒出 16 种颜色。</p>
     */
    fun getAvailableStates(id: Identifier): List<EntityEntry> = emptyList()
}
