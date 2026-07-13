package InterviewProblems.LibraryManagementSystem;

import InterviewProblems.LibraryManagementSystem.models.Book;
import InterviewProblems.LibraryManagementSystem.models.BorrowRecord;
import InterviewProblems.LibraryManagementSystem.models.User;
import InterviewProblems.LibraryManagementSystem.service.FineCalculator;

import java.util.HashMap;
import java.util.Map;

public class Library {

    private final Map<Integer, Book> books;
    private final Map<Integer, User> users;

    private int nextBookId;

    public Library() {
        books = new HashMap<>();
        users = new HashMap<>();
        nextBookId = 1;
    }

    public int addBook(String title, String author, int copies) {
        Book book = new Book(nextBookId, title, author, copies);
        books.put(nextBookId, book);
        return nextBookId++;
    }

    public void registerUser(int userId, String name) {
        if(users.containsKey(userId)){
            throw new IllegalArgumentException("User already present");
        }
        users.put(userId, new User(userId, name));
    }

    public void unregisterUser(int userId) {
        User user = users.get(userId);

        if (user == null) {
            throw new IllegalArgumentException("User not found.");
        }

        if (!user.getBorrowedBooks().isEmpty()) {
            throw new IllegalStateException(
                    "User must return all books before unregistering."
            );
        }

        users.remove(userId);

    }

    public void requestBorrow(int userId, int bookId, int day) {
        User user = users.get(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found.");
        }
        Book book = books.get(bookId);
        if (book == null) {
            throw new IllegalArgumentException("Book not found.");
        }
        if (user.hasBorrowed(bookId)) {
            throw new IllegalStateException(
                    "User already borrowed this book."
            );
        }
        if (book.isAvailable()) {
            book.borrowCopy();
            BorrowRecord record = new BorrowRecord(book, day);
            user.borrowBook(record);
            System.out.println(user.getName() + " borrowed \"" + book.getTitle() + "\"");
        } else {
            book.addToWaitlist(user);
            System.out.println("No copies available. " + user.getName() + " added to waitlist.");
        }
    }

    public void returnBook(int userId, int bookId, int day) {
        User user = users.get(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found.");
        }
        Book book = books.get(bookId);
        if (book == null) {
            throw new IllegalArgumentException("Book not found.");
        }
        if (!user.hasBorrowed(bookId)) {
            throw new IllegalStateException(
                    "User has not borrowed this book."
            );
        }
        BorrowRecord record = user.returnBook(bookId);
        int fine = FineCalculator.calculateFine(
                record.getDueDay(),
                day
        );
        book.returnCopy();
        System.out.println(user.getName() + " returned \"" + book.getTitle() + "\"");

        if (fine > 0) {
            System.out.println(
                    "Fine: ₹" + fine
            );
        }

        if (book.hasWaitingUsers()) {
            User nextUser = book.getNextWaitingUser();
            book.borrowCopy();
            BorrowRecord nextRecord = new BorrowRecord(book, day);
            nextUser.borrowBook(nextRecord);
            System.out.println(book.getTitle() + " automatically issued to " + nextUser.getName());
        }
    }
}