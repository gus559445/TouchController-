package top.fifthlight.touchcontroller.common.gal

import top.fifthlight.combine.data.Identifier

/**
 * 一个已注册实体类型的条目：注册 ID + 游戏内实际显示名（当前语言翻译）。
 */
data class EntityEntry(
    val id: Identifier,
    val displayName: String,
)

/**
 * 提供游戏内已注册的全部实体类型列表，
 * 供骑乘实体条件等 UI 做列表选择，而不是让用户手输 ID。
 */
interface EntityListProvider {
    /**
     * 游戏内已注册的全部实体类型（含注册 ID 与游戏内显示名），按 ID 排序。
     * 若当前平台未实现该 Provider，调用方应回退到手动输入。
     */
    fun getAllEntities(): List<EntityEntry>
}
