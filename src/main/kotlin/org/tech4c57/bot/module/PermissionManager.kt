package org.tech4c57.bot.module

import net.mamoe.mirai.event.events.MessageEvent
import org.tech4c57.bot.Foundation
import org.tech4c57.bot.permissions.CmdPermission

class PermissionManager(foundation: Foundation) : ModuleBase(foundation) {
    override fun commandName(): String {
        return "perm"
    }

    override fun checkPermission(perm: CmdPermission, evt: MessageEvent, level: Int): CmdPermission.PermissionResult {
        return if(evt.sender.id !in CmdPermission.globalSuperAdminUsers) {
            CmdPermission.PermissionResult.NoPermission
        } else {
            CmdPermission.PermissionResult.Execute
        }
    }

    override suspend fun execCommand(param: List<String>, evt: MessageEvent) {
        if(param.isEmpty()) {
            return
        }

        when(param[0]) {
            "enable" -> {
                if(!toggleCommand(param.getOrNull(1), true)) {
                    evt.subject.sendMessage("权限操作失败。")
                    return
                }
            }

            "disable" -> {
                if(!toggleCommand(param.getOrNull(1), false)) {
                    evt.subject.sendMessage("权限操作失败。")
                    return
                }
            }
        }
    }

    fun toggleCommand(cmd: String?, enable: Boolean): Boolean {
        if(cmd.isNullOrBlank()) {
            return false
        }

    }
}