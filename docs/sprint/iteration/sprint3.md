# Sprint 3 — du 19/11/25 au 03/12/25
Durée : 2 semaines
Équipe : Alexis, Maëlla, Constantin, Martin, Valentin

## Objectifs du Sprint :
Développement et validation des composants cœurs en isolation : Visualisation, OCR et Intelligence de résolution.

## Tâches liée à une personne :

Alexis : met en place l'architecture serveur incluant le traitement d'image (OpenCV) et l'OCR. Il a développé les routes de l'API, établi la première communication technique avec le front-end et validé les échanges de données via Postman.

Maëlla : travaille sur la logique algorithmique côté back-end en développant un agent simple pour la résolution de la grille.

Constantin : co-développe l'affichage de la grille vide et mène les recherches techniques sur l'architecture de communication pour envoyer les données au serveur.

Martin : intègre l'API Auth0 pour gérer l'authentification des utilisateurs.

Valentin : ouvre les issues nécessaires et met à jour le fichier Sprint.md ainsi que le Readme.

## Description des commits :
### Commit 1 : Mise en place de l'architecture Backend (Vision & OCR)

Ce commit instaure les fondations du serveur et intègre les fonctionnalités clés de traitement d'image :

Architecture & Configuration : Initialisation des fichiers cœur de l'application (Application.kt, Routing.kt) et configuration complète du build Gradle (dépendances).

Analyse d'image (OpenCV) : Implémentation du moteur d'analyse de grille (GridAnalyzer.kt) pour détecter la structure visuelle.

OCR (Reconnaissance de texte) : Intégration de l'extraction de questions (QuestionExtractor.kt), supportant à la fois Tesseract (local) et des APIs externes.

API & Routes : Création des endpoints (points d'entrée) pour l'analyse de grille, l'extraction de texte, ainsi qu'une route "Health Check" pour surveiller l'état du serveur.

Modèles de données : Définition des structures (DTOs) pour normaliser les réponses de l'API (GridModels.kt).

-- 