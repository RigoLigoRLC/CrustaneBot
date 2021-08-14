package org.tech4c57.bot.foundation

import com.mongodb.*
import com.mongodb.client.model.Filters
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates
import com.mongodb.connection.ServerSettings
import kotlinx.coroutines.runBlocking
import org.bson.Document
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.eq
import org.litote.kmongo.reactivestreams.KMongo
import org.tech4c57.bot.permissions.CmdPermission
import org.tech4c57.bot.structs.CommandPermission
import org.tech4c57.bot.structs.GlobalAdminList
import org.tech4c57.bot.structs.GlobalBlacklist

class BotDatabase constructor(
    address: String,
    port: Int = 27017,
    userName: String,
    password: String) {

    private var client: CoroutineClient = CoroutineClient(
        KMongo.createClient(
            MongoClientSettings.builder()
                .applyConnectionString(ConnectionString("mongodb://${address}:${port}"))
                .credential(MongoCredential.createCredential(userName, "admin", password.toCharArray()))
                .writeConcern(WriteConcern(1, 10_000))
                .retryReads(true)
                .retryWrites(true)
                .build()
        )
    )
    private var dbPerm: CoroutineDatabase = client.getDatabase("crustane_perm")
    private var dbData: CoroutineDatabase = client.getDatabase("crustane_data")
    private var cmdPermCollection: CoroutineCollection<CommandPermission> = dbPerm.getCollection<CommandPermission>("cmd_perm")
    private lateinit var permCache: MutableMap<String, Any>
    private var databaseUsable = false

    private val cachedPrivilegeLevels: MutableMap<Long, MutableMap<Long, Int>> = mutableMapOf()
    private val cachedBlacklistedUsers: MutableSet<Long> = mutableSetOf()

    init {
        runBlocking {
            fetchGlobalsFromDatabase()
        }
    }

    fun getUserPrivilegeLevel(group: Long, user: Long): Int {
        return (cachedPrivilegeLevels[group] ?: mutableMapOf(Pair(0L, 0)))[user] ?: -1
    }

    suspend fun fetchGlobalsFromDatabase() {
        // Get blacklisted users
        CmdPermission.globalSuperAdminUsers.clear()
        val globalBlacklistCursor = dbPerm.getCollection<GlobalAdminList>("global_perm")
            .find(GlobalAdminList::kind eq "admin").toList().iterator()
        if(globalBlacklistCursor.hasNext())
            globalBlacklistCursor.next().id.forEach {
                CmdPermission.globalSuperAdminUsers.add(it)
            }

        // Get superadmin users
        CmdPermission.globalBlacklistedUsers.clear()
        val globalSuperadminCursor = dbPerm.getCollection<GlobalBlacklist>("global_perm")
            .find(GlobalBlacklist::kind eq "blacklist").toList().iterator()
        if(globalSuperadminCursor.hasNext())
            globalSuperadminCursor.next().id.forEach {
                CmdPermission.globalBlacklistedUsers.add(it)
            }

        // Get user privileges
//        val globalPrivLevelCursor = dbPerm.getCollection("privilege_lvls").find(Filters.exists("group")).cursor()
//        if(globalPrivLevelCursor.hasNext())
//            globalPrivLevelCursor.forEach { itg ->
//                val groupList: MutableMap<Long, Int> = mutableMapOf()
//                (itg["users"] as List<*>).filterIsInstance<Document>().forEach { itu ->
//                    groupList[itu["id"] as? Long ?: 0L] = itu["lv"] as? Int ?: 0
//                }
//                cachedPrivilegeLevels[itg["id"] as? Long ?: 0L] = groupList
//            }
    }

    suspend fun fetchCommandPermFromDatabase(name: String): CmdPermission {
        val ret = CmdPermission()
        val commandPermCursor = cmdPermCollection
            .find(CommandPermission::name eq name).toList().iterator()
        if(commandPermCursor.hasNext()) {
            // Only fetch one if multiple exists
            val cmdPerm = commandPermCursor.next()
            ret.globalControl = cmdPerm.enabled
            ret.groupControlWhitelist = cmdPerm.whitelist_group
            ret.privilegedCmd = cmdPerm.privileged
            ret.friendMsgWhitelist = cmdPerm.whitelist_friend
            ret.groupGlobalPrivilegeLevel = cmdPerm.level

            cmdPerm.group_control.forEach {
                    ret.groupControl[it.id] = it.a
            }

            cmdPerm.friend_control.forEach {
                    ret.friendMsgAccessControl[it.id] = it.a
            }

            cmdPerm.level_in_groups.forEach {
                ret.groupSpecificPrivilegeLevel[it.id] = it.l
            }

            cmdPerm.privileged_users.forEach { it ->
                ret.groupPrivilegedUser[it.id] = mutableSetOf<Long>().also { itu ->
                    for (i in it.u)
                        itu.add(i)
                }
            }

            if(commandPermCursor.hasNext()) {
                // TODO: How to warn the bot owner?
            }
        }
        return ret
    }

    suspend fun deletePermission(cmd: String): Boolean {
        val permCollection = cmdPermCollection
        return permCollection.deleteMany(CommandPermission::name eq cmd).wasAcknowledged() &&
                permCollection.insertOne(CommandPermission(name = cmd)).wasAcknowledged()
    }

    suspend fun initializePermission(cmd: String): Boolean {
TODO()
    }

    suspend fun setGlobalControl(cmd: String, enable: Boolean): Boolean {
        val ret = cmdPermCollection
            .updateOne(
                CommandPermission::name eq cmd,
                Updates.set("enabled", enable),
                UpdateOptions().upsert(true)
            ).wasAcknowledged()
        ModuleHolderCore.refreshPermission(cmd)
        return ret
    }

    suspend fun setGroupControlWhitelist(cmd: String, enable: Boolean): Boolean {
        val ret = cmdPermCollection
            .updateOne(
                CommandPermission::name eq cmd,
                Updates.set("whitelist_group", enable),
                UpdateOptions().upsert(true)
            ).wasAcknowledged()
        ModuleHolderCore.refreshPermission(cmd)
        return ret
    }

    suspend fun setPrivilegeCmd(cmd: String, enable: Boolean): Boolean {
        val ret = cmdPermCollection
            .updateOne(
                CommandPermission::name eq cmd,
                Updates.set("privileged", enable),
                UpdateOptions().upsert(true)
            ).wasAcknowledged()
        ModuleHolderCore.refreshPermission(cmd)
        return ret
    }

    suspend fun setFriendMsgWhitelist(cmd: String, enable: Boolean): Boolean {
        val ret = cmdPermCollection
            .updateOne(
                CommandPermission::name eq cmd,
                Updates.set("whitelist_friend", enable),
                UpdateOptions().upsert(true)
            ).wasAcknowledged()
        ModuleHolderCore.refreshPermission(cmd)
        return ret
    }

    suspend fun setGlobalPrivilegeLevel(cmd: String, level: Int): Boolean {
        val ret = cmdPermCollection
            .updateOne(
                CommandPermission::name eq cmd,
                Updates.set("level", level),
                UpdateOptions().upsert(true)
            ).wasAcknowledged()
        ModuleHolderCore.refreshPermission(cmd)
        return ret
    }

    suspend fun setGroupSpecificPrivilegeLevel(cmd: String, group: Long, level: Int): Boolean {
        val ret = cmdPermCollection
            .updateMany(
                CommandPermission::name eq cmd,
                Updates.set("level_in_groups.$[targetGroup].l", level),
                UpdateOptions().upsert(true)
                    .arrayFilters(listOf(Filters.eq("targetGroup.id", group)))
            ).wasAcknowledged()
        ModuleHolderCore.refreshPermission(cmd)
        return ret
    }

    suspend fun unsetGroupSpecificPrivilegeLevel(cmd: String, group: Long): Boolean {
        val ret = cmdPermCollection
            .updateMany(
                CommandPermission::name eq cmd,
                Updates.unset("level_in_groups.$[targetGroup].l"),
                UpdateOptions().upsert(true)
                    .arrayFilters(listOf(Filters.eq("targetGroup.id", group)))
            ).wasAcknowledged()
        ModuleHolderCore.refreshPermission(cmd)
        return ret
    }

    fun getCommandCollection(cmd: String): CoroutineCollection<Document> {
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