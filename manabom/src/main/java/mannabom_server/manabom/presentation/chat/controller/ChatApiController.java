package mannabom_server.manabom.presentation.chat.controller;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.chat.dto.request.ChatMessageRequest;
import mannabom_server.manabom.application.chat.dto.response.ChatHistoryResponse;
import mannabom_server.manabom.application.chat.dto.response.ChatInitialSyncResponse;
import mannabom_server.manabom.application.chat.dto.response.ChatRoomListResponse;
import mannabom_server.manabom.application.chat.dto.response.ChatSyncResponse;
import mannabom_server.manabom.application.chat.service.ChatMemberService;
import mannabom_server.manabom.application.chat.service.ChatRoomService;
import mannabom_server.manabom.application.chat.service.ChatService;
import mannabom_server.manabom.application.common.dto.ApiResponse;
import mannabom_server.manabom.application.signup.service.S3FileUploadService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatApiController {
    private final ChatMemberService chatMemberService;
    private final S3FileUploadService s3FileUploadService;
    private final ChatService chatService;
    private final ChatRoomService chatRoomService;

    @PostMapping("/rooms/{roomId}/read")
    public ResponseEntity<ApiResponse<Void>> readMessage(@PathVariable Long roomId, @AuthenticationPrincipal Long userId,@RequestBody ChatMessageRequest request){
        chatMemberService.updateReadStatus(roomId, userId, request.getLastReadMessageId());
        return ResponseEntity.ok(ApiResponse.success(null, "읽음처리가 완료되었습니다."));
    }

    @PostMapping("/upload/image")
    public ResponseEntity<ApiResponse<String>> uploadChatImage(@RequestParam("file")MultipartFile file){
        return ResponseEntity.ok(ApiResponse.success(s3FileUploadService.uploadFile(file, "chat"), "이미지 업로드 성공하였습니다."));
    }

    @GetMapping("/sync/initial")
    public ResponseEntity<ApiResponse<ChatInitialSyncResponse>> getInitialSync(@AuthenticationPrincipal Long userId){
        ChatInitialSyncResponse response = chatService.getInitialSync(userId);
        return ResponseEntity.ok(ApiResponse.success(response, "초기 동기화 데이터 조회 완료했습니다."));
    }

    @GetMapping("/sync/list")
    public ResponseEntity<ApiResponse<List<ChatRoomListResponse>>> getChatRoomList(@AuthenticationPrincipal Long userId){
        List<ChatRoomListResponse> response = chatService.getChatRoomListSync(userId);
        return ResponseEntity.ok(ApiResponse.success(response, "채팅방 리스트 동기화 완료했습니다."));
    }

    @GetMapping("/sync/chat/{roomId}/")
    public ResponseEntity<ApiResponse<ChatSyncResponse>> getLatestChatMessagesList(@PathVariable Long roomId, @AuthenticationPrincipal Long userId, @RequestBody ChatMessageRequest request){
        ChatSyncResponse response = chatService.getLatestChatMessageListSync(roomId, userId, request.getLastReadMessageId());
        return ResponseEntity.ok(ApiResponse.success(response,"최신 채팅 메시지 리스트 동기화 완료했습니다."));
    }

    @GetMapping("/history/chat/{roomId}")
    public ResponseEntity<ApiResponse<ChatHistoryResponse>> getChatMessagesListHistory(@PathVariable Long roomId, @AuthenticationPrincipal Long userId, @RequestBody ChatMessageRequest request){
        ChatHistoryResponse response = chatService.getChatHistory(roomId, userId, request.getLastReadMessageId());
        return ResponseEntity.ok(ApiResponse.success(response,"과거 채팅 메시지 조회 완료했습니다."));
    }

    @DeleteMapping("/rooms/{roomId}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveChatRoom(@PathVariable Long roomId, @AuthenticationPrincipal Long userId){
        chatRoomService.leaveChatRoom(roomId, userId);
        return ResponseEntity.ok(ApiResponse.success(null,"채팅방 나가기 완료했습니다."));
    }


}

