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
@file:Suppress("unused")
package me.kgustave.nightfury.extensions

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import me.kgustave.nightfury.resources.Fraction
import me.kgustave.nightfury.resources.over
import net.dv8tion.jda.core.entities.*
import java.time.OffsetDateTime

infix fun List<User>.multipleUsers(argument: String) = listOut("user", argument) { it.formattedName(true) }
infix fun List<Member>.multipleMembers(argument: String) = listOut("member", argument) { it.user.formattedName(true) }
infix fun List<TextChannel>.multipleTextChannels(argument: String) = listOut("text channel", argument) { it.asMention }
infix fun List<VoiceChannel>.multipleVoiceChannels(argument: String) = listOut("voice channel", argument) { it.name }
infix fun List<Role>.multipleRoles(argument: String) = listOut("role", argument) { it.name }

private inline fun <T> List<T>.listOut(kind: String, argument: String, conversion: (T) -> String) = with(StringBuilder()) {
    append("Multiple ${kind}s found matching \"$argument\":\n")
    for(i in 0..3)
    {
        append("${this@listOut[i].let(conversion)}\n")
        if(i==3 && this@listOut.size>4)
            append("And ${this@listOut.size-4} other $kind${if(this@listOut.size-4 > 1) "s..." else "..."}")
        if(this@listOut.size==i+1)
            break
    }
    return@with toString()
}

infix fun User.formattedName(boldName: Boolean) = "${if(boldName) "**$name**" else name}#$discriminator"

inline val OffsetDateTime.readableFormat
    inline get() = "${dayOfWeek.niceName}, ${month.niceName} $dayOfMonth, $year"

fun noMatch(lookedFor: String, query: String) = "Could not find any $lookedFor matching \"$query\"!"

inline val <T: Enum<*>> T.niceName : String
    inline get() = run { "${name[0]}${name.substring(1).toLowerCase()}" }

inline val <T: AudioTrackInfo> T.formattedInfo : String
    inline get() = "**${title.filterMassMention()}** `[${formatTrackTime(length)}]`"

inline val <T: AudioTrack> T.formatTimeRemaining : Fraction
    inline get() = (position / 1000.0) over (duration / 1000)

fun formatTrackTime(duration: Long): String
{
    if(duration == Long.MAX_VALUE) return "LIVE"

    var seconds: Long = Math.round(duration / 1000.0)
    val hours: Long = seconds / (60 * 60)
    seconds %= (60 * 60).toLong()
    val minutes: Long = seconds / 60
    seconds %= 60
    return  (if(hours > 0) "$hours:" else "") +
            "${if(minutes < 10) "0$minutes" else minutes.toString()}:" +
            (if(seconds < 10) "0$seconds" else seconds.toString())
}

fun String.filterMassMention() = replace("@everyone", "@\u0435veryone").replace("@here", "@h\u0435re").trim()