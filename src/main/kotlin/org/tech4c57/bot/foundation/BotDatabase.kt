package org.tech4c57.bot.foundation

import com.mongodb.MongoClient
import com.mongodb.MongoClientOptions
import com.mongodb.MongoCredential
import com.mongodb.ServerAddress
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates
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
                .socketTimeout(10_000)
                .retryReads(true)
                .retryWrites(true)
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
        CmdPermission.globalBlacklistedUsers.clear()
        val globalBlacklistCursor = dbPerm.getCollection("global_perm").find(Filters.eq("kind", "blacklist")).cursor()
        if(globalBlacklistCursor.hasNext())
            (globalBlacklistCursor.next()["id"] as List<*>).filterIsInstance<Long>().forEach {
                CmdPermission.globalBlacklistedUsers.add(it)
            }

        // Get superadmin users
        CmdPermission.globalSuperAdminUsers.clear()
        val globalSuperadminCursor = dbPerm.getCollection("global_perm").find(Filters.eq("kind", "admin")).cursor()
        if(globalSuperadminCursor.hasNext())
            (globalSuperadminCursor.next()["id"] as List<*>).filterIsInstance<Long>().forEach {
                CmdPermission.globalSuperAdminUsers.add(it)
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
            // Only fetch one if multiple exists
            val cmdPerm = commandPermCursor.next()
            ret.globalControl = cmdPerm["enabled"] as? Boolean ?: false
            ret.groupControlWhitelist = cmdPerm["whitelist_group"] as? Boolean ?: false
            ret.privilegedCmd = cmdPerm["privileged"] as? Boolean ?: false
            ret.friendMsgWhitelist = cmdPerm["whitelist_friend"] as? Boolean ?: false
            ret.groupGlobalPrivilegeLevel = cmdPerm["level"] as? Int ?: 99

            (cmdPerm["group_control"] as? List<*> ?: listOf<Document>())
                .filterIsInstance<Document>()
                .forEach {
                    val id = it["id"] as? Long; val level = it["l"] as? Int
                    if(id != null && level != null) ret.groupSpecificPrivilegeLevel[id] = level
            }

            (cmdPerm["friend_control"] as? List<*> ?: listOf<Document>())
                .filterIsInstance<Document>()
                .forEach {
                    val id = it["id"] as? Long; val allow = it["a"] as? Boolean
                    if(id != null && allow != null) ret.friendMsgAccessControl[id] = allow
                }

            (cmdPerm["level_in_groups"] as? List<*> ?: listOf<Document>())
                .filterIsInstance<Document>()
                .forEach {
                    val id = it["id"] as? Long; val level = it["l"] as? Int
                    if(id != null && level != null) ret.groupSpecificPrivilegeLevel[id] = level
                }

            (cmdPerm["privileged_users"] as? List<*> ?: listOf<Document>())
                .filterIsInstance<Document>()
                .forEach { it ->
                    val id = it["id"] as? Long; val list = (it["u"] as? List<*>)?.filterIsInstance<Long>()
                    if(id != null && list != null) {
                        ret.groupPrivilegedUser[id] = mutableSetOf<Long>().also {
                            for(i in list)
                                it.add(i)
                        }
                    }
                }

            if(commandPermCursor.hasNext()) {
                // TODO: How to warn the bot owner?
            }
        }
        return ret
    }

    fun setGlobalControl(cmd: String, enable: Boolean): Boolean {
        val ret = dbPerm.getCollection("cmd_perm")
            .updateOne(
                Filters.eq("name", cmd),
                Updates.set("enabled", enable),
                UpdateOptions().upsert(true)
            ).wasAcknowledged()
        ModuleHolderCore.refreshPermission(cmd)
        return ret
    }

    fun setGroupControlWhitelist(cmd: String, enable: Boolean): Boolean {
        val ret = dbPerm.getCollection("cmd_perm")
            .updateOne(
                Filters.eq("name", cmd),
                Updates.set("whitelist_group", enable),
                UpdateOptions().upsert(true)
            ).wasAcknowledged()
        ModuleHolderCore.refreshPermission(cmd)
        return ret
    }

    fun setPrivilegeCmd(cmd: String, enable: Boolean): Boolean {
        val ret = dbPerm.getCollection("cmd_perm")
            .updateOne(
                Filters.eq("name", cmd),
                Updates.set("privileged", enable),
                UpdateOptions().upsert(true)
            ).wasAcknowledged()
        ModuleHolderCore.refreshPermission(cmd)
        return ret
    }

    fun setFriendMsgWhitelist(cmd: String, enable: Boolean): Boolean {
        val ret = dbPerm.getCollection("cmd_perm")
            .updateOne(
                Filters.eq("name", cmd),
                Updates.set("whitelist_friend", enable),
                UpdateOptions().upsert(true)
            ).wasAcknowledged()
        ModuleHolderCore.refreshPermission(cmd)
        return ret
    }

    fun setGlobalPrivilegeLevel(cmd: String, level: Int): Boolean {
        val ret = dbPerm.getCollection("cmd_perm")
            .updateOne(
                Filters.eq("name", cmd),
                Updates.set("level", level),
                UpdateOptions().upsert(true)
            ).wasAcknowledged()
        ModuleHolderCore.refreshPermission(cmd)
        return ret
    }

    fun setGroupSpecificPrivilegeLevel(cmd: String, group: Long, level: Int): Boolean {
        val ret = dbPerm.getCollection("cmd_perm")
            .updateMany(
                Filters.eq("name", cmd),
                Updates.set("level_in_groups.$[targetGroup].l", level),
                UpdateOptions().upsert(true)
                    .arrayFilters(listOf(Filters.eq("targetGroup.id", group)))
            ).wasAcknowledged()
        ModuleHolderCore.refreshPermission(cmd)
        return ret
    }

    fun unsetGroupSpecificPrivilegeLevel(cmd: String, group: Long): Boolean {
        val ret = dbPerm.getCollection("cmd_perm")
            .updateMany(
                Filters.eq("name", cmd),
                Updates.unset("level_in_groups.$[targetGroup].l"),
                UpdateOptions().upsert(true)
                    .arrayFilters(listOf(Filters.eq("targetGroup.id", group)))
            ).wasAcknowledged()
        ModuleHolderCore.refreshPermission(cmd)
        return ret
    }

    fun getCommandCollection(cmd: String): MongoCollection<Document> {
        return dbData.getCollection(cmd)
    }

    companion object {
        private var mutexAcquired: Boolean = false
        fun AcquireLock(): Boolean = if(mutexAcquired) false else { mutexAcquired = true; true }
        fun ReleaseLock(): Unit {
            mutexAcquired = false
        }

        fun getUserPrivilegeLevel(group: Long, user: Long): Int {
            return BotDatabase.db.cachedPrivilegeLevels[group]?.get(user) ?: 0
        }

        lateinit var db: BotDatabase
    }
}