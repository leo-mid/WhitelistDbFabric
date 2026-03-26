# WhitelistDb Mod

## Description
This mod is used to make it easier to have the same whitelist across different servers and to use different methods for allowing users to whitelist.
For example, having people in a school Minecraft server, and in order for them to join, they have to verify their school email to gain access to a whitelist command from a Discord bot. (Please note that this does not provide a Discord bot.)

## Commands
* /whitelistdb toggle
    * Turns the whitelist on and off
* /wban
    * Updates the database record to show they are banned across **all servers!**
    * Will kick the player if they are online - does work on players who aren't online
* /wunban
    * Updates the database record to show they are unbanned across **all servers!**

## Getting Started
To use this mod correctly, the table needs to at least have this information in it.
```
table name = server_whitelists
uuid : UUID
banned : Boolean
```

This mod also caches usernames and UUIDs locally to ban/unban offline players much more easily.
If this is being added to a server that is already up, you can use the Python script provided in `scripts/convert.py` to correctly take the `usercache.json` file in the root directory of the server and make a file that should be placed in `config/whitelistdb`. This isn't completely necessary, as the cache will auto-update when new people join, but this will make sure that everyone who has ever joined will be detected by the mod.

When the mod is in the mod folder, start the server, and it will make a file located at `config/whitelistdb-config.json`. This is where you can edit the default values to correctly set up the database and edit the messages that come up when not whitelisted and when users are banned.

Default layout of `config/whitelistdb-config.json`
```
{
  "host": "localhost",
  "port": 5432,
  "database": "minecraft",
  "username": "postgres",
  "password": "password",
  "ssl": false,
  "message": "You are not whitelisted!",
  "enabled": true,
  "banReason": "You have been banned!"
}
```

## Mod Information
Current version: 1.3.3

Versions Supported: 26.1

