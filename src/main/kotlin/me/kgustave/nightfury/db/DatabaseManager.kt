/*
 * Copyright 2017 Kaidan Gustave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.kgustave.nightfury.db

import me.kgustave.nightfury.db.sql.*
import me.kgustave.nightfury.entities.Case
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.utils.SimpleLog
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException
import java.util.Comparator

/**
 * @author Kaidan Gustave
 */
class DatabaseManager(url: String, user: String, pass: String) {

    companion object {
        private val LOG : SimpleLog = SimpleLog.getLog("SQL")
    }

    init {
        try {
            Class.forName("org.h2.Driver").newInstance()
        } catch (e: Exception) { LOG.fatal(e) }
    }

    private val connection : Connection = DriverManager.getConnection(url, user, pass)

    private val roleMe : SQLRoleMe = SQLRoleMe(connection)
    private val colorMe : SQLColorMe = SQLColorMe(connection)
    private val modRole : SQLModeratorRole = SQLModeratorRole(connection)
    private val mutedRole : SQLMutedRole = SQLMutedRole(connection)

    private val modLog : SQLModeratorLog = SQLModeratorLog(connection)
    private val ignoredChannels : SQLIgnoredChannels = SQLIgnoredChannels(connection)

    private val cases : SQLCases = SQLCases(connection)

    private val prefixes : SQLPrefixes = SQLPrefixes(connection)

    val localTags : SQLLocalTags = SQLLocalTags(connection)
    val globalTags : SQLGlobalTags = SQLGlobalTags(connection)

    val customCommands : SQLCustomCommands = SQLCustomCommands(connection)

