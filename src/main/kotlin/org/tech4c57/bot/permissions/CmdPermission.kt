package org.tech4c57.bot.permissions

import net.mamoe.mirai.event.events.*

// A data class for command permission data storage
class CmdPermission {
    enum class PermissionResult {
        Execute,
        NoPermission,
        Ignore
    }

    // Global control: if value is false, this command will never be invoked
    // DB: RW
    var globalControl: Boolean = false
    // Group control whitelist: if is true, bot will only respond to explicitly allowed groups defined below.
    // DB: RW
    var groupControlWhitelist: Boolean = false
    // Group control: contains the master switch for groups. false for block, true for allow.
    // DB: R
    var groupControl: MutableMap<Long, Boolean> = mutableMapOf()

    // Privileged command: if is true, then this command is only available to privileged users defined below.
    // Blacklist is ignored in this case. Note that privileged users can only be in groups.
    // DB: RW
    var privilegedCmd: Boolean = false
    // Group privileged user: the privileged users of each group; see privilegedCmd.
    // DB: R
    var groupPrivilegedUser: MutableMap<Long, MutableSet<Long>> = mutableMapOf()

    // Group privilege levels affect invokes in groups and temporary group DMs
    // Specific levels override global levels
    // DB: R(W)
    var groupSpecificPrivilegeLevel: MutableMap<Long, Int> = mutableMapOf()
    // DB: RW
    var groupGlobalPrivilegeLevel: Int = 0

    // Friend messages are managed throught access control list, either blacklist or whitelist
    // DB: RW
    var friendMsgWhitelist: Boolean = false
    // DB: R
    var friendMsgAccessControl: MutableMap<Long, Boolean> = mutableMapOf()



    companion object {
        // Super Admins have full control over bot permissions
        var globalSuperAdminUsers: MutableSet<Long> = mutableSetOf()

        // Global blacklist: bot will never respond to blocked user.
        var globalBlacklistedUsers: MutableSet<Long> = mutableSetOf()

        fun defaultCheckPermission(evt: MessageEvent, perm: CmdPermission, level: Int): PermissionResult {
            if(!perm.globalControl)
                return PermissionResult.Ignore
            if(evt.sender.id in globalBlacklistedUsers)
                return PermissionResult.Ignore
            when(evt) {
                is GroupAwareMessageEvent -> {
                    if(perm.groupControlWhitelist) { // Check group control policy
                        if (perm.groupControl[evt.group.id] != true)
                            return PermissionResult.Ignore
                    }
                    else if (perm.groupControl[evt.group.id] == false)
                        return PermissionResult.Ignore

                    return if(perm.privilegedCmd) { // Checks for privileged commands
                        if((perm.groupPrivilegedUser[evt.group.id])?.contains(evt.sender.id) == true)
                            PermissionResult.Execute
                        else
                            PermissionResult.NoPermission
                    } else {
                        if(level >= perm.groupSpecificPrivilegeLevel[evt.group.id] ?: perm.groupGlobalPrivilegeLevel)
                            PermissionResult.Execute
                        else
                            PermissionResult.NoPermission
                    }
                }

                is FriendMessageEvent -> {
                    if(perm.friendMsgWhitelist) {
                        if (perm.friendMsgAccessControl[evt.sender.id] != true)
                            return PermissionResult.Ignore
                    }
                    else if(perm.friendMsgAccessControl[evt.sender.id] == false)
                        return PermissionResult.Ignore
                    return PermissionResult.Execute
                }

                else -> return PermissionResult.Ignore
            }
        }

        fun checkPermissionSuperadmin(evt: MessageEvent): PermissionResult {
            return if(evt.sender.id !in globalSuperAdminUsers) {
                PermissionResult.NoPermission
            } else {
                PermissionResult.Execute
            }
        }
    }
}