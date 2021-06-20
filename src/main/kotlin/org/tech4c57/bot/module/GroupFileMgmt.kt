package org.tech4c57.bot.module

import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.contact.isAdministrator
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.nextEventOrNull
import kotlin.coroutines.suspendCoroutine

class GroupFileMgmt(bot: Bot) : ModuleBase(bot) {
    private val eventChannel = bot.eventChannel.filterIsInstance<MessageEvent>()
    private val cmdRegex by lazy { Regex("""^\s*%fm(:(([a-z]+)(:(.+)?)?)?)?$""") }

    init {
        eventChannel.subscribeAlways<GroupMessageEvent> {
            val match = cmdRegex.matchEntire(message.contentToString())
            if(match != null) {
                val segs = match.groupValues.filter { it != "" } // Why????
                when(segs.size) {
                    5, 6 -> sendCommandUsage(subject, segs[4])
                    7 -> executeCommand(subject, segs[4], segs[6], sender)
                    else -> sendUsage(subject)
                }
            }
        }
    }

    suspend fun sendUsage(subject: Group) {
        subject.sendMessage("命令 %fm: 文件管理\n" +
                "%fm:子命令:参数\n" +
                "mkdir - 新建文件夹\n" +
                "rmdir - 删除空文件夹\n" +
                "rm - 删除文件/递归删除文件夹")
    }

    suspend fun sendCommandUsage(subject: Group, subcmd: String) {
        val msg = when(subcmd) {
            "mkdir" -> "新建一个名称与参数相同的文件夹。"
            "rmdir" -> "删除名称与参数相同的文件夹。"
            "rm" -> "删除名称与参数相同的文件或文件夹及其内容。"
            else -> "该子命令不合法。"
        }
        subject.sendMessage("%fm 子命令$subcmd：$msg")
    }

    suspend fun executeCommand(subject: Group, subcmd: String, operand: String, sender: Member) {
        when(subcmd) {
            "mkdir" -> cmdMkdir(subject, operand)
            "rmdir" -> cmdRmdir(subject, operand)
            "rm" -> cmdRm(subject, operand, sender)
            else -> subject.sendMessage("子命令“$subcmd”不合法。")
        }
    }

    private suspend fun cmdMkdir(subject: Group, operand: String) {
        val dir = subject.filesRoot.resolve("/$operand")
        if(!dir.exists())
            subject.sendMessage(if(dir.mkdir()) "创建了文件夹“$operand”。" else "文件夹“$operand”无法被创建。")
        else
            subject.sendMessage("文件夹“$operand”已存在。")
    }

    private suspend fun cmdRmdir(subject: Group, operand: String) {
        val dir = subject.filesRoot.resolve("/$operand")
        if(dir.listFilesCollection().isNotEmpty())
            subject.sendMessage("文件夹”$operand“非空；请使用rm子命令强行删除。")
        else
            subject.sendMessage(if(dir.delete()) "已删除文件夹”$operand“。" else "文件夹”$operand“无法被删除。")
    }

    private suspend fun cmdRm(subject: Group, operand: String, invoker: Member) {
        val oper = subject.filesRoot.resolve("/$operand")
        if(!oper.exists()) { subject.sendMessage("指定的文件夹或文件”$operand“不存在。"); return }
        // Check if the invoker has permissions to do so
        if((invoker.id != oper.getInfo()?.uploaderId && !oper.isDirectory()) && !invoker.isAdministrator())
            subject.sendMessage("您不是文件的所有者也不是管理员，无法操作。")
        else {
            if(oper.isDirectory()) {
                subject.sendMessage("“$operand”是文件夹，请确认确实要删除它，以及其中的" +
                        "${oper.listFilesCollection().size}个文件。")
                val confirm = nextEventOrNull<GroupMessageEvent>(timeoutMillis = 10_000) {
                    it.sender.id == invoker.id && it.message.contentToString().startsWith("%")
                }
                if(confirm != null) {
                    when(confirm.message.contentToString()) {
                        "%yes", "%confirm", "%ok" -> {
                            subject.sendMessage(if (oper.delete()) "成功删除了“$operand”。" else "“$operand”无法被删除。")
                        }
                        "%no", "%cancel" -> subject.sendMessage("删除操作已取消。")
                        else -> subject.sendMessage("指令不正确；操作已取消。")
                    }
                } else {
                    subject.sendMessage("操作超时，已取消。")
                }
            } else {
                subject.sendMessage(if (oper.delete()) "成功删除了“$operand”。" else "“$operand”无法被删除。")
            }
        }
    }
}