# PP-Generator



PP-Generator is an [osu!](https://osu.ppy.sh/home) discord bot written in Java on IntelliJ as Maven project.

I honestly don't expect anyone to get this one running on their PC but feel free to browse through whatever I did :)

If you have any questions, ideas, suggestions, improvements, ..., feel free to either open an issue or just message me (`Badewanne3#0685`) on Discord directly. Also if you would like my instance of the bot on your server, just hit me up.


# Commands



I can't be bothered to update this file regularly with new commands but you can either take a look into `core/Main.java` to see which commands are added in the `addCommands` function or you can simply use the `<help` command which lists all commands.



# Filthy dependencies



If you are crazy enough to try and set it up yourself, here's some things to consider:

- To access the osu! API I'm using the [osu-api-Java-Client](https://github.com/osuWorks/osu-api-Java-Client) which needs to be installed locally first.

- Calculating the [performance points](https://osu.ppy.sh/help/wiki/Performance_Points) of a map/play is done via slight modification (to get outputs that are easier to parse) of [osu-tools](https://github.com/ppy/osu-tools)' command line function so this one must be installed aswell.

- Another minor struggle was the [log4j](https://logging.apache.org/log4j/2.x/) import. Its pom.xml dependency as of now is working though so it might be fine.

- The last and most annoying dependency for the code right now will be the local database setup I have. Since I'm not handing out access to that, you'll have to rewrite that interface a little to fit your needs.

- A lot of parts in the code require a file `/util/secrets.java` which I have as static class containing variables for discord token, osu api key, twitch client id, file paths, ... so you'll have to customize that one.

- Everything else should import automatically through Maven (I hope).



# Side notes



The bot is by no means written in a professional manner. Just a side project for me to get back into Java and give our discord server some more utility.

Since [iaace](https://www.iaace.gg/)'s amazing BoatBot was our previous go-to bot, the message format of PP-Generator is strongly based on BoatBot. I encourage you to check BoatBot out yourself if you don't know it yet :)