    fun startup() : Boolean
    {
        try {
            val statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE)
            statement.execute(
                    "CREATE TABLE cases (" +
                            "number int, guild_id long, message_id long, mod_id long, target_id long," +
                            " is_on_user boolean, action varchar(20), reason varchar(200)" +
                            "); " +
                            "CREATE TABLE channels (guild_id long, channel_id long, type varchar(20)); " +
                            "CREATE TABLE prefixes (guild_id long, prefix varchar(50)); " +
                            "CREATE TABLE roles (guild_id long, role_id long, type varchar(20)); " +
                            "CREATE TABLE global_tags (name varchar(50), owner_id long, content varchar(1900)); " +
                            "CREATE TABLE local_tags (name varchar(50), guild_id long, owner_id long, content varchar(1900)); "
            )
            statement.close()
            return true
        } catch (e : SQLException) {
            LOG.warn(e)
            return false
        }
    }

    fun createCasesTable() : Boolean
    {
        try {
            val statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE)
            statement.execute(
                    "CREATE TABLE cases (" +
                            "number int, guild_id long, message_id long, mod_id long, target_id long," +
                            " is_on_user boolean, action varchar(20), reason varchar(200)" +
                            ");"
            )
            statement.close()
            println("Created Cases Table!")
            return true
        } catch (e : SQLException) {
            LOG.warn(e)
            return false
        }
    }

    fun createChannelsTable() : Boolean
    {
        try {
            val statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE)
            statement.execute("CREATE TABLE channels (guild_id long, channel_id long, type varchar(20));")
            statement.close()
            println("Created Channels Table!")
            return true
        } catch (e : SQLException) {
            LOG.warn(e)
            return false
        }
    }

    fun createPrefixesTable() : Boolean
    {
        try {
            val statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE)
            statement.execute("CREATE TABLE prefixes (guild_id long, prefix varchar(50));")
            statement.close()
            println("Created Prefixes Table!")
            return true
        } catch (e : SQLException) {
            LOG.warn(e)
            return false
        }
    }

    fun createRolesTable() : Boolean
    {
        try {
            val statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE)
            statement.execute("CREATE TABLE roles (guild_id long, role_id long, type varchar(20));")
            statement.close()
            println("Created Prefixes Table!")
            return true
        } catch (e : SQLException) {
            LOG.warn(e)
            return false
        }
    }

    fun createTagsTables() : Boolean
    {
        try {
            val statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE)
            statement.execute("CREATE TABLE global_tags (name varchar(50), owner_id long, content varchar(1900)); " +
                    "CREATE TABLE local_tags (name varchar(50), guild_id long, owner_id long, content varchar(1900));")
            statement.close()
            println("Created Tags Tables!")
            return true
        } catch (e : SQLException) {
            LOG.warn(e)
            return false
        }
    }

    fun createCommandsTable() : Boolean
    {
        try {
            val statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE)
            statement.execute("CREATE TABLE custom_commands (name varchar(50), content varchar(1900), guild_id long)")
            statement.close()
            println("Created Custom Commands Tables!")
            return true
        } catch (e : SQLException) {
            LOG.warn(e)
            return false
        }
    }

    fun isRoleMe(role: Role) : Boolean {
        val rolemes = getRoleMes(role.guild)
        return rolemes.isNotEmpty() && rolemes.contains(role)
    }
    fun getRoleMes(guild: Guild) = roleMe.get(guild, guild.idLong)
    fun addRoleMe(role: Role) {
        roleMe.add(role.guild.idLong, role.idLong)
    }
    fun removeRoleMe(role: Role) = roleMe.remove(role.guild.idLong, role.idLong)

    fun isColorMe(role: Role) : Boolean {
        val colormes = getColorMes(role.guild)
        return colormes.isNotEmpty() && colormes.contains(role)
    }
    fun getColorMes(guild: Guild) = colorMe.get(guild, guild.idLong)
    fun addColorMe(role: Role) = colorMe.add(role.guild.idLong, role.idLong)
    fun removeColorMe(role: Role) = colorMe.remove(role.guild.idLong, role.idLong)

    fun getModRole(guild: Guild) = modRole.get(guild, guild.idLong)
    fun setModRole(role: Role) {
        if(getModRole(role.guild)!=null)
            modRole.update(role.idLong, role.guild.idLong)
        else
            modRole.set(role.guild.idLong, role.idLong)
    }
    fun resetModRole(guild: Guild) = modRole.reset(guild.idLong)

    fun getMutedRole(guild: Guild) = mutedRole.get(guild, guild.idLong)
    fun setMutedRole(role: Role) {
        if(getMutedRole(role.guild)!=null)
            mutedRole.update(role.idLong, role.guild.idLong)
        else
            mutedRole.set(role.guild.idLong, role.idLong)
    }
    fun resetMutedRole(guild: Guild) = mutedRole.reset(guild.idLong)

    fun getModLog(guild: Guild) = modLog.get(guild, guild.idLong)
    fun setModLog(channel: TextChannel) {
        if(getModLog(channel.guild)!=null)
            modLog.update(channel.idLong, channel.guild.idLong)
        else
            modLog.set(channel.guild.idLong, channel.idLong)
    }
    fun resetModLog(guild: Guild) = modLog.reset(guild.idLong)

    fun isIgnoredChannel(channel: TextChannel) : Boolean {
        val ignored = getIgnoredChannels(channel.guild)
        return ignored.isNotEmpty() && ignored.contains(channel)
    }
    fun getIgnoredChannels(guild: Guild) = ignoredChannels.get(guild, guild.idLong)
    fun addIgnoredChannel(channel: TextChannel) = ignoredChannels.add(channel.guild.idLong, channel.idLong)
    fun removeIgnoredChannel(channel: TextChannel) = ignoredChannels.remove(channel.guild.idLong, channel.idLong)

    fun getCaseMatching(guild: Guild, toMatch: (Case) -> Boolean) : Case {
        val cases = getCases(guild)
        if(cases.isEmpty()) return Case()
        return cases.stream().filter(toMatch).findFirst().takeIf { it.isPresent }?.get()?:Case()
    }
    fun getFirstCaseMatching(guild: Guild, toMatch: (Case) -> Boolean) : Case {
        val cases = getCases(guild)
        if(cases.isEmpty()) return Case()
        return cases.stream().filter(toMatch).sorted(Comparator.comparing(Case::number)).findFirst().takeIf { it.isPresent }?.get()?: Case()
    }
    fun getCases(guild: Guild) = cases.get(guild, guild.idLong)
    fun addCase(case: Case) = cases.add(*case.toDBArgs())
    fun updateCase(case: Case) = cases.updateCase(case)

    fun isPrefixFor(guild: Guild, prefix: String) : Boolean {
        val prefixes = getPrefixes(guild)
        return prefixes.isNotEmpty() && prefixes.stream().anyMatch { p -> p.equals(prefix, ignoreCase = true) }
    }
    fun getPrefixes(guild: Guild) = prefixes.get(guild, guild.idLong)
    fun addPrefix(guild: Guild, prefix: String) = prefixes.add(guild.idLong, prefix)
    fun removePrefix(guild: Guild, prefix: String) = prefixes.remove(guild.idLong, prefix)

    fun evaluate(string: String) {
        try {
            val statement = connection.prepareStatement(string)
            statement.execute()
            statement.close()
        } catch (e: SQLException) { throw e }
    }

    fun leaveGuild(guild: Guild)
    {
        roleMe.removeAll(guild.idLong)
        colorMe.removeAll(guild.idLong)
        modRole.reset(guild.idLong)
        mutedRole.reset(guild.idLong)
        modLog.reset(guild.idLong)
        ignoredChannels.removeAll(guild.idLong)
        cases.removeAll(guild.idLong)
        prefixes.removeAll(guild.idLong)
        localTags.deleteAllTags(guild)
        customCommands.removeAll(guild)
    }

    fun shutdown() = try { connection.close() } catch (e: SQLException) { LOG.warn(e) }
}