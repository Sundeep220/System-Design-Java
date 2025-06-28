package BehaviouralPatterns.Command.prob1;

public class DeleteTextCommand implements Command {
    private final Editor editor;
    private final String text;
    public DeleteTextCommand(Editor editor, String text) {
        this.editor = editor;
        this.text = text;
    }
    public void execute() {
        editor.removeText(text);
    }

    public void undo() {
        editor.appendText(text);
    }
}
