package org.tech4c57.bot.module

import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.GroupTempMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import org.tech4c57.bot.CmdParser

class PingPong(bot: Bot) : ModuleBase(bot) {
    private val eventChannel = bot.eventChannel.filterIsInstance<MessageEvent>()
    init {
        eventChannel.subscribeAlways<GroupMessageEvent> {
            if(message.serializeToMiraiCode() == "[mirai:at:${bot.id}] ping")
                subject.sendMessage("Pong!")
        }
        eventChannel.subscribeAlways<GroupTempMessageEvent> {
            if(CmdParser.parseZeroArg(message.contentToString()).commandName == "ping")
                subject.sendMessage("Pong!")
        }
        eventChannel.subscribeAlways<FriendMessageEvent> {
            if(CmdParser.parseZeroArg(message.contentToString()).commandName == "ping")
                subject.sendMessage("Pong!")
        }
    }
}