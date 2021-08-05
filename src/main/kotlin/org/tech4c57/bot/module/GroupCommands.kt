package org.tech4c57.bot.module

import net.mamoe.mirai.contact.isAdministrator
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import org.tech4c57.bot.Foundation

class GroupCommands(bot: Foundation) : ModuleBase(bot) {
    override fun commandName(): String {
        return "whoami"
    }

    override suspend fun execCommand(param: List<String>, evt: MessageEvent) {
        when (evt) {
            is GroupMessageEvent -> {
                val msg = "Crustane Bot 群：${evt.subject.id} " + "管理权限：${evt.subject.botPermission.isAdministrator()}"
                evt.subject.sendMessage(msg)
                evt.subject.name
            }

            else -> {
                return
            }
        }
    }
}
