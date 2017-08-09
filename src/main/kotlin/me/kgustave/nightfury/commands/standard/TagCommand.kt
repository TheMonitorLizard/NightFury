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

import club.minnced.kjda.promise
import com.jagrosh.jdautilities.menu.pagination.PaginatorBuilder
import com.jagrosh.jdautilities.waiter.EventWaiter
import me.kgustave.kjdautils.utils.findMembers
import me.kgustave.kjdautils.utils.findUsers
import me.kgustave.kjdautils.menu.*
import me.kgustave.nightfury.*
import me.kgustave.nightfury.annotations.AutoInvokeCooldown
import me.kgustave.nightfury.annotations.MustHaveArguments
import me.kgustave.nightfury.db.sql.SQLGlobalTags
import me.kgustave.nightfury.db.sql.SQLLocalTags
import me.kgustave.nightfury.jagtag.TagErrorException
import me.kgustave.nightfury.utils.*
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.User

/**
 * @author Kaidan Gustave
 */
class TagCommand(waiter: EventWaiter) : Command()
{
    init {
        this.name = "Tag"
        this.aliases = arrayOf("t")
        this.arguments = "[Tag Name] <Tag Args>"
        this.help = "Calls a tag."
        this.helpBiConsumer = Command.standardSubHelp(
                "Tags are a way to store text in a easy-to-call way.\n" +
                        "In addition to this, tags use JagTag syntax in order to " +
                        "process syntax structures such as `{@user}` into a user " +
                        "mention. Listings and descriptions for all structures is " +
                        "available at ${NightFury.github}wiki",
                true
        )
        this.guildOnly = false
        this.children = arrayOf(
                TagCreateGlobalCmd(),
                TagCreateCmd(),
                TagDeleteCmd(),
                TagEditCmd(),
                TagListCmd(waiter),
                TagOwnerCmd(),
                TagRawCmd(),

                TagOverrideCmd()
        )
    }

    override fun execute(event: CommandEvent)
    {
        if(event.args.isEmpty())
            return event.replyError(TOO_FEW_ARGS_ERROR.format("Try specifying a tag name in the format `${event.client.prefix}tag [tag name]`."))
        val parts = event.args.split(Regex("\\s+"),2)
        val name = if(event.client.getCommandByName(parts[0])!=null)
            return event.reply("*You remember Monitor's words: Not everything is a tag!*")
        else parts[0]
        val args = if(parts.size>1) parts[1] else ""
        if(event.isFromType(ChannelType.TEXT)) {
            val content : String = if(event.localTags.isTag(name, event.guild)) {
                event.localTags.getTagContent(name, event.guild)
            } else if(event.globalTags.isTag(name)) {
                event.globalTags.getTagContent(name)
            } else ""
            if(content.isEmpty())
                return event.replyError("**No Tag Found Matching \"$name\"**\n" +
                        SEE_HELP.format(event.client.prefix,this.name))
            else try {
                event.reply(
                        event.client.parser.clear()
                                .put("args", args.trim())
                                .put("user", event.author)
                                .put("guild", event.guild)
                                .put("channel", event.textChannel)
                                .parse(content)
                )
            } catch (e : TagErrorException) {
                if(e.message!=null) event.replyError(e.message)
                else                event.replyError("Tag matching \"$name\" could not be processed for an unknown reason!")
            }
        } else {
            val content : String = if(event.globalTags.isTag(name)) {
                event.globalTags.getTagContent(name)
            } else ""

            if(content.isEmpty())
                return event.replyError("**No Global Tag Found Matching \"$name\"**\n" +
                        SEE_HELP.format(event.client.prefix,this.name))
            else try {
                event.reply(
                        event.client.parser.clear()
                                .put("args", args)
                                .put("user", event.author)
                                .parse(content)
                )
            } catch (e : TagErrorException) {
                if(e.message!=null) event.replyError(e.message)
                else                event.replyError("Tag matching \"$name\" could not be processed for an unknown reason!")
            }
        }
    }
}

@MustHaveArguments
private class TagCreateCmd : Command()
{
    init {
        this.name = "Create"
        this.fullname = "Tag Create"
        this.arguments = "[Tag Name] [Tag Content]"
        this.help = "Creates a new local Tag."
        this.helpBiConsumer = Command.standardSubHelp(
                "Local tags are only available to the server they are created on.\n" +
                        "If there is already a global tag with the name specified when using this " +
                        "command, a local tag cannot be created, however a moderator or administrator " +
                        "may use the `Override` sub-command to create a local version as a replacement.\n\n" +

                        "Tag names cannot exceed 50 characters in length and cannot contain whitespace.\n" +
                        "Tag content cannot exceed 1900 characters.\n\n" +

                        "*If you discover any NSFW, racist, or in any other way 'harmful' tags, please report " +
                        "them immediately!*",
                true
        )
        this.cooldown = 150
        this.cooldownScope = CooldownScope.USER_GUILD
        this.guildOnly = true
    }

