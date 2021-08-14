package org.tech4c57.bot.foundation

import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.events.*
import org.tech4c57.bot.CmdParser
import org.tech4c57.bot.module.ModuleBase
import org.tech4c57.bot.permissions.CmdPermission
import org.tech4c57.bot.structs.CommandInfo
import org.tech4c57.bot.structs.MessageFilterInfo
import org.tech4c57.bot.utils.replyPlainMsg
import java.util.*

object ModuleHolderCore {
    enum class SubscribeTarget(val bit: Int) {
        Invalid(0),
        FriendMsg(1),
        GroupMsg(2),
        TemporaryMsg(4)
    }

    private val cmdIdList: MutableMap<String, CommandInfo> = mutableMapOf()
    private val msgFilterList: MutableList<MessageFilterInfo>
        = mutableListOf()

    suspend fun registerModule(module: ModuleBase, target: EnumSet<SubscribeTarget>): Boolean {
        return if(module.commandName() in cmdIdList || target.isEmpty())
            false
        else {
            module.updatePermission()
            cmdIdList[module.commandName()] = CommandInfo(target, module)
            if(module.hasMessageFilter)
                msgFilterList.add(
                    MessageFilterInfo(
                        filter = module::messageFilter,
                        filterHandler = module::filteredMessageHandler,
                        target,
                        module
                )
                )
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
        var permResult: CmdPermission.PermissionResult

        // Iterate through message filters registered, and find if a message can be processed
        // by a module directly
        // Message filter hooks is prioritized over commands, if filter matched and handler can be called,
        // The message is no longer treated as a command
        msgFilterList.forEach {
            if(it.filter(e)) {
                permResult = internalPermissionCheck(it.module, e)
                if(permResult == CmdPermission.PermissionResult.Execute) {
                    it.filterHandler(e)// Pass message to destination if it's filtered
                    return
                }
            }
        }

        // If no message filter and its corresponding handler was activated, then start command parsing
        val cmd = CmdParser.parse(e.message.serializeToMiraiCode())
        val commandInfo = cmdIdList[cmd.commandName]?.copy() ?: return
        if(commandInfo.target.intersect(listOf(sourceType)).isEmpty())
            return
        // Check permission
        permResult = internalPermissionCheck(commandInfo.module, e)
        when(permResult) {
            CmdPermission.PermissionResult.NoPermission -> {
                replyPlainMsg(e, "您无权使用此命令。")
            }

            CmdPermission.PermissionResult.Execute -> {
                commandInfo.module.execCommand(cmd.params, e)
            }

            else -> {
                // TODO: ?
            }
        }
    }

    suspend fun refreshPermission(cmd: String) {
        cmdIdList[cmd]?.module?.updatePermission()
    }

    suspend fun refreshAllPermissions() {
        cmdIdList.forEach {
            it.value.module.updatePermission()
        }
    }

    private fun internalPermissionCheck(module: ModuleBase, e: MessageEvent): CmdPermission.PermissionResult {
        return module.checkPermission(e,
            level =
            if(e is GroupAwareMessageEvent)
                BotDatabase.getUserPrivilegeLevel(e.subject.id, e.sender.id)
            else
                0
        )
    }

    fun commandExists(cmd: String): Boolean {
        return cmd in cmdIdList.keys
    }

    fun subscribe(bot: Bot) {
        bot.eventChannel.subscribeAlways<MessageEvent> (handler = ModuleHolderCore::msgHandler)
    }
}