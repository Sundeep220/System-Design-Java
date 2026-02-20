package Basics.VendingMachine.models;

import Basics.VendingMachine.enums.TransactionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class Transaction {
    private final String transactionId;
    private final List<CartItem> items;
    private final double totalAmount;
    private TransactionStatus status;
    private final LocalDateTime timestamp;

    public Transaction(List<CartItem> items, double totalAmount) {
        this.transactionId = UUID.randomUUID().toString();
        this.items = items;
        this.totalAmount = totalAmount;
        this.status = TransactionStatus.INITIATED;
        this.timestamp = LocalDateTime.now();
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public TransactionStatus getStatus() { return status; }
    public String getTransactionId() { return transactionId; }
    public List<CartItem> getItems() { return items; }
    public double getTotalAmount() { return totalAmount; }
}