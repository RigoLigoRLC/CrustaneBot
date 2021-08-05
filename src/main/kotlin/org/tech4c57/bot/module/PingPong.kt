package org.tech4c57.bot.module

import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.events.MessageEvent
import org.tech4c57.bot.Foundation

class PingPong(bot: Foundation) : ModuleBase(bot) {
    override fun commandName(): String {
        return "ping"
    }

    override suspend fun execCommand(args: List<String>, evt: MessageEvent) {
        evt.subject.sendMessage("Pong!")
    }
}