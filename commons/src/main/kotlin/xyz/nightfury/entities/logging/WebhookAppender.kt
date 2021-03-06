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
package xyz.nightfury.entities.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.IThrowableProxy
import ch.qos.logback.core.AppenderBase
import xyz.nightfury.entities.KEmbedBuilder
import net.dv8tion.jda.webhook.WebhookClient
import net.dv8tion.jda.webhook.WebhookClientBuilder
import ninja.leaping.configurate.hocon.HoconConfigurationLoader
import java.awt.Color
import java.time.OffsetDateTime
import java.util.*

/**
 * @author Kaidan Gustave
 */
class WebhookAppender: AppenderBase<ILoggingEvent>() {

    private lateinit var client: WebhookClient

    init {
        val hocon = HoconConfigurationLoader.builder()
            .setSource { this::class.java.getResourceAsStream("/bot.conf").bufferedReader(Charsets.UTF_8) }
            .build().load()

        val idNode = hocon.getNode("webhook", "id")
        val tokenNode = hocon.getNode("webhook", "token")

        if(idNode.isVirtual || tokenNode.isVirtual) {
            isInitialized = false
        } else {
            isInitialized = true
            client = WebhookClientBuilder(idNode.long, tokenNode.string)
                .setThreadFactory { Thread(it, "WebhookLogger").apply { isDaemon = true } }
                .build()
        }
    }

    override fun append(event: ILoggingEvent) {
        if(!isInitialized) return

        try {
            client.send {
                title { event.loggerName.split(packageRegex).run { this[size - 1] } }
                append(event.formattedMessage)
                val proxy = event.throwableProxy
                if(proxy != null)
                    append("\n\n${buildStackTrace(proxy)}")
                color { colorFromLevel(event.level) }
                footer { value = "Logged at" }
                time {
                    val gmt = Calendar.getInstance(TimeZone.getTimeZone("GMT"))
                    gmt.timeInMillis = event.timeStamp
                    OffsetDateTime.ofInstant(gmt.toInstant(), gmt.timeZone.toZoneId())
                }
            }
        } catch(ignored: Throwable) {}
    }

    override fun stop() {
        super.stop()
        client.close()
    }

    companion object {
        var isInitialized: Boolean = true

        private val packageRegex: Regex = Regex("\\.")

        private fun colorFromLevel(level: Level) = when(level) {
            Level.INFO  -> Color.BLUE
            Level.WARN  -> Color.ORANGE
            Level.ERROR -> Color.RED
            Level.DEBUG -> Color.YELLOW
            else        -> null
        }

        private const val EMBED_LIMIT = 750

        private fun buildStackTrace(proxy: IThrowableProxy) = buildString {
            append("```java\n")
            append(proxy.className)
            val message = proxy.message

            if(message != null)
                append(": $message")
            append("\n")

            val arr = proxy.stackTraceElementProxyArray
            for((index, element) in arr.withIndex()) {
                val str = element.steAsString
                if(str.length + length > EMBED_LIMIT) {
                    append("\t... (${arr.size - index + 1} more calls)")
                    break
                }
                append("\t$str\n")
            }
            append("```")
        }
    }

    private fun WebhookClient.send(embed: KEmbedBuilder.() -> Unit) = with(KEmbedBuilder()) {
        embed()
        send(this.build())
    }
}
