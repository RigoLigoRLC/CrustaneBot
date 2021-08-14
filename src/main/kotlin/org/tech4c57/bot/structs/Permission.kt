package org.tech4c57.bot.structs

// Command permission

data class CommandPermission(
    var name: String,

    var enabled: Boolean = false,
    var whitelist_group: Boolean = false,
    var privileged: Boolean = false,
    var whitelist_friend: Boolean = false,
    var level: Int = 0,

    var group_control: MutableList<AccessControlOfContact> = mutableListOf(),
    var level_in_groups: MutableList<LevelOfContact> = mutableListOf(),
    var friend_control: MutableList<AccessControlOfContact> = mutableListOf(),
    var privileged_users: MutableList<PrivilegedUsers> = mutableListOf()
)

data class PrivilegedUsers(
    var id: Long, //< ID of group
    var u: MutableList<Long> //< ID of privileged users in the defined group
)

data class AccessControlOfContact(
    var id: Long,
    var a: Boolean, //< Allow or block
)

data class LevelOfContact(
    var id: Long,
    var l: Int,
)

// Global permission

data class GlobalAdminList(
    var kind: String,
    var id: MutableList<Long>
)

data class GlobalBlacklist(
    var kind: String,
    var id: MutableList<Long>
)
//
//data class GlobalPrivilegeLevels(
//
//)