    override fun execute(event: CommandEvent)
    {
        val parts = event.args.split(Regex("\\s+"),2)

        val name = if(parts[0].length>50)
            return event.replyError("**Tag names cannot exceed 50 characters in length!**\n" +
                    SEE_HELP.format(event.client.prefix, fullname))
        else if(event.client.getCommandByName(parts[0])!=null)
            return event.replyError("**Illegal Tag Name!**\n" +
                    "Tags may not have names that match command names!")
        else parts[0]

        val content = if(parts.size==1)
            return event.replyError("**You must specify content when creating a tag!**\n" +
                    SEE_HELP.format(event.client.prefix, fullname))
        else if(parts[1].length>1900)
            return event.replyError("**Tag content cannot exceed 1900 characters in length!**\n" +
                    SEE_HELP.format(event.client.prefix, fullname))
        else parts[1]

        if(event.localTags.isTag(name, event.guild) || event.globalTags.isTag(name))
            return event.replyError("Tag named \"$name\" already exists!")
        else {
            event.localTags.addTag(name, event.author.idLong, content, event.guild)
            event.replySuccess("Successfully created local tag \"**$name**\" on ${event.guild.name}!")
            event.invokeCooldown()
        }
    }
}

@MustHaveArguments
private class TagCreateGlobalCmd : Command()
{
    init {
        this.name = "CreateGlobal"
        this.fullname = "Tag CreateGlobal"
        this.arguments = "[Tag Name] [Tag Content]"
        this.help = "Creates a new global tag."
        this.helpBiConsumer = Command.standardSubHelp(
                "Global tags are available to all servers.\n" +
                        "If there is already a global tag with the name specified when using this " +
                        "command, a global tag cannot be created, however a local override can be made " +
                        "on any server by a moderator or administrator using the `Override` sub-command.\n\n" +

                        "Tag names cannot exceed 50 characters in length and cannot contain whitespace.\n" +
                        "Tag content cannot exceed 1900 characters.\n\n" +

                        "*If you discover any NSFW, racist, or in any other way 'harmful' tags, please report " +
                        "them immediately!*",
                true
        )
        this.cooldown = 240
        this.cooldownScope = CooldownScope.USER
        this.guildOnly = false
    }

    override fun execute(event: CommandEvent)
    {
        val parts = event.args.split(Regex("\\s+"),2)
        val name = if(parts[0].length>50)
            return event.replyError("**Tag names cannot exceed 50 characters in length!**\n" +
                    SEE_HELP.format(event.client.prefix, fullname))
        else if(event.client.getCommandByName(parts[0])!=null)
            return event.replyError("**Illegal Tag Name!**\n" +
                    "Tags may not have names that match command names!")
        else parts[0]

        val content = if(parts.size==1)
            return event.replyError("**You must specify content when creating a tag!**\n" +
                    SEE_HELP.format(event.client.prefix, fullname))
        else if(parts[1].length>1900)
            return event.replyError("**Tag content cannot exceed 1900 characters in length!**\n" +
                    SEE_HELP.format(event.client.prefix, fullname))
        else parts[1]

        if((event.isFromType(ChannelType.TEXT) && event.localTags.isTag(name, event.guild)) || event.globalTags.isTag(name))
            return event.replyError("Tag named \"$name\" already exists!")
        else {
            event.globalTags.addTag(name,event.author.idLong,content)
            event.replySuccess("Successfully created global tag \"**$name**\"!")
            event.invokeCooldown()
        }
    }
}

@MustHaveArguments
private class TagDeleteCmd : Command()
{
    init {
        this.name = "Delete"
        this.fullname = "Tag Delete"
        this.arguments = "[Tag Name]"
        this.help = "Deletes a tag you own."
        this.helpBiConsumer = Command.standardSubHelp(
                "It's worth noting that if a user owns both the local and global version " +
                        "of a tag when using this command on a server, the priority when deleting goes " +
                        "to the *local* version, not the global one.",
                true
        )
        this.guildOnly = true
    }

