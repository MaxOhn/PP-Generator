package main.java.commands;

public class cmdDing extends cmdSong {
    @Override
    String[] getLyrics() {
        return new String[] {
                "Oh-oh-oh, hübsches Ding",
                "Ich versteck' mein' Ehering",
                "Klinglingeling, wir könnten's bring'n",
                "Doch wir nuckeln nur am Drink",
                "Oh-oh-oh, hübsches Ding",
                "Du bist Queen und ich bin King",
                "Wenn ich dich seh', dann muss ich sing'n:",
                "Tingalingaling, you pretty thing!"
        };
    }

    @Override
    int getDelay() {
        return 3000;
    }
}
