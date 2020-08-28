# PocketLint [<img src="https://api.travis-ci.org/RealGatt/PocketLint.svg" alt="Travis Passing Image">](https://travis-ci.org/github/RealGatt/PocketLint)
A bot written for the Official BackPocket Discord

## Requirements
- Java
- Maven
- MongoDB server
- Twitch Application

## Setup
1. Clone this repo
2. Build project w/ `mvn clean install`. Move JAR to where you want to run it.
3. Create a folder named "data" in the folder where the moved JAR is located.
4. In "data" folder, create the following files: `BotConfiguration.json` `MongoConfiguration.json` `TwitchConfiguration.json`

`BotConfiguration.json`
```json
{
  "botToken": "DISCORD BOT TOKEN HERE",
  "devMode": false,
  "botPrefix": "_"
}
```

`MongoConfiguration.json`
```json
{
  "mongoIP": "MONGO IP",
  "mongoUsername": "MONGO USERNAME",
  "mongoPassword": "MONGO PASSWORD",
  "mongoPort": 8726
}
```

`TwitchConfiguration.json`
```json
{
  "TwitchOAuthToken": "TWITCH OATUH TOKEN FOR THE TWITCH BOT ACCOUNT"
}
```

5. Run with `java -jar ...`
