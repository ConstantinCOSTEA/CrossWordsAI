# Sprint 1 — du 15/10/25 au 05/11/25
Durée : 2 semaines
Équipe : Alexis, Maëlla, Constantin, Martin, Valentin

## Objectifs du Sprint :
* Déployer l'architecture fondamentale de l'application Android (Jetpack Compose).
* Mettre en place la structure de navigation et les écrans principaux (Home, Caméra, Settings).
* Initialiser les veilles techniques (API, Architecture) et la documentation projet (README, sprint.md).

## Tâches liée à une personne :
Alexis : Veille API, Rédaction de la documentation et du compte rendu de veille
Maëlla : Veille architecture, Rédaction de la documentation et du compte rendu de veille
Constantin : Déploiement de la base de l'application (Architecture, Navigation, Modèles)
Martin : Déploiement de la base de l'application (Écrans : Home, Picture, Settings)
Valentin : Ajout du ReadMe, Rédaction de la documentation du sprint (sprint1.md)

## Description des commits :
### Commit 1 : Initialisation du Projet de Mots Croisés & Structure Fondamentale
**Objectif :** Mise en place de l'architecture de base, des écrans principaux (Home, Caméra, Settings) et des fondations techniques du projet Android.

**Détails :**
* **Architecture & Navigation :** Déploiement de l'architecture **Jetpack Compose (API 33)**, intégrant un système de navigation à **3 onglets** (Home, Picture, Settings) via `NavigationSuiteScaffold`. Configuration du thème **Material Design 3** et intégration du `SplashScreen`.
* **Fonctionnalités Clés :**
    * **HomeView :** Affichage des puzzles avec cartes interactives montrant les détails et la progression, avec animation d'expansion pour les options (Continuer/Supprimer).
    * **PictureView :** Intégration de **CameraX** pour la capture d'images, incluant la gestion des permissions.
    * **SettingsView :** Interface de paramètres structurée, avec gestion du thème (clair/sombre/système) et contrôles pour les futures fonctionnalités IA.
* **Technique :** Création des modèles de données initiaux (`CrosswordPuzzle`, `AppSettings`), support **Edge-to-Edge display** et utilisation des `Previews Composable`.

---

## Rétrospective
Ce qui a bien fonctionné 💪
* Bonne communication dans l’équipe
* Livrables terminés dans les temps

Ce qui reste à améliorer ⚙️
* Meilleure gestion du backlog en milieu de sprint
* Clarifier les responsabilités sur certaines tâches

Actions pour le prochain sprint 🚀
* Planifier une revue intermédiaire
* Ajouter un point de suivi quotidien plus court

## 💾 Liens utiles
* Tableau Trello / Jira du sprint
* Pull Requests liées
* [Documentation technique](../docs/)