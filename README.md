# Codenames + 
#### SoPra FS25

## Introduction 
Codenames + is a fun and easy way to play the classic board game with friends, right in your browser. two teams compete to find their secret words on the board using one-word hints from their Spymaster. It's all about clever thinking, teamwork and avoiding the assasin word that ends the game immediatly. Easy to play and fun every time! 

To keep the gameplay exciting, the game offers multiple modes: 

- Classic Mode: Standard randomized words for a traditional experience.
- Own Words Mode: Players can contribute their own words to build a fully customized board. 
- Theme Mode: Players can set a theme and the game auto generates words related to that theme. 
- Timed Mode: Adds real-time pressure by limiting how long teams have to give hints or make guesses. 

## Technologies 
### Development
- Java
- Spring Boot
- Gradle 
### Persistence 
- MongoDB 
### Websockets 
- Spring WebSocket (STOMP)
### Testing 
- JUnit
- Mockito
### External APIs
- Google Gemini 1.5 Flash API 

## High-level components 
The server-side of our project handles all the core logic and data persistence necessary to run the Codenames + game. 

Here are the most important components: 

- ### LobbyService ([LobbyService.java](https://github.com/Fisssch/SoPra_MG_Server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/service/LobbyService.java))
This class manages the lifecycles of lobbies, including their creation, player management and timed automatic deletion. It handles the role assignments and triggers the game start logic once the conditions are met. 
- ### GameService ([GameService.java](https://github.com/Fisssch/SoPra_MG_Server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/service/GameService.java))
This class handles the entire gameplay logic which includes, turn management, hint validation, handling guesses, tracking game progress and finally determine the winning team. It interacts with the Game, Card, Team and Player entities. 
- ### WebsocketService ([Websocketservice.java](https://github.com/Fisssch/SoPra_MG_Server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/service/WebsocketService.java))
This components is responsible for pushing real-time updates to connected clients over WebSockets. It keeps all the clients in sync regarding lobby changes or game events. 
- ### WordGenerationService ([WordGenerationService.java](https://github.com/Fisssch/SoPra_MG_Server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/service/WordGenerationService.java))
Fetches a list of 25 words for the game from the Gemini API based on the selected theme. 

## Launch & Deployment 

### Building with Gradle
You can use the local Gradle Wrapper to build the application.
-   macOS: `./gradlew`
-   Linux: `./gradlew`
-   Windows: `./gradlew.bat`

More Information about [Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html) and [Gradle](https://gradle.org/docs/).

### Build

```bash
./gradlew build
```

### Run 

```bash
./gradlew bootRun
```

You can verify that the server is running by visiting `localhost:8080` in your browser. 

#### API Key
In order to run this project locally you need your own Gemini API Key. Create a local.properties file in the root directory and then add the following line to it: 

- **API_KEY = YOUR_KEY** (Replace YOUR_KEY with your actual Gemini API Key).

This file is automatically read by the build.gradle file at runtime and injected into the app. Note that **local.properties** is ignored by version control to keep your API Key safe. 

### Test

```bash
./gradlew test
```

### Development Mode
You can start the backend in development mode, this will automatically trigger a new build and reload the application
once the content of a file has been changed.

Start two terminal windows and run:

`./gradlew build --continuous`

and in the other one:

`./gradlew bootRun`

If you want to avoid running all tests with every change, use the following command instead:

`./gradlew build --continuous -xtest`



## Roadmap 
- Game history tracking: store the game history data so players can review their past games, including winners, guesses and used hints. 
- Admin tools for lobby moderation: allow lobby creators to kick players or assign roles manually. 


## Authors and acknowledgment 
- [Silvan Wyss](https://github.com/Fisssch)
- [Mathis Beeler](https://github.com/beelermathis)
- [Elia Wyrsch](https://github.com/eliawy)
- [Luis Schmid](https://github.com/LooPyt)
- [Helinton Philip Pathmarajah](https://github.com/Helinton-Philip-Pathmarajah)

We especially thank Youssef Farag and all teaching assistants of the module Software Engineering Praktikum at UZH for their feedback at support during our project. 

## License 

This project is licensed under the [Apache License 2.0](https://github.com/Fisssch/SoPra_MG_Server/blob/main/LICENSE)
