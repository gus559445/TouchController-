package top.fifthlight.touchcontroller.common_1_20_1.gal

import net.minecraft.client.Minecraft
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EntityType
import top.fifthlight.combine.data.Identifier
import top.fifthlight.touchcontroller.common.gal.EntityEntry
import top.fifthlight.touchcontroller.common.gal.EntityListProvider

object EntityListProviderImpl : EntityListProvider {

    /**
     * 全部已注册实体类型，**每个类型一条**（不再按状态展开）。
     *
     * <p>状态改成在「已添加的条件」上用选择器挑：狼、羊这种多状态实体会把列表撑到几十条，
     * 而且大部分状态（驯服的、某个颜色的）玩家当时根本不关心。</p>
     */
    override fun getAllEntities(): List<EntityEntry> =
        BuiltInRegistries.ENTITY_TYPE.keySet()
            .mapNotNull { resourceLocation ->
                val type = BuiltInRegistries.ENTITY_TYPE.get(resourceLocation)
                    ?: return@mapNotNull null
                val id = Identifier("${resourceLocation.namespace}:${resourceLocation.path}")
                // type.description 是可翻译组件，.string 按当前客户端语言解析成实际名字
                val displayName = runCatching { type.description.string }.getOrDefault("")
                EntityEntry(id = id, displayName = displayName)
            }
            .sortedBy { it.id.toString() }

    override fun getAllStates(id: Identifier): List<EntityEntry> {
        val type = entityTypeOf(id) ?: return emptyList()
        val displayName = runCatching { type.description.string }.getOrDefault("")
        return EntityStateHelper.statesFor(type).map { (key, label) ->
            EntityEntry(id = id, displayName = displayName, stateKey = key, stateLabel = label)
        }
    }

    /**
     * 可选状态：**全部候选都返回**，不依赖世界。
     *
     * <p>之前是扫世界里实际出现的状态，结果「没进世界 / 旁边没这种生物」时一个都选不到，
     * 主界面里配置预设更是完全没法用。状态本来就是描述「这一类生物可以处于什么状态」，
     * 跟此刻世界里有没有它无关，所以这里直接给全表。</p>
     */
    override fun getAvailableStates(id: Identifier): List<EntityEntry> = getAllStates(id)

    /** 注册 ID → 实体类型；取不到（模组卸载、手输错了）就返回 null。 */
    private fun entityTypeOf(id: Identifier): EntityType<*>? {
        val resourceLocation = ResourceLocation.tryParse(id.toString()) ?: return null
        return BuiltInRegistries.ENTITY_TYPE.get(resourceLocation)
    }
}
