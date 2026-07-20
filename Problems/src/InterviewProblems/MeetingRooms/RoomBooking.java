package InterviewProblems.MeetingRooms;

import java.util.*;

public class RoomBooking {
    // Sorted room IDs ensure the first available room is lexicographically smallest.
    private final List<String> roomIds;

    // Each room stores meetings ordered by their start time.
    private final Map<String, TreeMap<Integer, Meeting>> roomBookings;

    // Direct lookup makes cancellation O(log n) instead of scanning all meetings.
    private final Map<String, Meeting> meetings;

    public RoomBooking(List<String> roomIds) {
        this.roomIds = new ArrayList<>(roomIds);
        Collections.sort(this.roomIds);
        this.roomBookings = new HashMap<>();
        this.meetings = new HashMap<>();

        for (String roomId : this.roomIds) {
            roomBookings.put(roomId, new TreeMap<>());
        }
    }

    public String bookMeeting(String meetingId, Integer startTime, Integer endTime){
        if(meetings.containsKey(meetingId)){
//            throw new RuntimeException("Meeting already exists.");
            return "";
        }

        for(String roomId: roomIds){
            TreeMap<Integer, Meeting> bookings = roomBookings.get(roomId);

            if(!isOverlapping(bookings, startTime, endTime)){
                Meeting meeting = new Meeting(meetingId, roomId, startTime, endTime);
                bookings.put(startTime, meeting);
                meetings.put(meetingId, meeting);
                return roomId;
            }

        }
        return "";
    }

    private boolean isOverlapping(TreeMap<Integer, Meeting> bookings, Integer startTime, Integer endTime) {
        // Only the closest previous and next meetings can overlap.
        Map.Entry<Integer, Meeting> previous = bookings.floorEntry(startTime);

        if (previous != null && previous.getValue().endTime >= startTime) {
            return true;
        }

        Map.Entry<Integer, Meeting> next = bookings.ceilingEntry(startTime);

        if(next != null && endTime >= next.getValue().startTime){
            return true;
        }

        return false;
    }

    public boolean cancelMeeting(String meetingId) {

        Meeting meeting = meetings.remove(meetingId);

        if (meeting == null) {
            return false;
        }

        TreeMap<Integer, Meeting> bookings = roomBookings.get(meeting.roomId);

        bookings.remove(meeting.startTime);

        return true;
    }
}
