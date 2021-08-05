package org.tech4c57.bot.foundation

import com.mongodb.MongoClient
import com.mongodb.MongoClientOptions
import com.mongodb.MongoCredential
import com.mongodb.ServerAddress
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import org.bson.Document
import org.tech4c57.bot.permissions.CmdPermission

class BotDatabase constructor(private val address: String,
                              private val port: Int = 27017,
                              private val userName: String,
                              private val password: String) {
    private lateinit var client: MongoClient
    private lateinit var dbPerm: MongoDatabase
    private lateinit var dbData: MongoDatabase
    private lateinit var permCache: MutableMap<String, Any>
    private var databaseUsable = false

    private val cachedPrivilegeLevels: MutableMap<Long, MutableMap<Long, Int>> = mutableMapOf()
    private val cachedBlacklistedUsers: MutableSet<Long> = mutableSetOf()

    init {
        client = MongoClient(
            ServerAddress(address, port),
            MongoCredential.createCredential(userName, "admin", password.toCharArray()),
            MongoClientOptions.builder()
                .minConnectionsPerHost(3)
                .maxWaitTime(20_000)
                .connectTimeout(10_000)
                .build()
        )

        dbPerm = client.getDatabase("crustane_perm")
        dbData = client.getDatabase("crustane_data")
    }

    init {
        fetchGlobalsFromDatabase()
    }

    fun getUserPrivilegeLevel(group: Long, user: Long): Int {
        return (cachedPrivilegeLevels[group] ?: mutableMapOf(Pair(0L, 0)))[user] ?: -1
    }

    fun fetchGlobalsFromDatabase() {
        // Get blacklisted users
        val globalBlacklistCursor = dbPerm.getCollection("global_perm").find(Filters.eq("kind", "blacklist")).cursor()
        if(globalBlacklistCursor.hasNext())
            (globalBlacklistCursor.next()["id"] as List<*>).filterIsInstance<Long>().forEach {
                cachedBlacklistedUsers.add(it)
            }

        // Get user privileges
        val globalPrivLevelCursor = dbPerm.getCollection("privilege_lvls").find(Filters.exists("group")).cursor()
        if(globalPrivLevelCursor.hasNext())
            globalPrivLevelCursor.forEach { itg ->
                val groupList: MutableMap<Long, Int> = mutableMapOf()
                (itg["users"] as List<*>).filterIsInstance<Document>().forEach { itu ->
                    groupList[itu["id"] as? Long ?: 0L] = itu["lv"] as? Int ?: 0
                }
                cachedPrivilegeLevels[itg["id"] as? Long ?: 0L] = groupList
            }
    }

    fun fetchCommandPermFromDatabase(name: String): CmdPermission {
        val ret = CmdPermission()
        val commandPermCursor = dbPerm.getCollection("cmd_perm").find(Filters.eq("name", name)).cursor()
        if(commandPermCursor.hasNext()) {
            // Only fetch one
            val cmdPerm = commandPermCursor.next()
            ret.privilegedCmd = cmdPerm["privileged"] as? Boolean ?: false
            ret.globalControl = cmdPerm["enabled"] as? Boolean ?: false
            ret.groupControlWhitelist = cmdPerm["whitelist_group"] as? Boolean ?: false
            ret.friendMsgWhitelist = cmdPerm["whitelist_friend"] as? Boolean ?: false
            ret.groupGlobalPrivilegeLevel = cmdPerm["level"] as? Int ?: 99

            (cmdPerm["group_control"] as? List<*> ?: listOf(Pair(0L, false)))
                .filterIsInstance<Map.Entry<Long, Boolean>>()
                .forEach {
                ret.groupControl[it.key] = it.value
            }

            (cmdPerm["friend_control"] as? List<*> ?: listOf(Pair(0L, false)))
                .filterIsInstance<Map.Entry<Long, Boolean>>()
                .forEach {
                    ret.friendMsgAccessControl[it.key] = it.value
                }

            (cmdPerm["level_in_groups"] as? List<*> ?: listOf(Pair(0L, 99)))
                .filterIsInstance<Map.Entry<Long, Int>>()
                .forEach {
                    ret.groupSpecificPrivilegeLevel[it.key] = it.value
                }

            (cmdPerm["privileged_users"] as? List<*> ?: listOf(Pair(0L, listOf(0L))))
                .filterIsInstance<Map.Entry<Long, List<Long>>>()
                .forEach { itp ->
                    ret.groupPrivilegedUser[itp.key]?.addAll(itp.value)
                }
        }
        return ret
    }

    companion object {
        var mutexAcquired: Boolean = false
        fun AcquireLock(): Boolean = if(mutexAcquired) false else { mutexAcquired = true; true }
        fun ReleaseLock(): Unit {
            mutexAcquired = false
        }
    }
}