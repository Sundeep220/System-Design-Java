package InterviewProblems.VendingMachine.models;

import InterviewProblems.VendingMachine.enums.ProductType;

public record Product(String id, ProductType type, String name, double price) {
}
