package org.tech4c57.bot.utils

import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.QuoteReply
import net.mamoe.mirai.message.data.buildMessageChain

suspend fun replyPlainMsg(evt: MessageEvent, msg: String) {
    evt.subject.sendMessage(
        buildMessageChain {
            +QuoteReply(evt.source)
            +PlainText(msg)
        }
    )
}

suspend fun ensureParamCount(evt: MessageEvent, expr: Boolean): Boolean {
    if(!expr) {
        evt.subject.sendMessage(
            buildMessageChain {
                +QuoteReply(evt.source)
                +PlainText("操作参数个数错误。")
            }
        )
        return false
    }
    return true
}