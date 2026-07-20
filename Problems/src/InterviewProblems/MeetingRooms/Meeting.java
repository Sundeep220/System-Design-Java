package InterviewProblems.MeetingRooms;

public class Meeting {
    String meetingId;
    String roomId;
    Integer startTime;
    Integer endTime;

    public Meeting(String meetingId, String roomId, Integer startTime, Integer endTime) {
        this.meetingId = meetingId;
        this.roomId = roomId;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
