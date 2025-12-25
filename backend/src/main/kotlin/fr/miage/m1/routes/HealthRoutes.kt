package fr.miage.m1.routes

import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Routes de santé et de test
 */
fun Route.healthRoutes() {
    // GET / - Route de test
    get("/") {
        call.respondText("CrossWordsAI Backend - Prêt !")
    }
    
    // GET /health - Health check
    get("/health") {
        call.respond(mapOf(
            "status" to "UP",
            "service" to "CrossWordsAI Backend",
            "version" to "1.0.0"
        ))
    }
}
