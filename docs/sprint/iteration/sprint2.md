# Sprint 2 — du 05/11/25 au 19/11/25
Durée : 2 semaines
Équipe : Alexis, Maëlla, Constantin, Martin, Valentin

## Objectifs du Sprint :
Mettre en place les architectures backend (serveur et multi-agents), implémenter le modèle MVVM dans l'application Android et intégrer l'authentification Auth0.

## Tâches liée à une personne :
Alexis : s'occupe de l'implémentation de l'architecture du serveur en créant un dossier serveur avec la configuration de base.

Maëlla : travaille sur l'implémentation de l'architecture multi-agents en créant le dossier agents.

Constantin : implémente le modèle MVVM pour l'application.

Martin : intègre l'API Auth0 pour gérer l'authentification des utilisateurs.

Valentin : ouvre les issues nécessaires et met à jour le fichier Sprint.md ainsi que le Readme.

## Description des commits :
### Commit 1 : Intégration de l'authentification Auth0
**Objectif :** Mise en place d'un système d'authentification sécurisé avec Auth0 pour gérer l'identification des utilisateurs dans l'application Android.

**Détails :**
* **Configuration Auth0 :** Initialisation du SDK Auth0 dans `MainActivity` via `Auth0.getInstance()` en utilisant les identifiants stockés dans `strings.xml` (client ID et domaine). La configuration inclut également un schéma personnalisé (`demo`) pour gérer les redirections OAuth.

* **Flux d'authentification :** 
    * **Connexion :** La méthode `login()` utilise `WebAuthProvider.login()` pour lancer le flux d'authentification web. L'utilisateur est redirigé vers la page de connexion Auth0 via un navigateur web intégré. Les scopes demandés incluent `openid`, `profile`, `email` et `offline_access` pour obtenir un accès complet aux informations utilisateur et un refresh token.
    * **Gestion de la session :** Après une authentification réussie, les informations utilisateur sont stockées dans un objet `UserSession` (défini dans ```MainActivity.kt```) qui contient l'état d'authentification, le nom d'utilisateur (récupéré depuis `givenName`, `nickname` ou `email`) et le token d'accès.
    * **Déconnexion :** La méthode `logout()` utilise `WebAuthProvider.logout()` pour déconnecter l'utilisateur et réinitialiser l'état de la session.

* **Gestion des callbacks :** L'application gère les redirections OAuth via `onNewIntent()` qui intercepte les intents de retour d'Auth0. La configuration dans `AndroidManifest.xml` définit un intent-filter avec le schéma personnalisé pour capturer les callbacks d'authentification.

* **Intégration UI :** L'interface `SettingsView` affiche l'état de connexion de l'utilisateur et propose des boutons "Sign In" / "Sign Out" qui déclenchent respectivement les fonctions `onLogin` et `onLogout` passées depuis `MainActivity`. L'état d'authentification est propagé à travers l'application via le paramètre `session` de `CrossWordsAIApp`.

* **Gestion des erreurs :** Les exceptions d'authentification (`AuthenticationException`) sont capturées et loggées pour faciliter le débogage en cas d'échec de connexion ou de déconnexion.

-- 