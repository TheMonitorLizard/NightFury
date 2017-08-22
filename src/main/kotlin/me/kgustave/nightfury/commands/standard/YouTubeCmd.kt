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
package me.kgustave.nightfury.commands.standard

import me.kgustave.nightfury.Command
import me.kgustave.nightfury.CommandEvent
import me.kgustave.nightfury.CooldownScope
import me.kgustave.nightfury.annotations.APICache
import me.kgustave.nightfury.annotations.MustHaveArguments
import me.kgustave.nightfury.api.YouTubeAPI

/**
 * @author Kaidan Gustave
 */
@APICache
@MustHaveArguments("Specify what to search youtube for.")
class YouTubeCmd(val ytAPI: YouTubeAPI) : Command()
{
    init {
        this.name = "YouTube"
        this.aliases = arrayOf("yt")
        this.arguments = "[Query]"
        this.help = "Searches YouTube."
        this.cooldown = 30
        this.cooldownScope = CooldownScope.USER
        this.guildOnly = false
    }

    override fun execute(event: CommandEvent)
    {
        val query = event.args
        event.channel.sendTyping().queue {
            val results = ytAPI.search(query)
            when
            {
                results == null -> event.replyError("An unexpected error occurred while searching!")
                results.isEmpty() -> event.replyError("No results were found for \"$query\"!")
                else -> event.replySuccess("**${event.author.asMention} https://youtube.com/watch?v=${results[0]}**")
            }
            event.invokeCooldown()
        }
    }

    @APICache
    @Suppress("unused")
    fun clearCache() = ytAPI.clearCache()
}