package BehaviouralPatterns.Memento.prob1;

public class Main {
    public static void main(String[] args) {
        RichTextEditor editor = new RichTextEditor();
        EditorHistory history = new EditorHistory(editor);

        editor.type("Hello");
        editor.setFontSize(12);
        editor.setFontStyle("Arial");
        history.save(); // Save 1

        editor.type("Hello World");
        editor.setFontSize(14);
        editor.setFontStyle("Arial");
        history.save(); // Save 2

        editor.type("Hello World!!");
        editor.setFontStyle("Courier New");
        System.out.println("Current: " + editor);

        history.undo();
        System.out.println("After Undo: " + editor);

        history.undo();
        System.out.println("After 2nd Undo: " + editor);

        history.redo();
        System.out.println("After Redo: " + editor);

        history.redo();
        System.out.println("After 2nd Redo: " + editor);
    }
}
