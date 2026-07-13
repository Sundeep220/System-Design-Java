package InterviewProblems.LibraryManagementSystem.models;

public class BorrowRecord {

    private static final int BORROW_DURATION = 14;

    private final Book book;
    private final int borrowDay;
    private final int dueDay;

    public BorrowRecord(Book book, int borrowDay) {
        this.book = book;
        this.borrowDay = borrowDay;
        this.dueDay = borrowDay + BORROW_DURATION;
    }

    public Book getBook() {
        return book;
    }

    public int getBorrowDay() {
        return borrowDay;
    }

    public int getDueDay() {
        return dueDay;
    }

    @Override
    public String toString() {
        return "BorrowRecord{" +
                "book=" + book.getTitle() +
                ", borrowDay=" + borrowDay +
                ", dueDay=" + dueDay +
                '}';
    }
}
