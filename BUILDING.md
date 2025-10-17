# Build Instructions

## Prérequis
- Java 21 JDK installé sur votre machine.
- IntelliJ IDEA (Community ou Ultimate).

## Importer le projet dans IntelliJ
1. Ouvrez IntelliJ et choisissez **Open**.
2. Sélectionnez le dossier `Outlaw-Minecraft-Bounty` contenant le fichier `pom.xml`.
3. IntelliJ devrait détecter automatiquement que c'est un projet Maven et proposer l'ouverture de la fenêtre Maven (icône en forme de lettre "M" sur le côté droit). Si la fenêtre n'apparaît pas :
   - Allez dans **File → Settings → Build, Execution, Deployment → Build Tools → Maven**.
   - Cochez **Enable Maven**.
   - Si le plugin Maven est désactivé, ouvrez **File → Settings → Plugins**, recherchez "Maven" et activez-le.

## Compiler le plugin en JAR depuis IntelliJ
1. Ouvrez la fenêtre **Maven** (généralement à droite).
2. Dans **Lifecycle**, double-cliquez sur **package**.
3. Maven génère le fichier `target/OutlawBounties-1.0.1.jar` que vous pouvez déposer dans votre dossier `plugins/` du serveur Paper.

## Compiler depuis la ligne de commande
Si vous préférez compiler sans passer par IntelliJ :

```bash
mvn -DskipTests package
```

Le fichier JAR sera également disponible dans `target/OutlawBounties-1.0.1.jar`.

> 💡 **Astuce :** si Maven n'est pas installé sur votre machine, vous pouvez l'ajouter via votre gestionnaire de paquets (Chocolatey, Homebrew, apt, etc.).
