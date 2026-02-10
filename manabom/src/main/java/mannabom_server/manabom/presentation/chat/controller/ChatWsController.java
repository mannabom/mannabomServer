package mannabom_server.manabom.presentation.chat.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.chat.dto.response.ChatMessageEvent;
import mannabom_server.manabom.application.chat.dto.request.ChatSendRequest;
import mannabom_server.manabom.domain.chat.enums.ChatRoomType;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWsController {
    private final SimpMessagingTemplate simpMessagingTemplate;

    @MessageMapping("/chat.send")
    public void send(ChatSendRequest request, Principal principal){
        if(principal==null) throw new IllegalArgumentException("채팅보내기: 인증되지 않은 웹소켓 세션");
        if(request.getRoomId()==null) throw new IllegalArgumentException("채팅보내기: 방번호가 입력되지 않음");
        if(request.getContent()==null || request.getContent().isBlank()) throw new IllegalArgumentException("채팅보내기: 내용이 입력되지 않음.");

        Long senderUserId;
        try {
            senderUserId = Long.parseLong(principal.getName());
        } catch (NumberFormatException e) {
            log.error("채팅보내기: 웹소켓의 유저 name이 Long이 아님. principal={},session invalid ",principal.getName(),e);
            throw new IllegalArgumentException("채팅보내기: 잘못된 사용자 아이디 Long 타입이 아님");
        }

        ChatMessageEvent event = ChatMessageEvent.builder()
                .roomId(request.getRoomId())
                .roomType(request.getChatRoomType().name())
                .sendAt(Instant.now())
                .senderUserId(senderUserId)
                .content(request.getContent())
                .messageId(null)
                .clientMessageId(request.getClientMeesageId())
                .messageType(request.getMessageType().name())
                .build();

        simpMessagingTemplate.convertAndSend(topicBy(request.getChatRoomType())+request.getRoomId(),event);

    }
    private String topicBy(ChatRoomType type){
        return switch(type){
            case PROFILE_MATCH -> "/topic/dm-profile/rooms/";
            case LOVEVIEW_MATCH -> "/topic/dm-code/rooms/";
            case MEETING_GROUP -> "/topic/meeting-group/rooms/";
            case MEETING_MATCH -> "/topic/meeting-match/rooms/";
        };
    }

}
