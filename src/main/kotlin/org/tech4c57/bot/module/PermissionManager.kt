package org.tech4c57.bot.module

import com.mongodb.MongoInternalException
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.buildMessageChain
import org.tech4c57.bot.Foundation
import org.tech4c57.bot.foundation.ModuleHolderCore
import org.tech4c57.bot.foundation.BotDatabase
import org.tech4c57.bot.permissions.CmdPermission
import org.tech4c57.bot.utils.ensureParamCount
import org.tech4c57.bot.utils.replyPlainMsg

class PermissionManager(foundation: Foundation) : ModuleBase(foundation) {
    override fun commandName(): String {
        return "perms"
    }

    override fun checkPermission(evt: MessageEvent, perm: CmdPermission, level: Int): CmdPermission.PermissionResult
        = CmdPermission.checkPermissionSuperadmin(evt)

    override suspend fun execCommand(param: List<String>, evt: MessageEvent) {
        if(param.isEmpty()) {
            return
        }

        try {
            when (param[0]) {
                "enable" -> {
                    if (ensureParamCount(evt, param.size >= 2) && ensureCommandOperable(evt, param[1], param))
                        successFailReply(evt, toggleCommand(param[1], true))
                    return
                }

                "disable" -> {
                    if (ensureParamCount(evt, param.size >= 2) && ensureCommandOperable(evt, param[1], param))
                        successFailReply(evt, toggleCommand(param[1], false))
                    return
                }

                "whitelist" -> {
                    if (ensureParamCount(evt, param.size >= 4) && ensureCommandOperable(evt, param[1], param)) {
                        val set = when (param[3]) {
                            "set" -> true
                            "unset" -> false
                            else -> {
                                replyPlainMsg(evt, "参数“${param[3]}”非法，应为“set”或“unset”。")
                                return
                            }
                        }
                        when (param[2]) {
                            "group" -> {
                                successFailReply(evt, toggleGroupWhitelist(param[1], set))
                            }
                            "friend" -> {
                                successFailReply(evt, toggleFriendWhitelist(param[1], set))
                            }
                            else -> {
                                replyPlainMsg(evt, "参数“${param[2]}”非法，应为“group”或“friend”。")
                                return
                            }
                        }
                        return
                    }
                }

                "level" -> {
                    if(ensureParamCount(evt, param.size >= 3)) {
                        when(param[2]) {
                            "set" -> {
                                if(ensureParamCount(evt, param.size >= 4) &&
                                        ensureCommandOperable(evt, param[1], param))
                                            successFailReply(evt, setCommandLevel(param[1], param[3].toIntOrNull()))
                            }

                            "sethere" -> {
                                if(ensureParamCount(evt, param.size >= 4) &&
                                        ensureCommandOperable(evt, param[1], param))
                                            successFailReply(evt, setCommandLevelInGroup(param[1],
                                                param[3].toIntOrNull(), evt.subject.id))
                            }

                            "unsethere" -> {
                                if(ensureParamCount(evt, param.size >= 4) &&
                                    ensureCommandOperable(evt, param[1], param))
                                    successFailReply(evt, unsetCommandLevelInGroup(param[1], evt.subject.id))
                            }
                        }
                    }
                }

                "refresh" -> {
                    BotDatabase.db.fetchGlobalsFromDatabase()
                    ModuleHolderCore.refreshAllPermissions()
                    replyPlainMsg(evt, "成功从数据库刷新了权限信息。")
                }

                "help" -> {
                    showHelp(evt.subject)
                    return
                }

                "__forcethrow__" -> {
                    throw MongoInternalException(param.getOrNull(1) ?: "人工生成的异常。")
                }

                else -> {
                    replyPlainMsg(evt, "权限操作不明。")
                    return
                }
            }
        } catch (e: Throwable) {
            replyPlainMsg(evt, "操作由于抛出异常而不能完成。")
            Foundation.tellOwner(
                buildMessageChain {
                    +PlainText("在群“${evt.subject.id}”（${(evt.subject as Group).name}）中" +
                            "“${evt.sender.id}”（${evt.senderName}）的操作\n" +
                            evt.message.serializeToMiraiCode() +
                            "引发了下述异常：\n" +
                            e.localizedMessage + "\n" +
                            e.stackTraceToString())
                }
            )
        }
    }

    private fun toggleCommand(cmd: String, enable: Boolean): Boolean {
        return BotDatabase.db.setGlobalControl(cmd, enable)
    }

    private fun toggleGroupWhitelist(cmd: String, set: Boolean): Boolean {
        return BotDatabase.db.setGroupControlWhitelist(cmd, set)
    }

    private fun toggleFriendWhitelist(cmd: String, set: Boolean): Boolean {
        return BotDatabase.db.setFriendMsgWhitelist(cmd, set)
    }

    private fun setCommandLevel(cmd: String, level: Int?): Boolean {
        return if(level != null) BotDatabase.db.setGlobalPrivilegeLevel(cmd, level) else false
    }

    private fun setCommandLevelInGroup(cmd: String, level: Int?, group: Long): Boolean {
        return if(level != null) BotDatabase.db.setGroupSpecificPrivilegeLevel(cmd, group, level) else false
    }

    private fun unsetCommandLevelInGroup(cmd: String, group: Long): Boolean {
        return BotDatabase.db.unsetGroupSpecificPrivilegeLevel(cmd, group)
    }

    suspend fun successFailReply(evt: MessageEvent, success: Boolean) {
        replyPlainMsg(evt, if(success) "权限操作成功。" else "权限操作失败。")
    }

    suspend fun ensureCommandOperable(evt: MessageEvent, cmd: String, param: List<String>): Boolean {
        if("force" != param.last()) {
            if(!ModuleHolderCore.commandExists(cmd)) {
                replyPlainMsg(evt, "操作的命令“${cmd}”不存在。要强制操作，请在尾部位置指定参数“force”。")
                return false
            }
        }
        return true
    }

    suspend fun showHelp(to: Contact) {
        to.sendMessage(
            "命令 ${commandName()} 帮助：\n" +
                    "只有超级管理员才可以使用。\n" +
                    "语法：%perms [操作] [目标命令] [参数]\n" +
                    "操作列表：\n" +
                    "enable - 打开命令全局开关\n" +
                    "disable - 关闭命令全局开关\n" +
                    "whitelist - 设置命令白名单模式\n" +
                    "（参数：[group/friend] [set/unset]）\n" +
                    ""
        )
    }
}