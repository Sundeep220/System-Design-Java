package CreationPatterns.Prototype.prob2.documents;

import CreationPatterns.Prototype.prob2.Document;
import CreationPatterns.Prototype.prob2.components.Body;
import CreationPatterns.Prototype.prob2.components.Footer;
import CreationPatterns.Prototype.prob2.components.Header;
import CreationPatterns.Prototype.prob2.components.MetaData;

public class ContractDocument implements Document {
    private Header header;
    private Body body;
    private Footer footer;
    private MetaData metaData;

    public ContractDocument(String title) {
        this.header = new Header(title, "2025-06-14");
        this.body = new Body("Standard employee contract...");
        this.footer = new Footer("HR Team", "Signed");
        this.metaData = new MetaData("System", "Legal");
    }

    // Deep Copy Constructor
    private ContractDocument(ContractDocument original) {
        this.header = new Header(original.header);
        this.body = new Body(original.body);
        this.footer = new Footer(original.footer);
        this.metaData = new MetaData(original.metaData);
    }

    @Override
    public Document clone() {
        return new ContractDocument(this);
    }

    @Override
    public void printContent() {
        System.out.println("=== Contract Document ===");
        System.out.println("Title: " + header.getTitle());
        System.out.println("Date: " + header.getDateCreated());
        System.out.println("Body: " + body.getContent());
        System.out.println("Author: " + footer.getAuthor());
        System.out.println("Signature: " + footer.getSignature());
        System.out.println("Created By: " + metaData.getCreatedBy());
        System.out.println("Department: " + metaData.getDepartment());
        System.out.println();
    }

    // Getters for customization
    public Header getHeader() { return header; }
    public Body getBody() { return body; }
    public Footer getFooter() { return footer; }
    public MetaData getMetaData() { return metaData; }
}
