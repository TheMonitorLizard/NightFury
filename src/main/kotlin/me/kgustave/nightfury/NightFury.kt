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
package me.kgustave.nightfury

import com.jagrosh.jagtag.JagTag
import com.jagrosh.jagtag.Parser
import com.jagrosh.jdautilities.waiter.EventWaiter
import me.kgustave.nightfury.api.E621API
import me.kgustave.nightfury.api.GoogleAPI
import me.kgustave.nightfury.api.GoogleImageAPI
import me.kgustave.nightfury.api.YouTubeAPI
import me.kgustave.nightfury.commands.admin.*
import me.kgustave.nightfury.commands.moderator.*
import me.kgustave.nightfury.commands.dev.*
import me.kgustave.nightfury.commands.music.PlayCmd
import me.kgustave.nightfury.commands.music.QueueCmd
import me.kgustave.nightfury.commands.music.SkipCmd
import me.kgustave.nightfury.commands.music.StopCmd
import me.kgustave.nightfury.commands.other.*
import me.kgustave.nightfury.commands.standard.*
import me.kgustave.nightfury.db.DatabaseManager
import me.kgustave.nightfury.extensions.*
import me.kgustave.nightfury.jagtag.getMethods
import me.kgustave.nightfury.listeners.InvisibleTracker
import me.kgustave.nightfury.music.MusicManager
import net.dv8tion.jda.core.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.file.Paths
import java.util.logging.Level

fun main(args: Array<String>?)
{
    NightFury.LOG.info("Starting NightFury...")

    java.util.logging.Logger.getLogger("org.apache.http.client.protocol.ResponseProcessCookies").level = Level.OFF

    try {
        NightFury()
    } catch(e : IOException) {
        NightFury.LOG.error("Failed to get configurations!",e)
    }
}

/**
 * @author Kaidan Gustave
 */
class NightFury(file: File = Paths.get(System.getProperty("user.dir"), "config.txt").toFile())
{
    companion object
    {
        val VERSION: String = this::class.java.`package`.implementationVersion?:"BETA"
        val GITHUB: String = "https://github.com/TheMonitorLizard/NightFury/"
        val LOG: Logger = LoggerFactory.getLogger("NightFury")

        fun shutdown(exit: Int)
        {
            LOG.info("Shutdown Complete! "+if(exit == 0)"Restarting..." else "Exiting...")
            System.exit(exit)
        }
    }

    init {
        val config = Config(file)
        val manager = DatabaseManager(config.dbURL, config.dbUser, config.dbPass)

        val e621 = E621API()
        val google = GoogleAPI()
        val image = GoogleImageAPI()
        val yt = YouTubeAPI(config.ytApiKey)

        val parser : Parser = JagTag.newDefaultBuilder().addMethods(getMethods()).build()

        val waiter = EventWaiter()
        val invisTracker = InvisibleTracker()
        val musicManager = MusicManager()
        val client = Client(
                config.prefix, config.devId, manager,
                config.success, config.warning, config.error,
                config.server, config.dbotskey, config.dborgkey,
                waiter, parser,

                AboutCmd(*config.permissions),
                AvatarCmd(),
                ColorMeCmd(),
                EmoteCmd(),
                GoogleCmd(google),
                HelpCmd(),
                ImageCmd(image),
                InfoCmd(invisTracker),
                InviteCmd(*config.permissions),
                PingCmd(),
                QuoteCmd(),
                RoleMeCmd(waiter),
                ServerCmd(waiter, invisTracker),
                TagCommand(waiter),
                YouTubeCmd(yt),

                PlayCmd(musicManager),
                QueueCmd(waiter, musicManager),
                SkipCmd(musicManager),
                StopCmd(musicManager),

                E621Cmd(e621, waiter),

                BanCmd(),
                CleanCmd(),
                KickCmd(),
                MuteCmd(),
                ReasonCmd(),
                SettingsCmd(),
                UnbanCmd(),
                UnmuteCmd(),

                CustomCommandCmd(waiter),
                ModeratorCmd(),
                ModLogCmd(),
                PrefixCmd(),
                WelcomeCmd(),

                LevelCmd(),
                ToggleCmd(),

                BashCmd(),
                EvalCmd(musicManager),
                GuildlistCmd(waiter),
                MemoryCmd(),
                ModeCmd(),
                RestartCmd(),
                ShutdownCmd(),
                WhitelistCmd(musicManager)
        )

        JDABuilder(AccountType.BOT) buildAsync {
            manager  { AsyncEventManager() }
            listener { client }
            listener { invisTracker }
            listener { musicManager }
            token    { config.token }
            status   { OnlineStatus.DO_NOT_DISTURB }
            game     { "Starting Up..." }
        }
    }

    private infix inline fun <reified T: JDABuilder> T.buildAsync(lazy: JDABuilder.() -> Unit)
    {
        lazy()
        buildAsync()
    }
}

internal class Config(key: File)
{
    private val tokens = try { key.readLines() } catch (e: IOException) { throw e }

    internal val token       : String            = tokens[0]
    internal val devId       : Long              = tokens[1].toLong()
    internal val dbotskey    : String            = tokens[2]
    internal val dborgkey    : String            = tokens[3]
    internal val dbURL       : String            = tokens[4]
    internal val dbUser      : String            = tokens[5]
    internal val dbPass      : String            = tokens[6]
    internal val ytApiKey    : String            = tokens[7]
    internal val prefix      : String            = if(key.nameWithoutExtension == "testConfig") "||" else "|"
    internal val success     : String            = "\uD83D\uDC32"
    internal val warning     : String            = "\uD83D\uDC22"
    internal val error       : String            = "\uD83D\uDD25"
    internal val server      : String            = "https://discord.gg/xkkw54u"
    internal val permissions : Array<Permission> = arrayOf(

            Permission.MESSAGE_HISTORY,
            Permission.MESSAGE_EMBED_LINKS,
            Permission.MESSAGE_ATTACH_FILES,
            Permission.MESSAGE_ADD_REACTION,

            Permission.MANAGE_PERMISSIONS,
            Permission.MANAGE_ROLES,
            Permission.MANAGE_CHANNEL,
            Permission.NICKNAME_MANAGE,
            Permission.MESSAGE_MANAGE,

            Permission.KICK_MEMBERS,
            Permission.BAN_MEMBERS,

            Permission.VIEW_AUDIT_LOGS

    )
}
