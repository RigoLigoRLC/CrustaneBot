package org.tech4c57.bot.module

import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.contact.isOperator
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.nextEventOrNull
import net.mamoe.mirai.message.data.FileMessage
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.MessageSource.Key.recall

class GroupTempFileClean(private val bot: Bot) : ModuleBase(bot) {
    private val channel = bot.eventChannel.filterIsInstance<GroupMessageEvent>()

    val matches: Array<String> = arrayOf(
        """[0-9a-f]{31}.jpg""",
        """QQ图片[0-9]+.jpg""",
        """[A-Z0-9\[\]{}_!@#$%()<>,\.~`]{23}.jpg""",
        """base.*\.apk""",
        """(IMG|VID|video)_[0-9_ ]+.(jpg|mp4)""",
        """(S|s)creenshot[0-9-_ ]+.jpg""",
        """.*(屏幕|深度)?(截图|录屏|录制).*""",
        """[0-9]{13}.(jpg|png|mp4)""",
        """[0-9-_ ]+.(jpg|png|mp4|mkv)""",
        """.*.gif"""
    )

    init {
        channel.subscribeAlways<GroupMessageEvent> {
            // Check GC command
            val plain = message.contentToString()


            // Check is file upload
            for(i in message) // Metadata is inside the chain, need to iterate through the entire thing
                if(i is FileMessage) {
                    // Match file name regex
                    for(j in matches) {
                        if (i.name.matches(j.toRegex())) {
                            MoveToBin(sender, subject, i.name, i.id, j)
                            return@subscribeAlways
                        }
                    }

                    // Match jpg/png size < 500KB
                    if(i.name.matches(""".*\.(jpg|png)""".toRegex()) && i.size < 500 * 1024) {
                        MoveToBin(sender, subject, i.name, i.id, "jpg/png < 500KiB")
                        return@subscribeAlways
                    }
                }
        }
    }

    private suspend fun MoveToBin(sender: Member, group: Group, fileName: String, fileId: String?, rule: String) {
        val sentMsg = group.sendMessage("“${sender.nameCardOrNick}”发送的文件“${fileName}”判定为垃圾文件，" +
                "规则：$rule；将被移动到收集箱内。")
        val file = if(fileId != null) group.filesRoot.resolveById(fileId) else group.filesRoot.resolve(fileName)
        val appealMsg = WaitAppeal(group, sender)
        if(appealMsg != null) {
            sentMsg.recall()
            appealMsg.recall()
        }

        if(file == null) {
            group.sendMessage("${fileName}无法被移动到收集箱内。")
            return
        }
        if(!file.moveTo(group.filesRoot.resolve("/CrustaneTempBin/"))) {
            group.sendMessage("${fileName}无法被移动到收集箱内。")
        }
    }

    private suspend fun WaitAppeal(group: Group, sender: Member): MessageChain? {
        val confirm = nextEventOrNull<GroupMessageEvent>(timeoutMillis = 15_000) {
            (group == it.subject && it.sender.id == sender.id && it.message.contentToString().startsWith("%")) ||
                    (it.message.filterIsInstance<FileMessage>().isNotEmpty())
        }
        if(confirm != null) {
            if(confirm.message.contentToString() == "%gc:appeal") {
                return confirm.message
            }
        }
        return null
    }

    suspend fun CreateBinForGroups() {
        // Prepare a temp file bin
        for(group in bot.groups/*.filter { it.id == 857409227L }*/) // todo:Add an "enabled" database
            if(group.botPermission.isOperator() && !group.filesRoot.resolve("/CrustaneTempBin").exists())
                group.sendMessage(
                    if(group.filesRoot.resolve("/CrustaneTempBin").mkdir())
                        "Crustane创建了垃圾文件收集箱。"
                    else
                        "Crustane无法为本群创建垃圾文件收集箱。"
                )
    }
}