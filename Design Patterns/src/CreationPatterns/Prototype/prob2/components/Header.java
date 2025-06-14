package CreationPatterns.Prototype.prob2.components;

import java.util.Date;

public class Header {
    private String title;
    private String dateCreated;

    public Header(String title, String dateCreated) {
        this.title = title;
        this.dateCreated = dateCreated;
    }

    public Header(Header original) {
        this.title = original.title;
        this.dateCreated = original.dateCreated;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(String dateCreated) {
        this.dateCreated = dateCreated;
    }
}
