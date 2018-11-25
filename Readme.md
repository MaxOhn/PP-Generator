# PP-Generator




Small [osu!](https://osu.ppy.sh/home) discord bot written in Java on IntelliJ as Maven project.

I honestly don't expect anyone to get this one running on their PC but feel free to browse through whatever I did :)



# Filthy dependencies




The struggle setting it up begins here.

- To access the osu! API I'm using the [osu-api-Java-Client](https://github.com/osuWorks/osu-api-Java-Client) which needs to be installed locally first.

- Calculating the [performance points](https://osu.ppy.sh/help/wiki/Performance_Points) of a map/play is done via [oppai-ng](https://github.com/Francesco149/oppai-ng)'s command line function so this one must be installed aswell.

- Another minor struggle was the log4j import, its pom.xml dependency as of now is working though so it might be fine.

- The last and most annoying dependency for the code right now will be the local database setup I have. Since I'm not handing out access to that, you'll have to rewrite that interface a little to fit your needs.

- Everything else is should import automatically through Maven (I hope).



# Side notes




If any question arise, feel free to contact me (Badewanne3#0685) on discord.

The bot is by no means written in a professional manner. Just a little project for me to get back into Java and give our discord server some more utility.

Since [iaace](https://www.iaace.gg/)'s amazing BoatBot was our previous go-to bot, the message format of PP-Generator is strongly based on BoatBot. I encourage you to check BoatBot out yourself if you don't know it yet :)