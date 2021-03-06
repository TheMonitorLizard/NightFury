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
@file:Suppress("unused")
package xyz.nightfury.extensions

import com.neovisionaries.ws.client.WebSocketFactory
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.audio.factory.IAudioSendFactory
import net.dv8tion.jda.core.entities.Game
import net.dv8tion.jda.core.hooks.IEventManager
import net.dv8tion.jda.core.utils.SessionController
import okhttp3.OkHttpClient
import java.util.concurrent.ConcurrentMap

// Copied from club.minnced.kjda.KJDABuilder

fun client(accountType: AccountType, init: JDABuilder.() -> Unit): JDA
    = JDABuilder(accountType).apply(init).buildAsync()

inline infix fun <reified T: JDABuilder> T.token(lazyToken: () -> String): T
    = this.setToken(lazyToken()) as T
inline infix fun <reified T: JDABuilder> T.game(lazy: () -> String): T
    = this.setGame(Game.playing(lazy())) as T
inline infix fun <reified T: JDABuilder> T.listening(lazy: () -> String): T
    = this.setGame(Game.listening(lazy())) as T
inline infix fun <reified T: JDABuilder> T.watching(lazy: () -> String): T
    = this.setGame(Game.watching(lazy())) as T
inline infix fun <reified T: JDABuilder> T.status(lazy: () -> OnlineStatus): T
    = this.setStatus(lazy()) as T
inline infix fun <reified T: JDABuilder> T.manager(lazy: () -> IEventManager): T
    = this.setEventManager(lazy()) as T
inline infix fun <reified T: JDABuilder> T.listener(lazy: () -> Any): T
    = this.addEventListener(lazy()) as T
inline infix fun <reified T: JDABuilder> T.audioSendFactory(lazy: () -> IAudioSendFactory): T
    = this.setAudioSendFactory(lazy()) as T
inline infix fun <reified T: JDABuilder> T.idle(lazy: () -> Boolean): T
    = this.setIdle(lazy()) as T
inline infix fun <reified T: JDABuilder> T.shutdownHook(lazy: () -> Boolean): T
    = this.setEnableShutdownHook(lazy()) as T
inline infix fun <reified T: JDABuilder> T.audio(lazy: () -> Boolean): T
    = this.setAudioEnabled(lazy()) as T
inline infix fun <reified T: JDABuilder> T.autoReconnect(lazy: () -> Boolean): T
    = this.setAutoReconnect(lazy()) as T
inline fun <reified T: JDABuilder> T.webSocketFactory(factory: WebSocketFactory = WebSocketFactory(),
                                                      init: WebSocketFactory.() -> Unit): T
    = this.setWebsocketFactory(factory.apply(init)) as T
inline fun <reified T: JDABuilder> T.httpSettings(builder: OkHttpClient.Builder = OkHttpClient.Builder(),
                                                  init: OkHttpClient.Builder.() -> Unit): T
    = this.setHttpClientBuilder(builder.apply(init)) as T
inline infix fun <reified T: JDABuilder> T.contextMap(lazy: () -> ConcurrentMap<String, String>?): T
    = this.setContextMap(lazy()) as T
inline infix fun <reified T: JDABuilder> T.sessionController(lazy: () -> SessionController): T
    = this.setSessionController(lazy()) as T
inline fun <reified T: JDABuilder> T.listener(vararg listener: Any): T
        = this.addEventListener(*listener) as T
inline fun <reified T: JDABuilder> T.removeListener(vararg listener: Any): T
        = this.removeEventListener(*listener) as T
inline operator fun <reified T: JDABuilder> T.plusAssign(other: Any) { listener(other) }
