package BehaviouralPatterns.Command.prob1;

public class Editor {
    private StringBuilder content = new StringBuilder();

    public void appendText(String words) {
        content.append(words);
    }

    public void removeText(String words) {
        content.delete(content.indexOf(words), content.indexOf(words) + words.length());
    }

    public String getText() {
        return content.toString();
    }
}
