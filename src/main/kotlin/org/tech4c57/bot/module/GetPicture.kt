package org.tech4c57.bot.module

import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import net.mamoe.mirai.contact.getMember
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.*
import org.bson.BSONObject
import org.bson.Document
import org.tech4c57.bot.Foundation
import org.tech4c57.bot.foundation.BotDatabase
import org.tech4c57.bot.permissions.CmdPermission
import org.tech4c57.bot.utils.ensureParamCount
import org.tech4c57.bot.utils.replyPlainMsg
import java.time.Instant
import java.time.ZoneId

data class FlashImageRecord(
    val group: Long,
    val image: String,
    val sender: Long,
    val time: Long
)

class GetPicture(foundation: Foundation): ModuleBase(foundation) {
    val store = BotDatabase.db.getCommandCollection("flash_image")

    override fun commandName(): String {
        return "getpic"
    }

    override suspend fun execCommand(param: List<String>, evt: MessageEvent) {
        when(evt) {
            is GroupMessageEvent -> {
                if(ensureParamCount(evt, param.size == 1)) {
                    when(param[0]) {
                        "see" -> {
                            val result = store.aggregate<FlashImageRecord>(listOf(
                                Aggregates.match(Filters.eq("group", evt.subject.id)),
                                Aggregates.sort(Sorts.descending("time")),
                                Aggregates.limit(5)
                            ))
                            val cursor = result.toList().iterator()

                            if(cursor.hasNext()) {
                                val queryMsg = MessageChainBuilder()
                                queryMsg.add("这个群中最近发送的闪照的记录有：\n")
                                while(cursor.hasNext()) {
                                    val item = cursor.next()
                                    queryMsg.add(">" +
                                            Instant.ofEpochSecond(item.time)
                                                .atZone(ZoneId.of("UTC+8")).toLocalDateTime().toString() +
                                            " ${evt.subject.getMember(item.sender)?.nick ?: "（用户不存在）"}" +
                                            "（${item.sender}）" +
                                            "，ID=${item.image}\n")
                                }

                                evt.subject.sendMessage(queryMsg.build())
                            } else {
                                evt.subject.sendMessage("这个群还没有闪照记录。")
                            }
                        }
                    }
                }
            }

            is FriendMessageEvent -> {
                Foundation.tellOwner(buildMessageChain {
                    +Image(param[0])
                })
            }
        }
    }

    override val hasMessageFilter: Boolean = true

    override suspend fun messageFilter(evt: MessageEvent): Boolean {
        return evt is GroupMessageEvent && evt.message.filterIsInstance<FlashImage>().isNotEmpty()
    }

    override suspend fun filteredMessageHandler(evt: MessageEvent) {
        evt.message.filterIsInstance<FlashImage>().forEach {
            store.insertOne(Document(
                mapOf(
                    Pair("group", evt.subject.id),
                    Pair("sender", evt.sender.id),
                    Pair("time", evt.time),
                    Pair("image", it.image.imageId)
                )
            ))
        }
    }
}