package top.fifthlight.touchcontroller.common_1_20_x.gal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import net.minecraft.client.Minecraft
import top.fifthlight.touchcontroller.common.gal.GameDispatcher
import kotlin.coroutines.CoroutineContext

object GameDispatcherImpl : GameDispatcher() {
    private val client = Minecraft.getInstance()

    override fun isDispatchNeeded(context: CoroutineContext) = !client.isSameThread

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        if (client.isSameThread) {
            Dispatchers.Unconfined.dispatch(context, block)
        } else {
            client.execute(block)
        }
    }
}