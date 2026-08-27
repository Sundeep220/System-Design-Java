package com.flowforge.flowforge.config;

import com.flowforge.flowforge.entity.Workflow;
import com.flowforge.flowforge.entity.WorkflowStep;
import com.flowforge.flowforge.repository.WorkflowRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!prod")
public class DataSeeder {

    @Bean
    CommandLineRunner seedData(WorkflowRepository workflowRepository) {
        return args -> {

            // --- Workflow 1: with steps ---
            Workflow payment = new Workflow("Payment Processing", "Handles payment transactions end-to-end", 3, 120);
            payment.addStep(new WorkflowStep(payment, "Validate Card", "VALIDATION", 1, "{\"provider\": \"stripe\"}"));
            payment.addStep(new WorkflowStep(payment, "Charge Amount", "HTTP_CALL", 2, "{\"endpoint\": \"/charge\"}"));
            payment.addStep(new WorkflowStep(payment, "Send Receipt", "EMAIL", 3, "{\"template\": \"receipt\"}"));
            workflowRepository.save(payment);

            // Small delay so createdAt differs between workflows
            Thread.sleep(50);

            // --- Workflow 2: with steps ---
            Workflow orderFlow = new Workflow("Order Fulfillment", "Manages order lifecycle from placement to delivery", 5, 300);
            orderFlow.addStep(new WorkflowStep(orderFlow, "Reserve Inventory", "HTTP_CALL", 1, "{\"service\": \"inventory\"}"));
            orderFlow.addStep(new WorkflowStep(orderFlow, "Process Payment", "SUBPROCESS", 2, "{\"workflowRef\": \"payment\"}"));
            orderFlow.addStep(new WorkflowStep(orderFlow, "Ship Order", "HTTP_CALL", 3, "{\"carrier\": \"fedex\"}"));
            orderFlow.addStep(new WorkflowStep(orderFlow, "Notify Customer", "EMAIL", 4, "{\"template\": \"shipped\"}"));
            workflowRepository.save(orderFlow);

            Thread.sleep(50);

            // --- Workflow 3: no steps ---
            Workflow notification = new Workflow("Notification Pipeline", "Routes notifications to appropriate channels", 2, 60);
            workflowRepository.save(notification);

            Thread.sleep(50);

            // --- Workflow 4 ---
            Workflow refund = new Workflow("Refund Processing", "Handles refund requests and reversals", 3, 180);
            refund.addStep(new WorkflowStep(refund, "Validate Refund", "VALIDATION", 1, "{\"maxAmount\": 5000}"));
            refund.addStep(new WorkflowStep(refund, "Reverse Charge", "HTTP_CALL", 2, "{\"provider\": \"stripe\"}"));
            workflowRepository.save(refund);

            Thread.sleep(50);

            // --- Workflow 5 ---
            Workflow userOnboarding = new Workflow("User Onboarding", "Guides new users through setup", 1, 90);
            userOnboarding.addStep(new WorkflowStep(userOnboarding, "Create Account", "HTTP_CALL", 1, null));
            userOnboarding.addStep(new WorkflowStep(userOnboarding, "Send Welcome Email", "EMAIL", 2, "{\"template\": \"welcome\"}"));
            userOnboarding.addStep(new WorkflowStep(userOnboarding, "Assign Default Role", "SCRIPT", 3, "{\"role\": \"USER\"}"));
            workflowRepository.save(userOnboarding);

            Thread.sleep(50);

            // --- Workflow 6 ---
            Workflow alerting = new Workflow("Alert Monitor", "Monitors system health and triggers alerts", 10, 30);
            workflowRepository.save(alerting);

            Thread.sleep(50);

            // --- Workflow 7 ---
            Workflow dataExport = new Workflow("Data Export Pipeline", null, 2, 600);
            dataExport.addStep(new WorkflowStep(dataExport, "Query Data", "SQL", 1, "{\"query\": \"SELECT * FROM reports\"}"));
            dataExport.addStep(new WorkflowStep(dataExport, "Generate CSV", "TRANSFORM", 2, null));
            dataExport.addStep(new WorkflowStep(dataExport, "Upload to S3", "HTTP_CALL", 3, "{\"bucket\": \"exports\"}"));
            workflowRepository.save(dataExport);

            Thread.sleep(50);

            // --- Workflow 8 ---
            Workflow cleanup = new Workflow("Cleanup Job", "Removes stale records older than 90 days", 1, 3600);
            workflowRepository.save(cleanup);

            Thread.sleep(50);

            // --- Workflow 9 ---
            Workflow reportGen = new Workflow("Report Generator", "Generates weekly analytics reports", 3, 900);
            reportGen.addStep(new WorkflowStep(reportGen, "Aggregate Metrics", "SQL", 1, null));
            reportGen.addStep(new WorkflowStep(reportGen, "Build Charts", "SCRIPT", 2, null));
            workflowRepository.save(reportGen);

            Thread.sleep(50);

            // --- Workflow 10 ---
            Workflow auditLog = new Workflow("Audit Logger", "Tracks all system changes for compliance", 5, 45);
            auditLog.addStep(new WorkflowStep(auditLog, "Capture Event", "LISTENER", 1, null));
            auditLog.addStep(new WorkflowStep(auditLog, "Enrich Context", "TRANSFORM", 2, null));
            auditLog.addStep(new WorkflowStep(auditLog, "Store Record", "HTTP_CALL", 3, "{\"target\": \"audit-db\"}"));
            workflowRepository.save(auditLog);

            System.out.println("=== Seed data loaded: 10 workflows ===");
        };
    }
}
