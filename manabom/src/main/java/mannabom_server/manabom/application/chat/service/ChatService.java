package mannabom_server.manabom.application.chat.service;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.domain.chat.repository.ChatMessageRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatMessageRepository chatMessageRepository;

    //채팅 보내기
    public void sendMessage(){}
    //채팅 읽기
    public void readMessage(){}
    //사진 보내기
    public void sendImage(){}

}
