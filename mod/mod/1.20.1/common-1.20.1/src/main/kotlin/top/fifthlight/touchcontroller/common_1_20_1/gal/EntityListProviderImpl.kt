package top.fifthlight.touchcontroller.common_1_20_1.gal

import net.minecraft.core.registries.BuiltInRegistries
import top.fifthlight.combine.data.Identifier
import top.fifthlight.touchcontroller.common.gal.EntityEntry
import top.fifthlight.touchcontroller.common.gal.EntityListProvider

object EntityListProviderImpl : EntityListProvider {
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
}
