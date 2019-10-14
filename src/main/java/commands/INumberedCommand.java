package main.java.commands;

/*
    Some commands allow a number directly after the command invoke i.e. <recent7
 */
public interface INumberedCommand extends ICommand {

    INumberedCommand setNumber(int number);

}
