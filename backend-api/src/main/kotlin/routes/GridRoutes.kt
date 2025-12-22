package fr.miage.m1.routes

import fr.miage.m1.controllers.grid.GridController
import io.ktor.server.routing.*

/**
 * Routes pour la gestion des grilles de mots croisés
 */
fun Route.gridRoutes() {
    val gridController = GridController()
    
    // POST /analyze-grid - Analyser la structure de la grille
    post("/analyze-grid") {
        gridController.analyzeGrid(call)
    }
    
    // POST /extract-questions - Extraire les questions avec OCR
    post("/extract-questions") {
        gridController.extractQuestions(call)
    }
    
    // POST /test-ocr - Tester l'OCR (retourne le texte brut)
    post("/test-ocr") {
        gridController.testOCR(call)
    }
    
    // GET /images/{filename} - Servir les images générées
    get("/images/{filename}") {
        gridController.serveImage(call)
    }
}