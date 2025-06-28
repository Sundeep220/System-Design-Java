package BehaviouralPatterns.Memento.prob1;

public class EditorState {
    private final String content;
    private final int fontSize;
    private final String fontStyle;

    public EditorState(String content, int fontSize, String fontStyle) {
        this.content = content;
        this.fontSize = fontSize;
        this.fontStyle = fontStyle;
    }

    public String getContent() {
        return content;
    }

    public int getFontSize() {
        return fontSize;
    }

    public String getFontStyle() {
        return fontStyle;
    }
}
