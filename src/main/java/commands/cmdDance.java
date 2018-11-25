package main.java.commands;

import main.java.util.statics;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class cmdDance implements Command {
    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        if (args.length == 0)
            return true;
        if (args[0].equals("--h") || args[0].equals("--help"))
            event.getTextChannel().sendMessage(help(0)).queue();
        return false;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        List<String> sLeft = new ArrayList<>(Arrays.asList("＼(^ω^＼)", "ヘ(^_^ヘ)", "ヾ(ﾟ∀ﾟゞ)", "ヘ(￣ー￣ヘ)", "\\\\(^.^\\\\)"));
        List<String> sRight = new ArrayList<>(Arrays.asList("(ﾉﾟ▽ﾟ)ﾉ", "〈( ^.^)ノ", "(/^.^)/", "(ノ^o^)ノ", "(｢･ω･)｢"));
        final int[] counter = {0};
        final String[] text = {"(/^.^)/"};
        event.getTextChannel().sendMessage(text[0]).queue(message -> {
            final Thread t = new Thread(() -> {
                while(!Thread.currentThread().isInterrupted()){
                    try {
                        Thread.sleep(2000);
                        text[0] = sLeft.contains(text[0]) ? sRight.get(counter[0]) : sLeft.get(counter[0]--);
                        counter[0] = (counter[0] + 1) % 5;
                        message.editMessage(text[0]).queue();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        message.editMessage("Oof that was exhausting xd").queue();
                    }
                }
            });
            t.start();
            ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
            exec.schedule(t::interrupt, 30, TimeUnit.SECONDS);
        });
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "dance --h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "dance` to make me show off my moves for the next 30 seconds";
            default:
                return help(0);
        }
    }
}
