package BehaviouralPatterns.Memento.prob1;

public class RichTextEditor {
    private String content = "";
    private int fontSize = 12;
    private String fontStyle = "Arial";

    public void type(String words) {
        content = words;
    }

    public String getContent() {
        return content;
    }

    public void setFontSize(int size) {
        fontSize = size;
    }

    public void setFontStyle(String style) {
        fontStyle = style;
    }

    public EditorState save() {
        return new EditorState(content, fontSize, fontStyle);
    }

    public void restore(EditorState state) {
        content = state.getContent();
        fontSize = state.getFontSize();
        fontStyle = state.getFontStyle();
    }

    @Override
    public String toString() {
        return "Content: " + content + ", FontSize: " + fontSize + ", FontStyle: " + fontStyle;
    }

}