    override fun execute(event: CommandEvent)
    {
        val name = event.args.split(Regex("\\s+"))[0]
        if(event.isFromType(ChannelType.TEXT)) {
            if(!event.localTags.isTag(name, event.guild)) {
                if(!event.globalTags.isTag(name))
                    event.replyError("Tag named \"$name\" does not exist!")
                else if(event.globalTags.getTagOwnerId(name)==event.author.idLong) {
                    event.globalTags.deleteTag(name, event.author.idLong)
                    event.replySuccess("Successfully deleted local tag \"**$name**\"!")
                } else {
                    event.replyError("**You cannot delete the global tag \"$name\" because you are not it's owner!**\n" +
                            SEE_HELP.format(event.client.prefix, fullname))
                }
            } else if(event.localTags.getTagOwnerId(name,event.guild)==event.author.idLong) {
                event.localTags.deleteTag(name, event.author.idLong, event.guild)
                event.replySuccess("Successfully deleted local tag \"**$name**\"!")
            } else {
                event.replyError("**You cannot delete the local tag \"$name\" because you are not it's owner!**\n" +
                        SEE_HELP.format(event.client.prefix, fullname))
            }
        } else {
            if(!event.globalTags.isTag(name))
                event.replyError("Tag named \"$name\" does not exist!")
            else if(event.globalTags.getTagOwnerId(name)==event.author.idLong) {
                event.globalTags.deleteTag(name, event.author.idLong)
                event.replySuccess("Successfully deleted local tag \"**$name**\"!")
            } else {
                event.replyError("**You cannot delete the global tag \"$name\" because you are not it's owner!**\n" +
                        SEE_HELP.format(event.client.prefix, fullname))
            }
        }
    }
}

@MustHaveArguments
private class TagEditCmd : Command()
{
    init {
        this.name = "Edit"
        this.fullname = "Tag Edit"
        this.arguments = "[Tag Name] [New Tag Content]"
        this.help = "Edits a tag you own."
        this.helpBiConsumer = Command.standardSubHelp(
                "It's worth noting that if a user owns both the local and global version " +
                        "of a tag when using this command on a server, the priority when editing goes " +
                        "to the *local* version, not the global one.",
                true
        )
        this.cooldown = 180
        this.cooldownScope = CooldownScope.USER
        this.guildOnly = false
    }

    override fun execute(event: CommandEvent)
    {
        val parts = event.args.split(Regex("\\s+"),2)
        val name = if(parts[0].length<=50)
            parts[0]
        else
            return event.replyError("**Tag names cannot exceed 50 characters in length!**\n" +
                    SEE_HELP.format(event.client.prefix, fullname))

        val newContent = if(parts.size==1)
            return event.replyError("**You must specify content when editing a tag!**\n" +
                    SEE_HELP.format(event.client.prefix, fullname))
        else if(parts[1].length>1900)
            return event.replyError("**Tag content cannot exceed 1900 characters in length!**\n" +
                    SEE_HELP.format(event.client.prefix, fullname))
        else parts[1]

        if(event.isFromType(ChannelType.TEXT)) {
            if(!event.localTags.isTag(name, event.guild)) {
                if(!event.globalTags.isTag(name)) {
                    event.replyError("Tag named \"$name\" does not exist!")
                } else if(event.globalTags.getTagOwnerId(name)==event.author.idLong) {
                    event.globalTags.editTag(newContent, name, event.author.idLong)
                    event.replySuccess("Successfully edit global tag \"**$name**\"!")
                    event.invokeCooldown()
                } else {
                    event.replyError("**You cannot edit the global tag \"$name\" because you are not it's owner!**\n" +
                            SEE_HELP.format(event.client.prefix, fullname))
                }
            } else if(event.localTags.getTagOwnerId(name, event.guild)==event.author.idLong) {
                event.localTags.editTag(newContent, name, event.author.idLong, event.guild)
                event.replySuccess("Successfully edit local tag \"**$name**\"!")
                event.invokeCooldown()
            } else {
                event.replyError("**You cannot edit the local tag \"$name\" because you are not it's owner!**\n" +
                        SEE_HELP.format(event.client.prefix, fullname))
            }
        } else {
            if(!event.globalTags.isTag(name)) {
                event.replyError("Tag named \"$name\" does not exist!")
            } else if(event.globalTags.getTagOwnerId(name)==event.author.idLong) {
                event.globalTags.editTag(newContent, name, event.author.idLong)
                event.replySuccess("Successfully edit local tag \"**$name**\"!")
                event.invokeCooldown()
            } else {
                event.replyError("**You cannot edit the global tag \"$name\" because you are not it's owner!**\n" +
                        SEE_HELP.format(event.client.prefix, fullname))
            }
        }
    }
}

