package org.tech4c57.bot

import net.mamoe.mirai.network.LoginFailedException
import org.tech4c57.bot.module.GroupCommands
import org.tech4c57.bot.module.GroupFileMgmt
import org.tech4c57.bot.module.PingPong

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
    }
    catch(e: Exception)
    {
        println("Open config file ${args[0]} failed, reason: ${e.message}.")
        return;
    }
    val botFoundation = Foundation(config)
    try { botFoundation.login() } catch (e: LoginFailedException) {
        println("Login failed! Message: ${e.message}")
        return
    }

    // Register modules
    val modPing = PingPong(botFoundation.bot)
    val modFM = GroupFileMgmt(botFoundation.bot)
    val modGrpGeneral = GroupCommands(botFoundation.bot)
}
