package main.java.commands.Fun;

import main.java.commands.Command;
import main.java.util.statics;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class cmdDance implements Command {
    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        if (args.length == 0)
            return true;
        if (args[0].equals("-h") || args[0].equals("-help"))
            event.getTextChannel().sendMessage(help(0)).queue();
        return false;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        /*
        // Legacy text output
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
        //*/
        EmbedBuilder eb = new EmbedBuilder()
                .setImage("https://cdn.discordapp.com/attachments/475447298370043935/530087223278698496/ezgif.com-gif-maker_1.gif");
        event.getTextChannel().sendMessage(eb.build()).queue();
    }

    @Override
    public String help(int hCode) {
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "dance` to make me show off my moves :^)";
            default:
                return help(0);
        }
    }
}
