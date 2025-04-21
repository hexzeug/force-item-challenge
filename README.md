# Force Item Challenge
Vanilla survival challenge to collect as many items as possible
in a randomly determined given order.

## Installation
The mod is only required on the server.
It can be installed on the client to play in Singleplayer or LAN-Worlds.

## Playing
Use `/timer` to configure and start the timer (Only operators can configure it).
As soon as the game starts every player is prompted an item (everyone a different random one).
Every collected _item is worth one point_.
The player with the most points wins.

### Skipping
Players can skip their item, if they figure getting it would take too much effort using `/skip`.
For each skip the final score is _decreased by one_.

If you are prompted an operator or otherwise unobtainable item
an operator can skip your challenge using `/skip <playername>` without the penalty point.

### Results
An operator can use `/results` repeatedly
to show the score and items of the players
in order of their placement (starting with last place).
To reopen a players results use `/results <playername>`.

### Had fun and want a rematch?
In Singleplayer just create a new world.

On a multiplayer server an operator can use `/new` to create a new world.
The command deletes the old world and stops the server.
Restarting it must unfortunately be done manually.


## Command aliases
- `/play` for `/timer resume`
- `/pause` for `/timer pause`