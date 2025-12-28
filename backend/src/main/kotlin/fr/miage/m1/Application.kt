package fr.miage.m1

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json

/**
 * Point d'entrée de l'application Ktor
 */
fun main() {
    val server = embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        module()
    }

    Runtime.getRuntime().addShutdownHook(Thread {
        server.stop(1000, 2000)
    })
    
    server.start(wait = true)
}

/**
 * Module principal de l'application
 */
fun Application.module() {
    // Configuration JSON
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
    
    // Configuration du routing
    configureRouting()

    monitor.subscribe(ApplicationStopping) {
        log.info("Arrêt du serveur CrossWordsAI...")
    }
    
    // Log de démarrage
    log.info("CrossWordsAI Backend demarre sur http://localhost:8080")
}
