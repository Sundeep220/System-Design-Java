package BehaviouralPatterns.Command.prob1;

import java.util.Stack;

public class CommandManager {
    private Command command;
    private Stack<Command> history = new Stack<>();

    public void executeCommand(Command command) {
        this.command = command;
        this.command.execute();
        history.push(command);
    }

    public void undoCommand() {
        if (!history.isEmpty()) {
            Command lastCommand = history.pop();
            lastCommand.undo();
        } else {
            System.out.println("Nothing to undo.");
        }
    }
}
