package InterviewProblems.VendingMachine.state;


import InterviewProblems.VendingMachine.enums.Denomination;
import InterviewProblems.VendingMachine.models.VendingMachine;

public interface State {

    void insertMoney(VendingMachine machine, Denomination denomination);

    void selectProduct(VendingMachine machine, String slotId);

    void dispense(VendingMachine machine);

    void cancel(VendingMachine machine);

    void complete(VendingMachine machine);
}