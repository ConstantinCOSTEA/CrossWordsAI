# 📝 Structure du Projet (Dossiers Principaux)

Le code source de l'application est organisé en trois répertoires principaux :

---

### 📂 `java + kotlin`
Ce dossier contient l'ensemble du code source de l'application, incluant les répertoires :
- `src` : code principal  
- `androidTest` et `test` : tests unitaires et d'intégration  

---

### 🎨 `res`
Ce répertoire regroupe toutes les ressources de l'application (comme les fichiers XML).  
Il contient notamment les définitions de :
- `colors` : couleurs  
- `strings` : textes  
- `theme` : thèmes et styles  

---

### ⚙️ `res generated`
Ce dossier est automatiquement généré par **Android Studio** lors de la compilation  
et **ne doit pas être modifié manuellement**.

---

# 💻 Logique Applicative (`src`)

Le répertoire `src` contient l'architecture essentielle de la logique métier et de l'interface utilisateur :

---

### 🧩 `ui`
Ce dossier est le cœur de la logique de l'interface utilisateur.  
Il contient :

- **`MainActivity`** : le composant principal qui gère l'application dans son ensemble,  
  y compris l'affichage du menu général (`menuView`).

- **Différentes views** : les vues spécifiques qui composent le `menuView`,  
  notamment celles associées aux quatre icônes de la barre de tâches (ou **barre de navigation**).

---

### 🎨 `theme`
Ce répertoire contient les définitions supplémentaires relatives au **thème de l'application**,  
au-delà des ressources de base situées dans `res`.