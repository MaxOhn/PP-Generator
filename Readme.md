# PP-Generator



PP-Generator is an [osu!](https://osu.ppy.sh/home) [discord](https://discordapp.com/) bot written in Java on IntelliJ as Maven project.

The main idea of this is to show how the bot is working concretely, not necessarily to run it yourself. Nonetheless, feel free to get it going :)

If you have any questions, ideas, suggestions, improvements, ..., feel free to either open an issue or just message me (`Badewanne3#0685`) on Discord directly. Also if you would like my instance of the bot on your server, just hit me up.


# Commands



I can't be bothered to update this file regularly with new commands but you can either take a look into `core/Main.java` to see which commands are added in the `addCommands` function or you can simply use the `<help` command which lists all commands.
I might create a spreadsheet that contains all the commands in the future, I'll see.



# Get it started yourself


There are a few things to take care of so here's how you should proceed:

- Java, Maven, and .NET must be installed so the commands `javac`, `mvn`, and `dotnet` can be used in the command window.

- Clone this repository via `git clone https://github.com/MaxOhn/PP-Generator.git`

- Move through your command window into `PPGenerator/src/main/resources/`. There you will find two `.jar` files which you need to add to your local Maven repository via:

    - `mvn install:install-file -Dfile=osu4j-2.1.jar -DgroupId=com.oopsjpeg -DartifactId=osu4j -Dversion=2.1 -Dpackaging=jar -DgeneratePom=true`
    - `mvn install:install-file -Dfile=twitch-api-wrapper-0.3-jar-with-dependencies.jar -DgroupId=com.mb3364 -DartifactId=twitch-api-wrapper -Dversion=0.3 -Dpackaging=jar -DgeneratePom=true`

- The file `/util/secrets.java` which contains variables such as discord token, osu api key, twitch client id, file paths, ... is naturally ommitted for this repository. Instead, there is a file `/util/secretsButPublic.java` that demonstrates which variables are required so you have to add them yourself.

- The variable `WITH_DB` in `secrets.java` should stay false until you've setup your own database containing discord-osu links, beatmap info, tracked twitch streams, ...

- Calculating the [performance points](https://osu.ppy.sh/help/wiki/Performance_Points) of a map/play is done via slight modification (to get outputs that are easier to parse) of [osu-tools](https://github.com/ppy/osu-tools)' command line function which is included in the resources directory. Unfortunately, this tool currently does not provide CtB pp calculation, and also its mania calculation is slightly off.



# Side notes



I tried to keep everything clean and elegant, even though I horribly failed at that at many points as you might be about to see :p 

The whole thing is just a side project for me to get back into Java and give our discord server some more utility.

Since [iaace](https://www.iaace.gg/)'s amazing BoatBot was our previous go-to bot, the message format of PP-Generator is strongly based on BoatBot. I encourage you to check BoatBot out yourself if you don't know it yet :)