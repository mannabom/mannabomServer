package mannabom_server.manabom.domain.meeting.repository;

import mannabom_server.manabom.application.meeting.dto.common.MeetingListCursor;
import mannabom_server.manabom.application.meeting.dto.request.MeetingRoomsSearchRequest;
import mannabom_server.manabom.domain.meeting.entity.Meeting;
import mannabom_server.manabom.domain.meeting.enums.MeetingBucket;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MeetingRepositoryCustom {

    List<Meeting> fetchBucketPage(MeetingRoomsSearchRequest request, MeetingBucket meetingBucket, MeetingListCursor cursor, int limit);
}
