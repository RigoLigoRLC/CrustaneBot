package org.tech4c57.bot

import net.mamoe.mirai.BotFactory
import net.mamoe.mirai.utils.BotConfiguration
import java.io.File
import java.nio.file.FileSystemException

class Foundation constructor(config: Map<String, String>) {
    internal val botconfig = config
    val bot = BotFactory.newBot(botconfig["qqid"]!!.toLong(),
            botconfig["qqpassword"]!!,
            BotConfiguration {
                protocol = when (botconfig["protocol"]) {
                    "pad" -> BotConfiguration.MiraiProtocol.ANDROID_PAD
                    "watch" -> BotConfiguration.MiraiProtocol.ANDROID_WATCH
                    "phone" -> BotConfiguration.MiraiProtocol.ANDROID_PHONE
                    else -> BotConfiguration.MiraiProtocol.ANDROID_PHONE
                }
                heartbeatStrategy = when (botconfig["heartbeat"]) {
                    "normal", "norm" -> BotConfiguration.HeartbeatStrategy.STAT_HB
                    "register" -> BotConfiguration.HeartbeatStrategy.REGISTER
                    "none" -> BotConfiguration.HeartbeatStrategy.NONE
                    else -> BotConfiguration.HeartbeatStrategy.STAT_HB
                }
                fileBasedDeviceInfo(botconfig["deviceinfo"] ?: "device.json")
                if (botconfig["contactcache"].toBoolean()) enableContactCache()
            })

    suspend fun login() {
        bot.login()
        bot.getFriend(botconfig["ownerqqid"]?.toLong() ?: 0)
            ?.sendMessage(botconfig["greetings"] ?: "Crustane is now online.")
    }

    companion object {
        fun readConfig(path: String): MutableMap<String, String> {
            val r = mutableMapOf<String, String>()
            val file = File(path)
            if(!file.canRead()) throw FileSystemException("File \"${path}\" unreadable.")
            for (i in file.readLines())
                if ('=' in i)
                    r[i.substringBefore('=')] = i.substringAfter('=')
            return r
        }
    }
}