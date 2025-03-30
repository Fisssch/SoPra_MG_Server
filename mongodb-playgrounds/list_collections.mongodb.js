// The result of the last command run in a playground is shown on the results panel.
// By default the first 20 documents will be returned with a cursor.
// Use 'console.log()' to print to the debug output.
// For more documentation on playgrounds please refer to
// https://www.mongodb.com/docs/mongodb-vscode/playgrounds/

// Database to use.
use("SoPraFS25");

console.log("Collections:");
db.getCollectionNames().forEach((collection) => {
    console.log(collection);
});
