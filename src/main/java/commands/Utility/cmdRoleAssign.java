package main.java.commands.Utility;

import main.java.commands.PrivilegedCommand;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class cmdRoleAssign extends PrivilegedCommand {

    @Override
    public void action(String[] args, MessageReceivedEvent event) {

    }

    @Override
    public String help(int hCode) {
        return null;
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.UTILITY;
    }
}