@AutoInvokeCooldown
private class TagListCmd(val waiter: EventWaiter) : Command()
{
    val builder : PaginatorBuilder = PaginatorBuilder()
            .timeout          { delay { 20 } }
            .showPageNumbers  { true }
            .useNumberedItems { true }
            .waitOnSinglePage { true }
            .waiter           { waiter }

    init {
        this.name = "List"
        this.fullname = "Tag List"
        this.arguments = "<User>"
        this.help = "Gets all the tags owned by a user."
        this.helpBiConsumer = Command.standardSubHelp(
                "Not specifying a user will get a list of tags owned by the person using the command.",
                true
        )
        this.guildOnly = false
        this.cooldown = 10
        this.cooldownScope = CooldownScope.USER
    }

    override fun execute(event: CommandEvent)
    {
        val query = event.args
        val temp : Member? = if(event.isFromType(ChannelType.TEXT)) {
            if(query.isEmpty()) {
                event.member
            } else {
                val found = event.guild.findMembers(query)
                if(found.isEmpty()) null
                else if(found.size>1) return event.replyError(multipleMembersFound(query, found))
                else found[0]
            }
        } else null

        val user : User = if(temp!=null) {
            temp.user
        } else if(query.isEmpty()) {
            event.author
        } else {
            val found = event.jda.findUsers(query)
            if(found.isEmpty()) return event.replyError(noMatch("users", query))
            else if(found.size>1) return event.replyError(multipleUsersFound(query, found))
            else found[0]
        }
        val member : Member? = if(temp == null && event.isFromType(ChannelType.TEXT)) event.guild.getMember(user) else temp

        val localTags = (if(member!=null) event.localTags.getAllTags(member.user.idLong,event.guild) else emptySet()).map { "$it (Local)" }
        val globalTags = event.globalTags.getAllTags(user.idLong).map { "$it (Global)" }

        if(localTags.isEmpty() && globalTags.isEmpty())
            event.replyError("${if(event.author==user) "You do" else "${user.formattedName(false)} does"} not have any tags!")

        with(builder)
        {
            text        { -> "Tags owned by ${user.formattedName(true)}" }
            if(localTags.isNotEmpty())
                items   { addAll(localTags) }
            items       { addAll(globalTags) }
            finalAction { event.linkMessage(it) }
            displayIn   { event.channel }
        }
    }
}

@MustHaveArguments
private class TagOwnerCmd : Command()
{
    init {
        this.name = "Owner"
        this.fullname = "Tag Owner"
        this.aliases = arrayOf("creator")
        this.arguments = "[Tag Name]"
        this.help = "Gets the owner of a tag."
        this.helpBiConsumer = Command.standardSubHelp(
                "There are several cases where this command **will not work**.\n" +
                        "It is a semi-reliable way to get the owner of a command, but there is " +
                        "no guarantee that an owner name (or even ID) will be returned.",
                true
        )
        this.cooldown = 10
        this.cooldownScope = CooldownScope.USER
        this.guildOnly = false
    }

    override fun execute(event: CommandEvent)
    {
        val name = event.args.split(Regex("\\s+"))[0]
        val ownerId : Long
        val isLocal : Boolean = if(event.isFromType(ChannelType.TEXT)) {
            if(event.localTags.isTag(name, event.guild)) {
                ownerId = event.localTags.getTagOwnerId(name, event.guild)
                true
            } else if(event.globalTags.isTag(name)) {
                ownerId = event.globalTags.getTagOwnerId(name)
                false
            } else return event.replyError("Tag named \"$name\" does not exist!")
        } else if(event.globalTags.isTag(name)) {
            ownerId = event.globalTags.getTagOwnerId(name)
            false
        } else return event.replyError("Tag named \"$name\" does not exist!")

        // If this happens... Uh... Let's just put this here in case :/
        if(ownerId==0L) return event.replyError("Tag named \"$name\" does not exist!")
        // Cover overrides
        if(isLocal && ownerId==1L) {
            event.invokeCooldown()
            return event.replyWarning("Local tag named \"$name\" belongs to the server.")
        }

        val str = if(isLocal) "local tag \"${event.localTags.getOriginalName(name, event.guild)}\""
        else "global tag \"${event.globalTags.getOriginalName(name)}\""

        event.jda.retrieveUserById(ownerId).promise() then {
            if(it == null) event.replyError("The owner of $str was improperly retrieved!")
            else           event.replySuccess("The $str is owned by ${it.formattedName(true)}${
            if(!event.jda.users.contains(it)) " (ID: ${it.id})." else "."
            }")
            event.invokeCooldown()
        } catch {
            event.replyError("The owner of $str could not be retrieved for an unexpected reason!")
        }
    }
}

