# Local WiFi Multiplayer

This mode lets players on the same LAN enter one Monopoly Deal table from the lobby.

## Demo Flow

1. Start the game and choose **Local WiFi Game**.
2. On the host computer, enter a player name and click **Host Room**.
3. Share the host IP shown in the lobby with the other players.
4. On each guest computer, enter a name, the host IP, and the same port, then click **Join Room**.
5. Every player clicks **Ready**.
6. The host clicks **Start Network Game**.
7. All connected players enter the game table with the same player order and shared deck seed.

## Current Behavior

- The room supports chat, ready state, reconnect, and host-only game start.
- When the game starts, every client creates the same initial table using the shared deck seed.
- Player actions are broadcast to the other clients and replayed locally.
- Only the local player's own turn is interactive in the network table.
- Network payment and counter-action prompts are handled automatically so clients do not desync on modal choices.

## Notes

- Use the same project build on every machine.
- All devices must be on the same local network and allowed through the firewall.
- If a client falls out of sync, return to the lobby and start a fresh room.
