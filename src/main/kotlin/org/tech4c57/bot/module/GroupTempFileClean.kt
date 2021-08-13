package org.tech4c57.bot.module

import kotlinx.coroutines.flow.toList
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.nextEventOrNull
import net.mamoe.mirai.message.data.FileMessage
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.MessageSource.Key.recall
import org.tech4c57.bot.CmdParser
import org.tech4c57.bot.Foundation
import org.tech4c57.bot.permissions.CmdPermission

class GroupTempFileClean(private val env: Foundation) : ModuleBase(env) {
    override val hasMessageFilter = true

    override fun commandName(): String {
        return "gc"
    }

    override suspend fun execCommand(param: List<String>, evt: MessageEvent) {
        if(evt is GroupMessageEvent);
        else
            return

        when(param.size) {
            0 -> sendUsage(evt.group)
            else -> execCommand(evt.group, evt.sender, param)
        }
    }

    override suspend fun messageFilter(evt: MessageEvent): Boolean {
        if(evt !is GroupMessageEvent || !evt.group.botPermission.isAdministrator())
            return false
        // Check is file upload
        return evt.message.filterIsInstance<FileMessage>().isNotEmpty()
    }

    override suspend fun filteredMessageHandler(evt: MessageEvent) {
        if(evt !is GroupMessageEvent) return // Ensure it is group message again
        evt.message.filterIsInstance<FileMessage>().forEach {
            // Match file name regex
            for(j in matches) {
                if (it.name.matches(j.toRegex())) {
                    MoveToBin(evt.sender, evt.subject, it.name, it.id, j)
                }
            }

            // Match jpg/png size < 500KB
            if(it.name.matches(""".*\.(jpg|png)""".toRegex()) && it.size < 500 * 1024) {
                MoveToBin(evt.sender, evt.subject, it.name, it.id, "jpg/png < 500KiB")
            }
        }
    }

    private val matches: Array<String> = arrayOf(
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
            val cmd = CmdParser.parse(confirm.message.contentToString())
            if(cmd.commandName == "gc" && cmd.params.size == 1 && cmd.params[0] == "appeal") {
                return confirm.message
            }
        }
        return null
    }

    suspend fun CreateBinForGroups() {
        // Prepare a temp file bin
        for(group in env.bot.groups/*.filter { it.id == 857409227L }*/) // todo:Add an "enabled" database
            createBinForGroup(group, false)
    }

    suspend fun createBinForGroup(group: Group, alertOnFail: Boolean = true) {
        if(group.filesRoot.resolve("/CrustaneTempBin").exists() && alertOnFail) {
            group.sendMessage("本群已经有垃圾文件收集箱。")
            return
        }
        if(!group.botPermission.isOperator() && alertOnFail) {
            group.sendMessage("Crustane不是管理员，无权创建垃圾文件收集箱。")
            return
        }
        if(group.filesRoot.resolve("/CrustaneTempBin").mkdir()) {
            group.sendMessage("Crustane创建了垃圾文件收集箱。")
            return
        }
        if(alertOnFail) {
            group.sendMessage("Crustane由于某种原因无法为本群创建垃圾文件收集箱。")
        }
    }

    private suspend fun dumpBin(group: Group, invoker: Member) {
        val bin = group.filesRoot.resolve("/CrustaneTempBin")
        var size = 0L
        var count = 0L
        if(!invoker.isOperator()) {
            group.sendMessage("您不是管理员，无权操作清理收集箱。")
            return
        }
        if(!group.botPermission.isOperator()) {
            group.sendMessage("Crustane没有管理员权限，不能清理收集箱。")
            return
        }
        if(!bin.exists()) {
            group.sendMessage("Crustane还没有收集箱。请使用%gc init创建收集箱。")
            return
        }
        try {
            bin.listFiles().toList().forEach {
                size += it.getInfo()?.length ?: 0
                it.delete()
                if(++count % 50L == 0L) {
                    group.sendMessage("清理了${count}个文件，总大小${size/1_000_000.0}MiB...")
                }
            }
        }
        catch (e: Exception) {
            group.sendMessage("Crustane清理文件时出错：${e.message}")
        }
        group.sendMessage("共清理了${count}个文件，总大小${size/1_000_000.0}MiB。")
    }

    private suspend fun sendUsage(group: Group) {
        group.sendMessage("命令 gc: 群文件垃圾收集\n" +
                "%gc 子命令 [参数]\n" +
                "init - 为群聊创建收集箱（需机器人有管理员权限）\n" +
                "appeal - 为被判定为垃圾的文件申诉（需15秒内使用）\n" +
                "collect - 在群文件中查找垃圾文件（管理员才可用）\n" +
                "dump - 清理收集箱内所有文件（管理员才可用）")
    }

    suspend fun sendCmdUsage(group: Group, subCmd: String) {
        val msg = when(subCmd) {
            "dump" -> "无更多信息。"
            "collect" -> "暂未实现。"
            "init" -> "如果机器人为管理员，则自动创建“CrustaneTempBin”作为收集箱。"
            "appeal" -> "如果您是管理员或文件发送者，则可在一个文件被识别为垃圾文件15秒内执行此命令，" +
                    "如此可取消Crustane移至收集箱的操作。如果超时或未上传文件，则命令无效果。"
            else -> {
                sendUsage(group)
                return
            }
        }
        group.sendMessage("%gc 子命令$subCmd：$msg")
    }

    private suspend fun execCommand(group: Group, invoker: Member, params: List<String>) {
        when(params[0]) {
            "init" -> createBinForGroup(group)
            "appeal" -> return
            "dump" -> dumpBin(group, invoker)
        }
    }
}