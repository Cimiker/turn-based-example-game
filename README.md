# turn_based_example_game

A [libGDX](https://libgdx.com/) project generated with [gdx-liftoff](https://github.com/libgdx/gdx-liftoff).

This project was generated with a template including simple application launchers and an `ApplicationAdapter` extension that draws libGDX logo.

## Gameplay description
The goal of the game is to get rid of all your cards. You play cards to the play pile in the middle. You can play only one card per turn, and your card must have the same colour or symbol. If you cannot play any cards, you must draw from the draw pile. There are special cards that reverse the turn order, skip the next player, or make them draw cards. There are also wild cards that can be played on all cards. If a player has TWO cards in their hand, a DUO button will appear. If the player who has 2 cards clicks it before anyone else, they are protected. If not, they must draw cards.
TL;DR This is an UNO clone

## How to run
1. Open the project in an IDE, preferably IntelliJ. While it's possible to run the game from the command line, running multiple clients simultaneously can be tricky.
2. Launch the server by running ServerLauncher.java from the server package.
3. Launch two clients by running Lwjgl3Launcher.java from the lwjgl3 package.
- It’s recommended to start the clients by pressing the green triangle at the top of the screen in IntelliJ.
- To avoid issues when running multiple clients, select "Current file" in the Run Configuration settings.
4. Start playing!
  
## Platforms / Main modules
- `core`: Main module with the application logic shared by all platforms.
- `lwjgl3`: Primary desktop platform using LWJGL3; was called 'desktop' in older docs.
- `server`: A separate application without access to the `core` module.

## Other Files and Directories
- assets: Contains game resources like sprites, images, fonts, and sounds used by the client.
- gradle/wrapper: No need to modify. Contains files for the Gradle wrapper, ensuring a consistent Gradle version across different environments.
- .editorconfig: No need to modify. Defines coding style rules, such as indentation and line endings, in the whole project.
- .gitattributes: No need to modify. This .gitattributes file ensures consistent line endings for .bat files.
- .gitignore: Tells Git which files and directories to exclude from version control.
- build.gradle: No need to modify. Defines the build configuration for a Gradle project, including dependencies, plugins, and tasks.
- gradle.properties: No need to modify. Contains project-specific properties and configuration settings, such as version numbers and JVM options.
- gradlew: No need to modify. Script that runs Gradle tasks with a specific version.
- gradlew.bat: No need to modify. Script that runs Gradle tasks on Windows without a pre-installed Gradle.
- settings.gradle: No need to modify. Configures the structure of a multi-project Gradle build, specifying which subprojects are included.

## Gradle

This project uses [Gradle](https://gradle.org/) to manage dependencies.
The Gradle wrapper was included, so you can run Gradle tasks using `gradlew.bat` or `./gradlew` commands.
Useful Gradle tasks and flags:

- `--continue`: when using this flag, errors will not stop the tasks from running.
- `--daemon`: thanks to this flag, Gradle daemon will be used to run chosen tasks.
- `--offline`: when using this flag, cached dependency archives will be used.
- `--refresh-dependencies`: this flag forces validation of all dependencies. Useful for snapshot versions.
- `build`: builds sources and archives of every project.
- `cleanEclipse`: removes Eclipse project data.
- `cleanIdea`: removes IntelliJ project data.
- `clean`: removes `build` folders, which store compiled classes and built archives.
- `eclipse`: generates Eclipse project data.
- `idea`: generates IntelliJ project data.
- `lwjgl3:jar`: builds application's runnable jar, which can be found at `lwjgl3/build/libs`.
- `lwjgl3:run`: starts the application.
- `server:run`: runs the server application.
- `test`: runs unit tests (if any).

Note that most tasks that are not specific to a single project can be run with `name:` prefix, where the `name` should be replaced with the ID of a specific project.
For example, `core:clean` removes `build` folder only from the `core` project.
