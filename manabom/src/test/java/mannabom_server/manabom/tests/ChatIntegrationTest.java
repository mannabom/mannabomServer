package mannabom_server.manabom.tests;

import mannabom_server.manabom.application.chat.dto.event.ChatRoomLeaveEvent;
import mannabom_server.manabom.application.chat.dto.request.ChatSendRequest;
import mannabom_server.manabom.application.chat.dto.response.ChatInitialSyncResponse;
import mannabom_server.manabom.application.chat.dto.response.ChatMessageEvent;
import mannabom_server.manabom.application.chat.dto.response.ChatReadEvent;
import mannabom_server.manabom.application.chat.service.ChatMemberService;
import mannabom_server.manabom.application.chat.service.ChatRoomService;
import mannabom_server.manabom.application.chat.service.ChatService;
import mannabom_server.manabom.application.notification.service.NotificationService;
import mannabom_server.manabom.domain.chat.entity.ChatMember;
import mannabom_server.manabom.domain.chat.entity.ChatMessage;
import mannabom_server.manabom.domain.chat.entity.ChatRoom;
import mannabom_server.manabom.domain.chat.enums.ChatMemberStatus;
import mannabom_server.manabom.domain.chat.enums.ChatMessageType;
import mannabom_server.manabom.domain.chat.enums.ChatRoomType;
import mannabom_server.manabom.domain.chat.repository.ChatMemberRepository;
import mannabom_server.manabom.domain.chat.repository.ChatMessageRepository;
import mannabom_server.manabom.domain.chat.repository.ChatRoomRepository;
import mannabom_server.manabom.domain.meeting.entity.Meeting;
import mannabom_server.manabom.domain.user.entity.Profile;
import mannabom_server.manabom.domain.user.entity.User;
import mannabom_server.manabom.domain.user.repository.ProfileRepository;
import mannabom_server.manabom.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatIntegrationTest {

    // 테스트 대상 서비스들
    @InjectMocks
    private ChatService chatService;
    @InjectMocks private ChatMemberService chatMemberService;
    @InjectMocks private ChatRoomService chatRoomService;

    // 가짜 객체(Mock)들
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatMemberRepository chatMemberRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProfileRepository profileRepository;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private NotificationService notificationService;

    private User testUser;
    private ChatRoom testRoom;
    private ChatMember testMember;

    @BeforeEach
    void setUp() {
        // 공통 데이터 세팅
        testUser = User.builder().userId(1L).build();
        testRoom = ChatRoom.builder().id(100L).type(ChatRoomType.MEETING_GROUP).build();
        testMember = ChatMember.create(testRoom, testUser);

        // Redis Mock 세팅
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("1. 메시지 전송 테스트: 메시지가 저장되고 알림이 발송되어야 한다")
    void sendMessage_test() {
        // given
        ChatSendRequest request = new ChatSendRequest(100L, ChatMessageType.TEXT, "안녕하세요", "client-msg-id-1");
        Profile profile = Profile.builder().nickName("테스터").build();

        when(chatRoomRepository.findById(100L)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(profileRepository.findByUser(testUser)).thenReturn(Optional.of(profile));
        when(chatMemberRepository.findAllByRoomIdAndStatus(100L, ChatMemberStatus.ACTIVATE)).thenReturn(List.of(testMember));
        when(chatMemberRepository.countChatMemberByRoomIdAndStatus(100L, ChatMemberStatus.ACTIVATE)).thenReturn(2);
        when(chatMemberRepository.findByRoomIdAndUser_UserIdAndStatus(100L, 1L, ChatMemberStatus.ACTIVATE)).thenReturn(Optional.of(testMember));

        // when
        chatService.sendMessage(request, 1L);

        // then
        verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
        verify(valueOperations).set(startsWith("unread:count:"), anyString());
        verify(messagingTemplate).convertAndSend(eq("/topic/rooms/100"), any(ChatMessageEvent.class));
    }

    @Test
    @DisplayName("2. 읽음 상태 업데이트 테스트: Redis 카운트가 감소하고 읽음 이벤트가 발행되어야 한다")
    void updateReadStatus_test() {
        // given
        Long roomId = 100L; Long userId = 1L; Long lastSeenId = 50L;
        ChatMessage unreadMsg = ChatMessage.builder().id(49L).build();

        when(chatMemberRepository.findByRoomIdAndUser_UserIdAndStatus(roomId, userId, ChatMemberStatus.ACTIVATE))
                .thenReturn(Optional.of(testMember));
        when(chatMessageRepository.findByRoomIdAndIdBetween(roomId, 1L, lastSeenId))
                .thenReturn(List.of(unreadMsg));
        when(valueOperations.decrement("unread:count:49")).thenReturn(0L);

        // when
        chatMemberService.updateReadStatus(roomId, userId, lastSeenId);

        // then
        verify(valueOperations).decrement("unread:count:49");
        verify(messagingTemplate).convertAndSend(eq("/topic/rooms/100/read"), any(ChatReadEvent.class));
    }

    @Test
    @DisplayName("3. 채팅방 나가기 테스트: 멤버 상태가 변경되고 외부 이벤트가 발행되어야 한다")
    void leaveChatRoom_test() {
        // given
        Long roomId = 100L; Long userId = 1L;
        Meeting meeting = Meeting.builder().id(10L).build();
        // 리플렉션이나 빌더로 meeting 세팅 (testRoom에 meeting 연관관계가 있다고 가정)
        testRoom = ChatRoom.createMeetingChatRoom(meeting);

        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(chatMemberRepository.findByRoomIdAndUser_UserIdAndStatus(roomId, userId, ChatMemberStatus.ACTIVATE))
                .thenReturn(Optional.of(testMember));

        // when
        chatRoomService.leaveChatRoom(roomId, userId);

        // then
        assertEquals(ChatMemberStatus.DEACTIVATED, testMember.getStatus());
        verify(eventPublisher).publishEvent(any(ChatRoomLeaveEvent.class));
    }

    @Test
    @DisplayName("4. 동기화 데이터 조회 테스트: 안 읽은 메시지 개수가 정확히 합산되어야 한다")
    void getInitialSync_test() {
        // given
        when(chatMemberRepository.findAllByUser_UserIdAndStatus(1L, ChatMemberStatus.ACTIVATE))
                .thenReturn(List.of(testMember));
        when(chatMessageRepository.countUnreadMessages(any(), anyLong())).thenReturn(3);

        // when
        ChatInitialSyncResponse response = chatService.getInitialSync(1L);

        // then
        assertEquals(3, response.getUnreadMessageCount());
        verify(chatMessageRepository).countUnreadMessages(any(), anyLong());
    }
}
