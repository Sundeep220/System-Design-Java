package InterviewProblems.LibraryManagementSystem.models;

import java.util.HashMap;
import java.util.Map;

public class User {

    private final int userId;
    private final String name;

    // key = bookId
    // value = BorrowRecord
    private final Map<Integer, BorrowRecord> borrowedBooks;

    public User(int userId, String name) {
        this.userId = userId;
        this.name = name;
        this.borrowedBooks = new HashMap<>();
    }

    public boolean hasBorrowed(int bookId) {
        return borrowedBooks.containsKey(bookId);
    }

    public void borrowBook(BorrowRecord record) {
        borrowedBooks.put(record.getBook().getBookId(), record);
    }

    public BorrowRecord returnBook(int bookId) {
        return borrowedBooks.remove(bookId);
    }

    public BorrowRecord getBorrowRecord(int bookId) {
        return borrowedBooks.get(bookId);
    }

    public int getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public Map<Integer, BorrowRecord> getBorrowedBooks() {
        return Map.copyOf(borrowedBooks);
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", name='" + name + '\'' +
                ", borrowedBooks=" + borrowedBooks.keySet() +
                '}';
    }
}