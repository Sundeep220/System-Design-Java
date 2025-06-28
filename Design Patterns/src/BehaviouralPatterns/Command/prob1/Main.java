package BehaviouralPatterns.Command.prob1;

public class Main {
    public static void main(String[] args) {
        Editor editor = new Editor();
        CommandManager manager = new CommandManager();

        manager.executeCommand(new AppendTextCommand(editor, "Hello "));
        manager.executeCommand(new AppendTextCommand(editor, "World!"));
        System.out.println(editor.getText());
        manager.undoCommand();
        System.out.println(editor.getText());

        manager.executeCommand(new DeleteTextCommand(editor, "llo"));
        System.out.println(editor.getText());

        manager.undoCommand();
        System.out.println(editor.getText());
    }
}
