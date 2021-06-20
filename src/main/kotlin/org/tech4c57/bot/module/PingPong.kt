package org.tech4c57.bot.module

import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.GroupTempMessageEvent
import net.mamoe.mirai.event.events.MessageEvent

class PingPong(bot: Bot) : ModuleBase(bot) {
    private val eventChannel = bot.eventChannel.filterIsInstance<MessageEvent>()
    init {
        eventChannel.subscribeAlways<GroupMessageEvent> {
            if(message.serializeToMiraiCode() == "[mirai:at:1776526885] ping")
                subject.sendMessage("Pong!")
        }
        eventChannel.subscribeAlways<GroupTempMessageEvent> {
            if(message.contentToString() == "%ping")
                subject.sendMessage("Pong!")
        }
    }
}