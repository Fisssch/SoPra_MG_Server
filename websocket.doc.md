# Websockets

| Path | Description | Data |
| ---- | ----------- | ---- |
| /topic/lobby/{id}/players | Player changes status or role or team. Id und data im body. | `ready: bool, color: string, role: string, playerId: long` <- Updated data of player |
| /topic/lobby/{id}/gameMode | Game mody of lobby is changed. | `gameMode: string` <- new game mode |
| /topic/lobby/{id}/addPlayer | Player is added to lobby. | `ready: bool, color: string, role: string, playerId: long` <- Data of added player |
| /topic/lobby/{id}/removePlayer | Player is removed from lobby. | `id: long` <- ID of removed player |
| /topic/lobby/{id}/start | Game start. | `true` |
| /topic/game/{id}/hint | Hint is given | `hint: string, wordsCount: int, teamId: long` |
| /topic/game/{id}/guess | Guess is made | `teamColor: string, wordStr: string` <- Team Color isch color vo team wo als nöchsts dra isch mit guesse |
| /topic/game/{id}/gameCompleted | Game finished. | `Color of winning team` |
