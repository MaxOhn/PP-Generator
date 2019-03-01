package main.java.commands;

public class cmdBrainpower extends cmdSong {
    @Override
    String[] getLyrics() {
        return new String[] {
                "Andrenaline is pumping",
                "Andrenaline is pumpiiing",
                "Generator",
                "Automatic Lover",
                "Atomic",
                "Atomiiic",
                "Ooooverdrive",
                "Blockbuster",
                "Brain Power",
                "Call me a leader -- cocaine",
                "Don't you try it",
                "Don't you tryyy it",
                "Innovator",
                "Killer Machine",
                "There's no fate",
                "Take control",
                "Brain Poweeeer",
                "O-oooooooooo AAAAE-A-A-I-A-U- EO-"
        };
    }

    @Override
    int getDelay() {
        return 2500;
    }
}
