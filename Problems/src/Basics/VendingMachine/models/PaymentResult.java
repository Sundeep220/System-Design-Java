package Basics.VendingMachine.models;

public class PaymentResult {
    private final boolean success;
    private final String paymentId;
    private final String failureReason;

    public PaymentResult(boolean success, String paymentId, String failureReason) {
        this.success = success;
        this.paymentId = paymentId;
        this.failureReason = failureReason;
    }

    public boolean isSuccess() { return success; }
    public String getPaymentId() { return paymentId; }
    public String getFailureReason() { return failureReason; }
}