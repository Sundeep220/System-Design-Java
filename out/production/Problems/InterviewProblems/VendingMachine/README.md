| Requirement               | Suggested Design Change                                                                                               |
| ------------------------- | --------------------------------------------------------------------------------------------------------------------- |
| Multiple payment methods  | Add new `PaymentStrategy` implementation without changing existing code (Open/Closed Principle).                      |
| Discount coupons          | Introduce a `PricingStrategy` or `CouponStrategy` before payment calculation.                                         |
| Multiple vending machines | Create a `VendingMachineManager` to manage several machine instances.                                                 |
| Admin refill              | Add an `AdminService` responsible for inventory and cash replenishment.                                               |
| Sales reports             | Introduce a `TransactionRepository` and persist completed transactions.                                               |
| Concurrent users          | Synchronize critical operations or use locks around inventory and transaction updates.                                |
| Dynamic pricing           | Add a `PricingService` to compute prices instead of storing fixed values in `Product`.                                |
| Product recommendations   | Add a `RecommendationService` without modifying the core vending machine logic.                                       |
| Cashless-only machine     | Replace `CashPayment` with `UPIPayment`/`CardPayment`; the machine remains unchanged because of the Strategy pattern. |
