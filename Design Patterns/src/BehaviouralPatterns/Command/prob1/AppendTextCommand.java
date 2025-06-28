package BehaviouralPatterns.Command.prob1;

public class AppendTextCommand implements Command {
    private Editor editor;
    private String text;
    public AppendTextCommand(Editor editor, String text) {
        this.editor = editor;
        this.text = text;
    }
    public void execute() {
        editor.appendText(text);
    }

    @Override
    public void undo() {
        editor.removeText(text);
    }
}
