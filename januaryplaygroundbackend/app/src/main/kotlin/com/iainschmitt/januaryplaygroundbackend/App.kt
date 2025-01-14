/*
 * This source file was generated by the Gradle 'init' task
 */
package com.iainschmitt.januaryplaygroundbackend

import io.javalin.Javalin
import io.javalin.websocket.WsContext
import java.util.concurrent.ConcurrentHashMap

private val userMap = ConcurrentHashMap<WsContext, String>()
private var usercount = 0

class CredentialsDto(val username: String, val password: String)

fun main(vararg args: String) {
    if (args.size < 2) {
        throw IllegalArgumentException("Empty args")
    }
    val db = DatabaseHelper(args[0])

    val secure =
        when (args[1]) {
            "insecure" -> false
            "secure" -> true
            else -> throw IllegalArgumentException("Invalid `cookieSecure`")
        }

    val app = Javalin.create({ config ->
        config.bundledPlugins.enableCors { cors ->
            cors.addRule {
                it.allowHost("http://localhost:5173")
                it.allowCredentials = true
            }
        }
    }).start(7070)
    val auth = Auth(db, secure)
    app.get("/health") { ctx -> ctx.result("Up") }
    app.post("/auth/signup") { ctx -> auth.signUpHandler(ctx) }
    app.post("/auth/login") { ctx -> auth.logInHandler(ctx) }
    app.get("/auth/test") { ctx -> auth.testAuthHandler(ctx) }

    // TODO: Add auth here
    app.ws("/ws") { ws ->
        ws.onConnect { ctx ->
            val username = "User" + usercount++
            userMap[ctx] = username
            println("Connected: $username")
        }
        ws.onClose { ctx ->
            val username = userMap[ctx]
            userMap.remove(ctx)
            println("Disconnected: $username")
        }
    }

    // startServerEventSimulation()
}

private fun startServerEventSimulation() {
    Thread {
        while (true) {
            Thread.sleep(5000) // Every 5 seconds
            val serverUpdate = "Server time: ${System.currentTimeMillis()}"
            println("Starting server send process")
            userMap.keys.filter { it.session.isOpen }.forEach { session ->
                session.send(serverUpdate)
            }
        }
    }
        .start()
}
