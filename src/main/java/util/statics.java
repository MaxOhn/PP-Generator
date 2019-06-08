package main.java.util;

public class statics {

    public static final String prefix = "<";

    public static final String prefixAlt = "!!";

    public static final String[] authorities = {"admin", "mod", "moderator", "botcommander", "bot commander"};

    public static final String raspResources = "./resources/";

    public static final String log4jPath = secrets.RELEASE
            ? raspResources + "log4j.properties"
            : ".\\src\\main\\resources\\log4j.properties";

    public static final String thumbPath = secrets.RELEASE
            ? raspResources + "mapThumb/"
            : ".\\src\\main\\resources\\mapThumb\\";

    public static final String gameName = secrets.RELEASE
            ? ("osu! slave (" + prefix + "help)")
            : "in developement";

    public static final String perfCalcPath = secrets.RELEASE
            ? "PerformanceCalculator/PerformanceCalculator.dll"
            : "./src/main/resources/PerformanceCalculator/PerformanceCalculator.dll";

    public static final String execPrefix = secrets.RELEASE ? "" : "cmd /c ";
}
