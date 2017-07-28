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
package me.kgustave.nightfury.commands.dev

import me.kgustave.nightfury.*

/**
 * @author Kaidan Gustave
 */
class ModeCmd : Command() {

    init {
        this.name = "mode"
        this.arguments = Argument("<mode>")
        this.help = "sets the bots mode"
        this.guildOnly = false
        this.devOnly = true
        this.category = Category.OWNER
    }

    override fun execute(event: CommandEvent)
    {
        try {
            event.client.targetListener(event.args)
            event.replySuccess("Targeted listener `${event.args.toLowerCase()}`!")
        } catch (e : IllegalArgumentException) {
            if(e.message!=null) event.replyError(e.message!!)
            else throw e
        }
    }
}