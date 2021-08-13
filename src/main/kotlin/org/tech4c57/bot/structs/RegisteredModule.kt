package org.tech4c57.bot.structs

import net.mamoe.mirai.event.events.MessageEvent
import org.tech4c57.bot.foundation.ModuleHolderCore
import org.tech4c57.bot.module.ModuleBase
import java.util.*

data class MessageFilterInfo(val filter: suspend (MessageEvent) -> Boolean,
                             val filterHandler: suspend (MessageEvent) -> Unit,
                             val target: EnumSet<ModuleHolderCore.SubscribeTarget>,
                             val module: ModuleBase)

data class CommandInfo(val target: EnumSet<ModuleHolderCore.SubscribeTarget>,
                       val module: ModuleBase)