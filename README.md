# LOG2990 - Code de base

Code de base à utiliser pour démarrer le développement de votre projet.
Le code contient 2 projets séparés :

-   client : le site Web fait avec le cadriciel(_framework_) **Angular**.
-   serveur : le serveur dynamique bâti avec la librairie **Express** ou **NestJs** ([selon votre choix](#choix-du-serveur)).

# Commandes npm

Les commandes commençant par `npm` devront être exécutées dans les dossiers `client` et `server`. Les scripts non standard doivent être lancés en lançant `npm run myScript`.

## Installation des dépendances de l'application

1. Installer `npm`. `npm` vient avec `Node` que vous pouvez télécharger [ici](https://nodejs.org/en/download/)

2. Lancer `npm ci` (Continuous Integration) pour installer les versions exactes des dépendances du projet. Ceci est possiblement seulement si le fichier `package-lock.json` existe. Ce fichier vous est fourni dans le code de départ.

## Ajout de dépendances aux projets

Vous pouvez ajouter d'autres dépendances aux deux projets avec la commande `npm install nomDependance`.

Pour ajouter une dépendance comme une dépendance de développement (ex : librairie de tests, types TS, etc.) ajoutez l'option `--save-dev` : `npm install nomDependance --save-dev`.

Un ajout de dépendance modifiera les fichiers `package.json` et `package-lock.json`.

**Important** : assurez-vous d'ajouter ces modifications dans votre Git. N'installez jamais une dépendance du projet globalement.

# Client

## Développement local

Lorsque la commande `npm start` est lancée dans le dossier _/client_, le script suivant (disponible dans `package.json`) est exécuté : `ng serve --open` qu exécute les 2 étapes suivantes :

1. **Bundle generation** : Traduire le code TypeScript et la syntaxe Angular en JavaScript standard. À la fin de cette étape, vous verrez que les fichiers suivants sont générés : `vendor.js`,`polyfills.js`, `main.js`,`runtime.js` et `styles.css`. Ces fichiers continent le code de votre application ainsi que le CSS des différents Components.

    **Note** : ceci est un _build_ de développement : la taille des fichiers est très grande et le code n'est pas minifié. Vous pouvez accéder à votre code à travers les outils de développement de votre navigateur et déboguer avec des _breaking points_ par exemple. Une configuration de _debugger_ pour VSCode est également disponible. Voir la section _Debugger_ pour plus d'informations.

2. **Development Server** : un serveur web statique sera lancé sur votre machine pour pouvoir servir votre application web. Le serveur est lancé sur le port 4200 et est accessible à travers `http://localhost:4200/` ou `127.0.0.1:4200`. Une page web avec cette adresse s'ouvrira automatiquement.

    **Note** : le serveur de développement n'est accessible qu'à partir de votre propre machine. Vous pouvez le rendre disponible à tous en ajoutant `--host 0.0.0.0` dans la commande `npm start`. Le site sera donc accessible dans votre réseau local à partir de votre adresse IP suivie du port 4200. Par exemple : `132.207.5.35:4200`. Notez que le serveur de développement n'est pas fait pour un déploiement ouvert et vous recevrez un avertissement en le lançant.

### Génération de composants du client

Pour créer de nouveaux composants, nous vous recommandons l'utilisation d'angular CLI. Il suffit d'exécuter `ng generate component component-name` pour créer un nouveau composant.

Il est aussi possible de générer des directives, pipes, services, guards, interfaces, enums, modules, classes, avec cette commande `ng generate directive|pipe|service|class|guard|interface|enum|module`.

## Aide supplémentaire et documentation

Pour obtenir de l'aide supplémentaire sur Angular CLI, utilisez `ng help` ou [Angular CLI README](https://github.com/angular/angular-cli/blob/master/README.md).

Pour la documentation d'Angular, vous pouvez la trouver [ici](https://angular.io/docs)

Pour obtenir de l'aide supplémentaire sur les tests avec Angular, utilisez [Angular Testing](https://angular.io/guide/testing)

# Serveur

## Développement local

Lorsque la commande `npm start` est lancée dans le dossier _/server_, le script suivant (disponible dans `package.json`) est exécuté : `nodemon` qui effectue 2 étapes similaires au client :

1. **Build** : transpile le code TypeScript en JavaScript et copie les fichiers dans le répertoire `/out`.

    **Note** : L'outil `nodemon` est un utilitaire qui surveille pour des changements dans vos fichiers `*.ts` et relance automatiquement le serveur si vous modifiez un de ses fichiers. La modification d'un autre fichier nécessitera un relancement manuel du serveur (interrompre le processus et relancer `npm start`).

2. **Deploy** : lance le serveur à partir du fichier `index.js`. Le serveur est lancé sur le port 3000 et est accessible à travers `http://localhost:3000/` ou `127.0.0.1:3000`. Le site est aussi accessible dans votre réseau local à partir de votre adresse IP suivie du port 3000. Par exemple : `132.207.5.35:3000`. Un _debugger_ est également attaché au processus Node. Voir la section _Debugger_ pour plus d'information.

    **Note** : ceci est un serveur dynamique qui ne sert pas des pages HTML, mais répond à des requêtes HTTP. Pour savoir comment accéder à sa documentation et connaître plus sur son fonctionnement, consultez la section _Documentation du serveur_ dans ce document.

## Documentation du serveur

La documentation de votre serveur est disponible en format OpenAPI sur la route suivante : `/api/docs`
Pour y accéder, vous pouvez aller à `<url_serveur>/api/docs` une fois le serveur démarré.

Cette page décrit les différentes routes accessibles sur le serveur ainsi qu'une possibilité de les tester en envoyer des requêtes au serveur. Vous n'avez qu'à choisir une des routes et appuyer sur le bouton "Try it out" et lancer la requête avec "Execute".
