package CreationPatterns.Prototype.prob2.components;

public class Footer {
    private String author;
    private String signature;

    public Footer(String author, String signature) {
        this.author = author;
        this.signature = signature;
    }

    public Footer(Footer original) {
        this.author = original.author;
        this.signature = original.signature;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}

