package CreationPatterns.Prototype.prob2.components;

public class Body {
    private String content;

    public Body(String content) {
        this.content = content;
    }

    public Body(Body original) {
        this.content = original.content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}

