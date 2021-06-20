package org.tech4c57.bot.module

import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.isAdministrator
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent

class GroupCommands(bot: Bot) : ModuleBase(bot) {
    private val eventChannel = bot.eventChannel.filterIsInstance<MessageEvent>()
    init {
        eventChannel.subscribeAlways<GroupMessageEvent> {
            when(message.contentToString()) {
                "%whoami" -> {
                    val msg = "Crustane Bot 群：${subject.id} " +
                            "管理权限：${subject.botPermission.isAdministrator()}"
                    subject.sendMessage(msg)
                }
            }
        }
    }
}