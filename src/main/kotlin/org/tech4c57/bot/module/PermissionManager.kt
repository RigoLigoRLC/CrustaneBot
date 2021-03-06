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
                "drop" -> {
                    if (ensureParamCount(evt, param.size >= 2) && ensureCommandOperable(evt, param[1], param))
                        successFailReply(evt, deletePermission(param[1]))
                    return
                }

                "init" -> {
                    if (ensureParamCount(evt, param.size >= 2) && ensureCommandOperable(evt, param[1], param))
                        successFailReply(evt, deletePermission(param[1]))
                    return
                }

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
                                replyPlainMsg(evt, "?????????${param[3]}?????????????????????set?????????unset??????")
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
                                replyPlainMsg(evt, "?????????${param[2]}?????????????????????group?????????friend??????")
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
                    replyPlainMsg(evt, "??????????????????????????????????????????")
                }

                "help" -> {
                    showHelp(evt.subject)
                    return
                }

                "__forcethrow__" -> {
                    throw MongoInternalException(param.getOrNull(1) ?: "????????????????????????")
                }

                else -> {
                    replyPlainMsg(evt, "?????????????????????")
                    return
                }
            }
        } catch (e: Throwable) {
            replyPlainMsg(evt, "??????????????????????????????????????????")
            Foundation.tellOwner(
                buildMessageChain {
                    +PlainText("?????????${evt.subject.id}??????${(evt.subject as Group).name}??????" +
                            "???${evt.sender.id}??????${evt.senderName}????????????\n" +
                            evt.message.serializeToMiraiCode() +
                            "????????????????????????\n" +
                            e.localizedMessage + "\n" +
                            e.stackTraceToString())
                }
            )
        }
    }

    private suspend fun deletePermission(cmd: String): Boolean {
        return BotDatabase.db.deletePermission(cmd)
    }

    private suspend fun initializePermission(cmd: String): Boolean {
        return BotDatabase.db.deletePermission(cmd)
    }

    private suspend fun toggleCommand(cmd: String, enable: Boolean): Boolean {
        return BotDatabase.db.setGlobalControl(cmd, enable)
    }

    private suspend fun toggleGroupWhitelist(cmd: String, set: Boolean): Boolean {
        return BotDatabase.db.setGroupControlWhitelist(cmd, set)
    }

    private suspend fun toggleFriendWhitelist(cmd: String, set: Boolean): Boolean {
        return BotDatabase.db.setFriendMsgWhitelist(cmd, set)
    }

    private suspend fun setCommandLevel(cmd: String, level: Int?): Boolean {
        return if(level != null) BotDatabase.db.setGlobalPrivilegeLevel(cmd, level) else false
    }

    private suspend fun setCommandLevelInGroup(cmd: String, level: Int?, group: Long): Boolean {
        return if(level != null) BotDatabase.db.setGroupSpecificPrivilegeLevel(cmd, group, level) else false
    }

    private suspend fun unsetCommandLevelInGroup(cmd: String, group: Long): Boolean {
        return BotDatabase.db.unsetGroupSpecificPrivilegeLevel(cmd, group)
    }

    suspend fun successFailReply(evt: MessageEvent, success: Boolean) {
        replyPlainMsg(evt, if(success) "?????????????????????" else "?????????????????????")
    }

    suspend fun ensureCommandOperable(evt: MessageEvent, cmd: String, param: List<String>): Boolean {
        if("force" != param.last()) {
            if(!ModuleHolderCore.commandExists(cmd)) {
                replyPlainMsg(evt, "??????????????????${cmd}??????????????????????????????????????????????????????????????????force??????")
                return false
            }
        }
        return true
    }

    suspend fun showHelp(to: Contact) {
        to.sendMessage(
            "?????? ${commandName()} ?????????\n" +
                    "???????????????????????????????????????\n" +
                    "?????????%perms [??????] [????????????] [??????]\n" +
                    "???????????????\n" +
                    "enable - ????????????????????????\n" +
                    "disable - ????????????????????????\n" +
                    "whitelist - ???????????????????????????\n" +
                    "????????????[group/friend] [set/unset]???\n" +
                    ""
        )
    }
}