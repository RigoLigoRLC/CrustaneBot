package org.tech4c57.bot.module;

import net.mamoe.mirai.Bot;
import net.mamoe.mirai.event.events.MessageEvent
import org.tech4c57.bot.Foundation
import kotlin.system.exitProcess

class EmergencyStop(bot: Foundation, owner: Long) : ModuleBase(bot) {
    private val ownerId: Long = owner

    override fun commandName(): String {
        return "terminate"
    }

    override suspend fun execCommand(param: List<String>, evt: MessageEvent) {
        if(evt.sender.id == ownerId) {
            evt.subject.sendMessage("Crustane已被所有者停止。")
            evt.sender.sendMessage("Crustane已被所有者停止。")
            evt.bot.close(Throwable("Owner has forcibly stopped the bot."))
            exitProcess(1)
        } else {
            evt.subject.sendMessage("Crustane只能被所有者停止。")
        }
    }
}
