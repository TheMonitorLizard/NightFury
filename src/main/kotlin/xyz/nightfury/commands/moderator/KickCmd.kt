/*
 * Copyright 2017-2018 Kaidan Gustave
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
package xyz.nightfury.commands.moderator

import xyz.nightfury.Category
import xyz.nightfury.Command
import xyz.nightfury.CommandEvent
import xyz.nightfury.annotations.MustHaveArguments
import xyz.nightfury.entities.then
import xyz.nightfury.extensions.kick
import xyz.nightfury.extensions.formattedName
import net.dv8tion.jda.core.Permission
import xyz.nightfury.annotations.HasDocumentation
import xyz.nightfury.db.entities.ModLogger

/**
 * @author Kaidan Gustave
 */
@HasDocumentation
@MustHaveArguments("Mention a user or provide a user ID to kick.")
class KickCmd : Command() {

    init {
        this.name = "Kick"
        this.arguments = "[@User or ID] <Reason>"
        this.help = "Kicks a user from the server."
        this.botPermissions = arrayOf(Permission.KICK_MEMBERS)
        this.category = Category.MODERATOR
        this.guildOnly = true
    }

    override fun execute(event: CommandEvent)
    {
        val parsed = event.modSearch()?:return

        val id = parsed.first
        val reason = parsed.second

        val target = event.guild.getMemberById(id)?:return event.replyError("Could not find a member matching \"${event.args}\"!")

        // Error Responses
        val error = when {
            event.selfMember == target
                    -> "I cannot kick myself from the server!"

            event.author == target
                    -> "You cannot kick yourself from the server!"

            event.guild.owner.user == target
                    -> "You cannot kick ${target.user.formattedName(true)} because they are the owner of the server!"

            !event.selfMember.canInteract(target)
                    -> "I cannot kick ${target.user.formattedName(true)}!"

            !event.member.canInteract(target)
                    -> "You cannot kick ${target.user.formattedName(true)}!"

            else    -> null
        }

        if(error != null)
            return event.replyError(error)

        target.kick(reason) then {
            if(reason != null)
                ModLogger.newKick(event.member, target.user, reason)
            else
                ModLogger.newKick(event.member, target.user)
            event.replySuccess("${target.user.formattedName(true)} was kicked from the server.")
        } catch {
            event.replyError("Kicking ${target.user.formattedName(true)} failed for an unexpected reason!")
        }
    }
}
