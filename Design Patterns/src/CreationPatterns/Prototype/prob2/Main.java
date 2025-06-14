package CreationPatterns.Prototype.prob2;

import CreationPatterns.Prototype.prob2.documents.ContractDocument;
import CreationPatterns.Prototype.prob2.documents.NDADocument;
import CreationPatterns.Prototype.prob2.documents.ReportDocument;
import CreationPatterns.Prototype.prob2.registry.DocumentRegistry;

public class Main {
    public static void main(String[] args) {
        // Before Registry
//        / Base templates
//        ContractDocument baseContract = new ContractDocument("Employee Agreement");
//        NDADocument baseNDA = new NDADocument("Startup NDA");
//        ReportDocument baseReport = new ReportDocument("Q2 Financials");
//
//        // Clone & customize NDA
//        NDADocument ndaClone = (NDADocument) baseNDA.clone();
//        ndaClone.getHeader().setTitle("Vendor NDA");
//        ndaClone.getBody().setContent("NDA terms for third-party vendors.");
//        ndaClone.getFooter().setAuthor("Legal Advisor");
//
//        // Clone & customize Report
//        ReportDocument reportClone = (ReportDocument) baseReport.clone();
//        reportClone.getHeader().setTitle("Q3 Financial Report");
//        reportClone.getBody().setContent("Performance analysis for Q3.");
//        reportClone.getFooter().setAuthor("CFO");
//
//        // Print originals & clones
//        baseContract.printContent();
//        baseNDA.printContent();
//        baseReport.printContent();
//
//        ndaClone.printContent();
//        reportClone.printContent();


        DocumentRegistry registry = new DocumentRegistry();

        // Register base prototypes
        registry.register("contract", new ContractDocument("Employee Agreement"));
        registry.register("nda", new NDADocument("Startup NDA"));
        registry.register("report", new ReportDocument("Q2 Financials"));

        // Get clones
        ContractDocument contractClone = (ContractDocument) registry.getClone("contract");
        NDADocument ndaClone = (NDADocument) registry.getClone("nda");
        ReportDocument reportClone = (ReportDocument) registry.getClone("report");

        // Customize
        contractClone.getHeader().setTitle("Freelancer Agreement");
        contractClone.getBody().setContent("Agreement for freelance contractors.");

        ndaClone.getHeader().setTitle("Investor NDA");
        ndaClone.getBody().setContent("NDA terms for angel investors.");
        ndaClone.getFooter().setAuthor("Startup Legal");

        reportClone.getHeader().setTitle("Annual Report 2025");
        reportClone.getBody().setContent("Full-year performance and projections.");
        reportClone.getFooter().setAuthor("CEO Office");

        // Print results
        System.out.println("=== From Registry ===");
        contractClone.printContent();
        ndaClone.printContent();
        reportClone.printContent();
    }
}
