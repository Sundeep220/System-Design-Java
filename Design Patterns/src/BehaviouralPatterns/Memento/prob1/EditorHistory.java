package BehaviouralPatterns.Memento.prob1;

import java.util.Stack;

public class EditorHistory {
    private final Stack<EditorState> undoStack = new Stack<>();
    private final Stack<EditorState> redoStack = new Stack<>();
    private final RichTextEditor editor;

    public EditorHistory(RichTextEditor editor) {
        this.editor = editor;
    }

    public void save() {
        undoStack.push(editor.save());
        redoStack.clear(); // Clear redo on new edit
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            EditorState prevState = undoStack.pop(); // 1. Go one step back
            redoStack.push(editor.save()); // save current before undo
            editor.restore(prevState);  // 3. Apply previous state
        } else {
            System.out.println("Nothing to undo.");
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            EditorState nextState = redoStack.pop(); // 1. Get next redo step
            undoStack.push(editor.save()); // save current before redo
            editor.restore(nextState); // 3. Apply redo state
        } else {
            System.out.println("Nothing to redo.");
        }
    }
}
