// The result of the last command run in a playground is shown on the results panel.
// By default the first 20 documents will be returned with a cursor.
// Use 'console.log()' to print to the debug output.
// For more documentation on playgrounds please refer to
// https://www.mongodb.com/docs/mongodb-vscode/playgrounds/

// Database to use.
use("SoPraFS25");

console.log("Teams:");
db.getCollection("TEAM")
    .find({})
    .forEach((team) => {
        console.log(team);
    });
console.log("----------------------");

console.log("Players:");
db.getCollection("PLAYER")
    .find({})
    .forEach((player) => {
        console.log(player);
    });
console.log("----------------------");

console.log("Lobbies");
db.getCollection("LOBBY")
    .find({})
    .forEach((lobby) => {
        console.log(lobby);
    });
console.log("----------------------");

console.log("Games");
db.getCollection("GAME")
    .find({})
    .forEach((lobby) => {
        console.log(lobby);
    });
console.log("----------------------");

console.log("Users:");
db.getCollection("USER")
    .find({})
    .forEach((user) => {
        console.log(user);
    });
console.log("----------------------");
