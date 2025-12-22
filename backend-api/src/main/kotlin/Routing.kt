package fr.miage.m1

import fr.miage.m1.routes.gridRoutes
import fr.miage.m1.routes.healthRoutes
import io.ktor.server.application.*
import io.ktor.server.routing.*

/**
 * Configuration centrale du routing
 * Regroupe toutes les routes de l'application
 */
fun Application.configureRouting() {
    routing {
        // Routes de santé et de test
        healthRoutes()
        
        // Routes pour la gestion des grilles
        gridRoutes()
    }
}
