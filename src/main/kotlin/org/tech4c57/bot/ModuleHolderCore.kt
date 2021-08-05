package org.tech4c57.bot

import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.GroupTempMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import org.tech4c57.bot.module.ModuleBase
import java.util.*

object ModuleHolderCore {
    enum class SubscribeTarget(val bit: Int) {
        Invalid(0),
        FriendMsg(1),
        GroupMsg(2),
        TemporaryMsg(4)
    }

    var cmdIdList: MutableMap<String, Pair<EnumSet<SubscribeTarget>, ModuleBase>> = mutableMapOf()

    fun registerModule(module: ModuleBase, target: EnumSet<SubscribeTarget>): Boolean {
        return if(module.commandName() in cmdIdList || target.isEmpty())
            false
        else {
            cmdIdList[module.commandName()] = Pair(target, module)
            true
        }
    }

    private suspend fun msgHandler(e: MessageEvent, e1: MessageEvent): Unit {
        val sourceType: SubscribeTarget = when (e) {
            is GroupMessageEvent -> SubscribeTarget.GroupMsg
            is FriendMessageEvent -> SubscribeTarget.FriendMsg
            is GroupTempMessageEvent -> SubscribeTarget.TemporaryMsg
            else -> SubscribeTarget.Invalid
        }
        val cmd = CmdParser.parse(e.message.serializeToMiraiCode())
        val modulePair = cmdIdList[cmd.commandName] ?: return
        if(modulePair.first.removeAll(listOf(sourceType)))
            modulePair.second.execCommand(cmd.params, e)
    }

    fun subscribe(bot: Bot) {
        bot.eventChannel.subscribeAlways<MessageEvent> (handler = ::msgHandler)
    }


}