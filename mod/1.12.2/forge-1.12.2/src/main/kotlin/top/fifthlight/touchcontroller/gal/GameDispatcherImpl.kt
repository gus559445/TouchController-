package top.fifthlight.touchcontroller.gal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import net.minecraft.client.Minecraft
import top.fifthlight.touchcontroller.common.gal.GameDispatcher
import kotlin.coroutines.CoroutineContext

object GameDispatcherImpl : GameDispatcher() {
    private val client = Minecraft.getMinecraft()

    override fun isDispatchNeeded(context: CoroutineContext) = !client.isCallingFromMinecraftThread

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        if (client.isCallingFromMinecraftThread) {
            Dispatchers.Unconfined.dispatch(context, block)
        } else {
            client.addScheduledTask(block)
        }
    }
}