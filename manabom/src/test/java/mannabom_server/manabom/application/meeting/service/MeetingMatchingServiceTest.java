package mannabom_server.manabom.application.meeting.service;

import mannabom_server.manabom.application.chat.service.ChatRoomService;
import mannabom_server.manabom.application.meeting.dto.response.MatchingResultDataDto;
import mannabom_server.manabom.domain.meeting.entity.Meeting;
import mannabom_server.manabom.domain.meeting.entity.MeetingMatch;
import mannabom_server.manabom.domain.meeting.enums.MatchingStatus;
import mannabom_server.manabom.domain.meeting.repository.MeetingMatchRepository;
import mannabom_server.manabom.domain.meeting.repository.MeetingRepository;
import mannabom_server.manabom.infrastructure.redis.meetingmatching.RedisMatchAtomicOps;
import mannabom_server.manabom.infrastructure.redis.meetingmatching.RedisMatchHistory;
import mannabom_server.manabom.infrastructure.redis.meetingmatching.RedisMatchMeta;
import mannabom_server.manabom.infrastructure.redis.meetingmatching.RedisMatchQueue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingMatchingServiceTest {

    @Mock private RedisMatchQueue queue;
    @Mock private RedisMatchHistory history;
    @Mock private RedisMatchAtomicOps atomic;
    @Mock private RedisMatchMeta meta;
    @Mock private MeetingMatchRepository meetingMatchRepository;
    @Mock private MeetingRepository meetingRepository;
    @Mock private MeetingMemberReadService meetingMemberReadService;
    @Mock private ChatRoomService chatRoomService;
    @Mock private MeetingMemberService meetingMemberService;
    @Mock private MeetingService meetingService;
    @Mock private MeetingMatchingTimeoutScheduler meetingMatchingTimeoutScheduler;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private MeetingMatchingService meetingMatchingService;

    @Test
    void returnsFailedResultWithoutLoadingOpponentMembers() {
        Meeting meeting1 = Meeting.builder().id(10L).build();
        Meeting meeting2 = Meeting.builder().id(11L).build();
        MeetingMatch match = org.mockito.Mockito.mock(MeetingMatch.class);
        when(match.getId()).thenReturn(20L);
        when(match.getMeeting1()).thenReturn(meeting1);
        when(match.getMeeting2()).thenReturn(meeting2);
        when(match.getMatchingStatus()).thenReturn(MatchingStatus.FAILED);
        when(meetingMatchRepository.findByIdWithMeeting(20L)).thenReturn(Optional.of(match));
        when(meetingMemberService.isMember(10L, 1L)).thenReturn(true);

        MatchingResultDataDto response = meetingMatchingService.getMatchingResult(20L, 1L);

        assertThat(response.getMatchingStatus()).isEqualTo(MatchingStatus.FAILED);
        assertThat(response.isRequeued()).isTrue();
        assertThat(response.getOpponentTeamInfo()).isNull();
        verifyNoInteractions(meetingMemberReadService);
    }
}
