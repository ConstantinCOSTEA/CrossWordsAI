package fr.miage.m1

import fr.miage.m1.routes.crosswordRoutes
import fr.miage.m1.routes.healthRoutes
import io.ktor.server.application.*
import io.ktor.server.routing.*

/**
 * Configuration centrale du routing
 */
fun Application.configureRouting() {
    routing {
        // Routes de santé
        healthRoutes()
        
        // Routes mots croisés
        crosswordRoutes()
    }
}
