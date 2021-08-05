package org.tech4c57.bot.module

import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.events.BotGroupPermissionChangeEvent
import net.mamoe.mirai.event.events.MessageEvent
import org.tech4c57.bot.Foundation
import org.tech4c57.bot.permissions.CmdPermission

abstract class ModuleBase(bot: Foundation) {
    abstract fun commandName(): String
    abstract suspend fun execCommand(param: List<String>, evt: MessageEvent)

    private var perm: CmdPermission = CmdPermission()
    open fun checkPermission(perm: CmdPermission, evt: MessageEvent, level: Int) =
        CmdPermission.defaultCheckPermission(perm, evt, level)

}