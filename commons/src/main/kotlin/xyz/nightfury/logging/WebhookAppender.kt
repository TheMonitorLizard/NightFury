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
package xyz.nightfury.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.IThrowableProxy
import ch.qos.logback.core.AppenderBase
import xyz.nightfury.entities.KEmbedBuilder
import net.dv8tion.jda.webhook.WebhookClient
import net.dv8tion.jda.webhook.WebhookClientBuilder
import xyz.nightfury.util.ext.hocon
import xyz.nightfury.util.ignored
import java.awt.Color
import java.time.OffsetDateTime
import java.util.*

/**
 * @author Kaidan Gustave
 */
class WebhookAppender : AppenderBase<ILoggingEvent>() {
    override fun start() = Companion.start()
    override fun append(eventObject: ILoggingEvent) = Companion.append(eventObject)
    override fun stop() = Companion.stop()

    companion object : AppenderBase<ILoggingEvent>() {
        private const val EMBED_LIMIT = 750
        private val PACKAGE_REGEX = Regex("\\.")

        private lateinit var CLIENT: WebhookClient

        var IS_INITIALIZED = true
            private set

        override fun start() {
            super.start()
            val hocon = hocon {
                setSource { this::class.java.getResourceAsStream("/webhook.conf").bufferedReader(Charsets.UTF_8) }
            }
            val idNode = hocon.getNode("webhook", "id")
            val tokenNode = hocon.getNode("webhook", "token")
            if(idNode.isVirtual || tokenNode.isVirtual) {
                this.IS_INITIALIZED = false
            } else {
                val id = idNode.long
                val token = tokenNode.getList { "$it" }.joinToString("_")
                this.IS_INITIALIZED = true
                this.CLIENT = WebhookClientBuilder(id, token).apply {
                    setThreadFactory { Thread(it, "WebhookLogger").apply { isDaemon = true } }
                }.build()
            }
        }

        override fun append(event: ILoggingEvent) {
            if(!IS_INITIALIZED) return
            ignored {
                CLIENT.send {
                    title { event.loggerName.split(PACKAGE_REGEX).run { this[size - 1] } }
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
            }
        }

        override fun stop() {
            super.stop()
            CLIENT.close()
        }

        private fun colorFromLevel(level: Level): Color? {
            return when(level) {
                Level.INFO  -> Color.BLUE
                Level.WARN  -> Color.ORANGE
                Level.ERROR -> Color.RED
                Level.DEBUG -> Color.YELLOW
                else        -> null
            }
        }

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

        private fun WebhookClient.send(embed: KEmbedBuilder.() -> Unit) = with(KEmbedBuilder()) {
            embed()
            send(this.build())
        }
    }
}