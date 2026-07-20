package InterviewProblems.MeetingRooms;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) {

        RoomBooking bookingSystem = new RoomBooking(Arrays.asList("C", "A", "B"));

        // A is lexicographically smallest, so it should be selected.
        System.out.println(
                bookingSystem.bookMeeting("M1", 10, 12)
        ); // A

        // A is occupied, so B is selected.
        System.out.println(
                bookingSystem.bookMeeting("M2", 11, 15)
        ); // B

        // A and B are occupied, so C is selected.
        System.out.println(
                bookingSystem.bookMeeting("M3", 11, 15)
        ); // C

        // All rooms are occupied during this time.
        System.out.println(
                bookingSystem.bookMeeting("M4", 11, 12)
        ); // ""

        // Cancel M1 from room A.
        System.out.println(
                bookingSystem.cancelMeeting("M1")
        ); // true

        // A is now free, so it is selected again.
        System.out.println(
                bookingSystem.bookMeeting("M4", 11, 12)
        ); // A

        // Meeting does not exist.
        System.out.println(
                bookingSystem.cancelMeeting("M100")
        ); // false

        // Meeting IDs must be unique while active.
        System.out.println(
                bookingSystem.bookMeeting("M4", 20, 25)
        ); // ""
    }
}
