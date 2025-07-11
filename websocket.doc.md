# Websockets

| Path | Description | Data |
| ---- | ----------- |------|
| /topic/lobby/{id}/players | Player changes status or role or team. Id und data im body. | `ready: bool, color: string, role: string, playerId: long` <- Updated data of player |
| /topic/lobby/{id}/gameMode | Game mody of lobby is changed. | `gameMode: string` <- new game mode |
| /topic/lobby/{id}/addPlayer | Player is added to lobby. | `ready: bool, color: string, role: string, playerId: long` <- Data of added player |
| /topic/lobby/{id}/removePlayer | Player is removed from lobby. | `id: long` <- ID of removed player |
| /topic/lobby/{id}/start | Game start. | `true` |
| /topic/lobby/{id}/end | Game in this lobby has ended (used to reset UI or redirect to lobby page) | `true` |
| /topic/lobby/{id}/readyError | All players are ready but not able to start game | `reason` <- string with problem |
| /topic/lobby/{id}/playerStatus | Total and ready players in the lobby are updated | `totalPlayers: number, readyPlayers: number` <- Number of players/ ready players |
| /topic/lobby/{id}/customWords | Custom word is added to the lobby | `customWords: List<string>` <- list with all the custom words |
| /topic/lobby/{id}/close | Lobby is closed due to inactivity | `CLOSED` |
| /topic/lobby/{id}/theme | New theme is set | `theme` <- string with current theme |
| /topic/lobby/{id}/turnDuration | Turn duration for Timed mode is changed | `turnDuration` <- integer with current turn duration |
| /topic/lobby/{id}/language | New language is set | `language` <- string with current language |
| /topic/lobby/{id}/chat/global | A message is sent in the global chat | `sender: string, message: string` |
| /topic/lobby/{id}/chat/team/{color} | A message is sent in the team chat | `sender: string, message: string` |
| /topic/game/{id}/hint | Hint is given | `hint: string, wordsCount: int, teamId: long, guessesLeft: int` |
| /topic/game/{id}/guess | Guess is made | `teamColor: string, wordStr: string` <- Team Color isch color vo team wo als nöchsts dra isch mit guesse |
| /topic/game/{id}/gameCompleted | Game finished. | `Color of winning team` |
| /topic/game/{id}/board | Sends the updated gameboard after a guess. | `updatedboard: List <Card>, guessesLeft: number` <- list of all cards on the board, each containing a word, color and guessed status. |
| /topic/lobby/{id}/lostPlayers | Lobby open-for-lost-players setting changed | `true or false` <- Boolean flag indicating if the lobby is open for lost players |
