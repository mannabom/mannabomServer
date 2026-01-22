package mannabom_server.manabom.application.meeting.dto.common;

import java.util.List;

public record MeetingPage<T>(String nextCursor, boolean hasNext, List<T> meetings){

}
