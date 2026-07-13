package InterviewProblems.LibraryManagementSystem;

public class Main {

    public static void main(String[] args) {

        Library library = new Library();

        // ---------------- Register Users ----------------

        library.registerUser(1, "Alice");
        library.registerUser(2, "Bob");
        library.registerUser(3, "Charlie");

        // ---------------- Add Books ----------------

        int cleanCodeId = library.addBook(
                "Clean Code",
                "Robert C. Martin",
                2
        );

        int dddId = library.addBook(
                "Domain Driven Design",
                "Eric Evans",
                1
        );

        // ----------------------------------------------------
        // Borrow Clean Code
        // ----------------------------------------------------

        library.requestBorrow(1, cleanCodeId, 1);   // Alice
        library.requestBorrow(2, cleanCodeId, 2);   // Bob

        // No copies left
        library.requestBorrow(3, cleanCodeId, 3);   // Charlie -> Waitlist

        // ----------------------------------------------------
        // Borrow another book
        // ----------------------------------------------------

        library.requestBorrow(1, dddId, 4);

        // ----------------------------------------------------
        // Return with Fine
        // ----------------------------------------------------

        // Borrowed on day 1
        // Due = day 15
        // Returned on day 20
        // Fine = (20 - 15) * 20 = ₹100

        library.returnBook(1, cleanCodeId, 20);

        // Charlie should automatically receive the book.

        // ----------------------------------------------------
        // Return second book (No Fine)
        // ----------------------------------------------------

        library.returnBook(1, dddId, 10);

        // ----------------------------------------------------
        // Bob returns later
        // ----------------------------------------------------

        library.returnBook(2, cleanCodeId, 25);

        // ----------------------------------------------------
        // Charlie returns
        // ----------------------------------------------------

        library.returnBook(3, cleanCodeId, 30);

        // ----------------------------------------------------
        // Unregister user
        // ----------------------------------------------------

        library.unregisterUser(1);

        System.out.println();
        System.out.println("Library simulation completed.");
    }
}