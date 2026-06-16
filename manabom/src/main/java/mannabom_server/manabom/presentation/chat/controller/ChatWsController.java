package mannabom_server.manabom.presentation.chat.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.chat.dto.response.ChatMessageEvent;
import mannabom_server.manabom.application.chat.dto.request.ChatSendRequest;
import mannabom_server.manabom.application.chat.service.ChatService;
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
    private final ChatService chatService;

    @MessageMapping("/chat.send")
    public void send(ChatSendRequest request, Principal principal){
        if(principal==null) throw new IllegalArgumentException("채팅보내기: 인증되지 않은 웹소켓 세션");

        Long senderUserId;
        try {
            senderUserId = Long.parseLong(principal.getName());
        } catch (NumberFormatException e) {
            log.error("채팅보내기: 웹소켓의 유저 name이 Long이 아님. principal={},session invalid ",principal.getName(),e);
            throw new IllegalArgumentException("채팅보내기: 잘못된 사용자 아이디 Long 타입이 아님");
        }
        chatService.sendMessage(request, senderUserId);
    }


}
