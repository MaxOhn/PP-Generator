package main.java.commands.Fun;

import main.java.util.statics;

public class cmdBackgroundGameMania extends cmdBackgroundGame {

    @Override
    public String getSourcePath() {
        return statics.getBgGamePathMania;
    }

    @Override
    public String getName() {
        return "bgm";
    }
}
