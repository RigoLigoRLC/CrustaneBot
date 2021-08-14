package org.tech4c57.bot.module

import net.mamoe.mirai.event.events.MessageEvent
import org.tech4c57.bot.Foundation
import org.tech4c57.bot.foundation.BotDatabase
import org.tech4c57.bot.permissions.CmdPermission

abstract class ModuleBase(bot: Foundation) {
    abstract fun commandName(): String
    abstract suspend fun execCommand(param: List<String>, evt: MessageEvent)
    open suspend fun messageFilter(evt: MessageEvent): Boolean = false
    open suspend fun filteredMessageHandler(evt: MessageEvent): Unit = Unit
    open val hasMessageFilter = false

    protected var perm: CmdPermission = CmdPermission()
    
    open fun checkPermission(evt: MessageEvent, perm: CmdPermission = this.perm, level: Int) =
        CmdPermission.defaultCheckPermission(evt, perm, level)
    open suspend fun updatePermission() {
        perm =  BotDatabase.db.fetchCommandPermFromDatabase(commandName())
    }
}