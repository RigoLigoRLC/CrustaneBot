package org.tech4c57.bot

import net.mamoe.mirai.network.LoginFailedException
import org.tech4c57.bot.foundation.BotDatabase
import org.tech4c57.bot.foundation.ModuleHolderCore
import org.tech4c57.bot.module.*
//import org.tech4c57.bot.module.GroupTempFileClean
import java.util.*

suspend fun main(args: Array<String>) {
    if(args.isEmpty()) {
        println("Parameter: botconf_file")
        return
    }
    /*
        config file contents: k=v
        qqid=QQ ID of Bot
        qqpassword=Password of Bot
        [opt]protocol=pad/watch/<phone> Connection protocol
        [opt]heartbeat=<normal/norm>/register/none Heartbeat packet
        [opt]deviceinfo=<device.json> Device Info file(device.json)
        [opt]contactcache=true/<false> Cache contacts
        [opt]ownerqqid=QQ ID of bot owner, only one is accepted, must be friend of bot
        [opt]greetings=Greetings message to the owner
     */
    val config: Map<String, String>
    try {
        config = Foundation.readConfig(args[0])
    } catch (e: Exception) {
        println("Open config file ${args[0]} failed, reason: ${e.message}.")
        return;
    }

    // Connect to and initialize database, read things needs to be cached
    try {
        BotDatabase.db = BotDatabase(
            config["dbhost"] ?: "localhost",
            config["dbport"]?.toInt() ?: 27017,
            config["dbuser"]!!,
            config["dbpwd"]!!)
    } catch (e: Exception) {
        println("Failed to initialize database! ${e.message}")
        return
    }

    val botFoundation = Foundation(config)
    try { botFoundation.login() } catch (e: LoginFailedException) {
        println("Login failed! Message: ${e.message}")
        return
    }

    // Register modules
    ModuleHolderCore.registerModule(GroupCommands(botFoundation), EnumSet.of(ModuleHolderCore.SubscribeTarget.GroupMsg))
    ModuleHolderCore.registerModule(PingPong(botFoundation),
                                EnumSet.of(
                                    ModuleHolderCore.SubscribeTarget.GroupMsg,
                                           ModuleHolderCore.SubscribeTarget.FriendMsg,
                                           ModuleHolderCore.SubscribeTarget.TemporaryMsg))
    ModuleHolderCore.registerModule(EmergencyStop(botFoundation, botFoundation.botconfig["ownerqqid"]?.toLong() ?: 0),
        EnumSet.of(
            ModuleHolderCore.SubscribeTarget.GroupMsg,
                   ModuleHolderCore.SubscribeTarget.FriendMsg))
    ModuleHolderCore.registerModule(GroupTempFileClean(botFoundation), EnumSet.of(ModuleHolderCore.SubscribeTarget.GroupMsg))
    ModuleHolderCore.registerModule(PermissionManager(botFoundation),
        EnumSet.of(
            ModuleHolderCore.SubscribeTarget.GroupMsg,
            ModuleHolderCore.SubscribeTarget.FriendMsg))
    ModuleHolderCore.registerModule(GetPicture(botFoundation),
        EnumSet.of(
            ModuleHolderCore.SubscribeTarget.FriendMsg,
            ModuleHolderCore.SubscribeTarget.GroupMsg))

    ModuleHolderCore.subscribe(botFoundation.bot)

    botFoundation.bot.join() // So the bot coroutine doesn't exit until bot is terminated
}
