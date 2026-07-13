package InterviewProblems.LibraryManagementSystem.models;

import java.util.ArrayDeque;
import java.util.Queue;

public class Book {

    private final int bookId;
    private final String title;
    private final String author;

    private final int totalCopies;
    private int availableCopies;

    // FIFO reservation queue
    private final Queue<User> waitlist;

    public Book(int bookId, String title, String author, int copies) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;

        this.totalCopies = copies;
        this.availableCopies = copies;

        this.waitlist = new ArrayDeque<>();
    }

    public boolean isAvailable() {
        return availableCopies > 0;
    }

    public void borrowCopy() {
        if (availableCopies == 0) {
            throw new IllegalStateException("No copies available.");
        }
        availableCopies--;
    }

    public void returnCopy() {
        if (availableCopies == totalCopies) {
            throw new IllegalStateException("All copies are already in library.");
        }
        availableCopies++;
    }

    public void addToWaitlist(User user) {
        waitlist.offer(user);
    }

    public User getNextWaitingUser() {
        return waitlist.poll();
    }

    public boolean hasWaitingUsers() {
        return !waitlist.isEmpty();
    }

    // ---------------- Getters ----------------

    public int getBookId() {
        return bookId;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    public int getAvailableCopies() {
        return availableCopies;
    }

    @Override
    public String toString() {
        return "Book{" +
                "bookId=" + bookId +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", availableCopies=" + availableCopies +
                "/" + totalCopies +
                '}';
    }
}