@MustHaveArguments
private class TagRawCmd : Command()
{
    init {
        this.name = "Raw"
        this.fullname = "Tag Raw"
        this.arguments = "[Tag Name]"
        this.help = "Gets the raw, non-parsed form of a tag."
        this.helpBiConsumer = Command.standardSubHelp(
                "It's worth noting that if a user owns both the local and global version " +
                        "of a tag when using this command on a server, the priority when getting content goes " +
                        "to the *local* version, not the global one.",
                true
        )
        this.guildOnly = false
    }

    override fun execute(event: CommandEvent)
    {
        val parts = event.args.split(Regex("\\s+"),2)
        val name = parts[0]
        if(event.isFromType(ChannelType.TEXT))
        {
            val content : String = if(event.localTags.isTag(name, event.guild)) {
                event.localTags.getTagContent(name, event.guild)
            } else if(event.globalTags.isTag(name)) {
                event.globalTags.getTagContent(name)
            } else ""
            if(content.isEmpty())
                return event.replyError("**No Tag Found Matching \"$name\"**\n${SEE_HELP.format(event.client.prefix,this.fullname)}")
            else event.reply("```\n$content```")
        }
        else
        {
            val content : String = if(event.globalTags.isTag(name)) {
                event.globalTags.getTagContent(name)
            } else ""

            if(content.isEmpty())
                return event.replyError("**No Global Tag Found Matching \"$name\"**\n${SEE_HELP.format(event.client.prefix,this.fullname)}")
            else event.reply("```\n$content```")
        }
    }
}

@MustHaveArguments
private class TagOverrideCmd : Command()
{
    init {
        this.name = "Override"
        this.fullname = "Tag Override"
        this.arguments = "[Tag Name] [Tag Content]"
        this.help = "Overrides a local or global tag."
        this.helpBiConsumer = Command.standardSubHelp(
                "It's worth noting that if a user owns both the local and global version " +
                        "of a tag when using this command on a server, the priority when overriding goes " +
                        "to the *local* version, not the global one.",
                true
        )
        this.category = Category.MODERATOR
        this.guildOnly = true
    }

    override fun execute(event: CommandEvent)
    {
        val parts = event.args.split(Regex("\\s+"),2)

        val name = if(parts[0].length<=50)
            parts[0]
        else
            return event.replyError("**Tag names cannot exceed 50 characters in length!**\n" +
                    SEE_HELP.format(event.client.prefix, fullname))

        val newContent = if(parts.size==1)
            return event.replyError("**You must specify content when overriding a tag!**\n" +
                    SEE_HELP.format(event.client.prefix, fullname))
        else if(parts[1].length>1900)
            return event.replyError("**Tag content cannot exceed 1900 characters in length!**\n" +
                    SEE_HELP.format(event.client.prefix, fullname))
        else parts[1]

        if(!event.localTags.isTag(name,event.guild)) {
            if(!event.globalTags.isTag(name)) {
                event.replyError("Tag named \"$name\" does not exist!")
            } else {
                val ownerId = event.globalTags.getTagOwnerId(name)
                val member = event.guild.getMemberById(ownerId)
                if(member!=null && (member.isOwner || event.member.canInteract(member)))
                    return event.replyError("I cannot override the global tag \"**$name**\" because " +
                            "you are not able to interact with them due to role hierarchy placement!")
                event.localTags.overrideTag(newContent, event.globalTags.getOriginalName(name), ownerId, event.guild)
                event.replySuccess("Successfully overrode global tag \"**$name**\"!")
            }
        } else {
            val member = event.guild.getMemberById(event.globalTags.getTagOwnerId(name))
            if(member!=null && (member.isOwner || event.member.canInteract(member)))
                return event.replyError("I cannot override the global tag \"**$name**\" because " +
                        "you are not able to interact with them due to role hierarchy placement!")
            event.localTags.addTag(event.globalTags.getOriginalName(name), 1L, newContent, event.guild)
            event.replySuccess("Successfully overrode global tag \"**$name**\"!")
        }
    }
}

val CommandEvent.localTags : SQLLocalTags
    get() {return this.client.manager.localTags}
val CommandEvent.globalTags : SQLGlobalTags
    get() {return this.client.manager.globalTags}