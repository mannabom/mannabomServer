package mannabom_server.manabom.application.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.meeting.service.MeetingMatchingService;
import mannabom_server.manabom.application.meeting.service.MeetingMemberService;
import mannabom_server.manabom.application.notification.dto.SseData;
import mannabom_server.manabom.application.pushService.service.pushSender.PushService;
import mannabom_server.manabom.domain.meeting.entity.Meeting;
import mannabom_server.manabom.domain.meeting.entity.MeetingMatch;
import mannabom_server.manabom.domain.meeting.entity.MeetingMember;
import mannabom_server.manabom.domain.meeting.enums.MeetingDecision;
import mannabom_server.manabom.domain.meeting.enums.SseEventName;
import mannabom_server.manabom.domain.meeting.repository.MeetingMatchRepository;
import mannabom_server.manabom.domain.notification.entity.Notification;
import mannabom_server.manabom.domain.notification.repository.NotificationRepository;
import mannabom_server.manabom.domain.pushMessage.PushMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final SseService sseService;
    private final PushService pushService;
    private final MeetingMemberService meetingMemberService;

    private final NotificationRepository notificationRepository;
    private final MeetingMatchRepository meetingMatchRepository;

    private final ObjectMapper objectMapper;

    @Transactional
    public void sendMatchFound(Long matchId, Instant decisionDeadLine){
        MeetingMatch match = findMatch(matchId);
        String timeStr = formatTime(decisionDeadLine);

        String title = "두근두근 새 매칭 도착! 💘";
        String message = timeStr + "까지 수락 여부를 결정해주세요.";

        Map<String, Object> data = new HashMap<>();
        data.put("matchId", matchId);
        data.put("deadline", decisionDeadLine.toString());




        broadcastToMeetingMembers(match.getMeeting1().getId(),SseEventName.MATCH_FOUND,title, message,data);
        broadcastToMeetingMembers(match.getMeeting2().getId(),SseEventName.MATCH_FOUND,title, message,data);
    }

    @Transactional
    public void sendMatchSuccess(Long matchId, Long chatRoomId,boolean isByTimeout){
        MeetingMatch match = findMatch(matchId);

        Map<String,Object> data = new HashMap<>();
        data.put("matchId",matchId);
        data.put("chatRoomId",chatRoomId);

        sendMatchSuccessToTeam(match.getMeeting1(),match.getMeeting1Decision(),isByTimeout,data);
        sendMatchSuccessToTeam(match.getMeeting2(),match.getMeeting2Decision(),isByTimeout,data);

    }
    private void sendMatchSuccessToTeam(Meeting meeting, MeetingDecision decision, boolean isByTimeout, Map<String, Object> data){
        String title, message;
        if(decision==MeetingDecision.AUTO_ACCEPTED){
            if(isByTimeout){
                title = "매칭 자동 성사 ⏰";
                message = "시간 초과 및 거절권 부족으로 자동 수락되었습니다. 채팅으로 대화를 나눠보세요";
            }else {
                title = "매칭 발견 및 성사 🎉";
                message = "거절권 부족으로 자동 수락되었습니다. 채팅으로 대화를 나눠보세요";
            }
        }
        else{
            title = "매칭 성사! 🎉";
            message = "상대방의 수락으로 매칭이 성사되었습니다! 채팅으로 대화를 나눠보세요";
        }
        broadcastToMeetingMembers(meeting.getId(),SseEventName.MATCHING_COMPLETED,title, message,data);
    }


    @Transactional
    public void sendMatchFailure(Long matchId, Long triggerUserId, boolean isByTimeout){
        MeetingMatch match = findMatch(matchId);

        Map<String,Object> data = new HashMap<>();
        data.put("matchId",matchId);

        String title = "매칭 실패 😢";
        String message;
        if(isByTimeout){
            boolean isMeeting1Timeout = match.getMeeting1Decision()==MeetingDecision.AUTO_REJECTED;
            Long timeoutTeam = isMeeting1Timeout ? match.getMeeting1().getId() : match.getMeeting2().getId();
            Long opponent = isMeeting1Timeout ? match.getMeeting2().getId() : match.getMeeting1().getId();

            message = "시간 안에 응답하지 않아. 자동으로 거절되었습니다. 더 좋은 인연을 찾아볼게요!";
            broadcastToMeetingMembers(timeoutTeam,SseEventName.MATCHING_FAILED,title,message,data);
            message= "상대방의 무응답으로 매칭이 취소되었습니다.더 좋은 인연을 찾아볼게요!";
            broadcastToMeetingMembers(opponent,SseEventName.MATCHING_FAILED,title,message,data);
        }
        else {
            boolean didMeeting1Reject = isMemberOfMeeting(match.getMeeting1().getId(), triggerUserId);            Long rejectTeam = didMeeting1Reject ? match.getMeeting1().getId() : match.getMeeting2().getId();
            Long opponent = didMeeting1Reject ? match.getMeeting2().getId() : match.getMeeting1().getId();

            message = "우리 팀 리더가 매칭을 거절했습니다.더 좋은 인연을 찾아볼게요!";
            broadcastToMeetingMembersExcept(rejectTeam,triggerUserId,SseEventName.MATCHING_FAILED,title,message,data);
            message= "상대방의 거절로 매칭이 취소되었습니다. 더 좋은 인연을 찾아볼게요!" ;
            broadcastToMeetingMembers(opponent,SseEventName.MATCHING_FAILED,title,message,data);
        }
    }

    @Transactional
    public void sendNotification(Long userId, SseEventName event, String title, String message, Object data) {
        String jsonData = null;
        try{
            if(data!=null)
                jsonData= objectMapper.writeValueAsString(data);
        }
         catch (JsonProcessingException e) {
            jsonData = "{}";
        }
        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .type(event)
                .data(jsonData)
                .isRead(false)
                .build();
        notificationRepository.save(notification);

        if (!sseService.send(userId, event, new SseData(title, message, data))) {

            log.info("유저({})가 오프라인이므로 Push 알림을 전송합니다.", userId);
            PushMessage pushMessage = new PushMessage(title, message, convertObjectToMap(event, data));
            pushService.sendToUser(userId, pushMessage);
        }
    }

    private Map<String, String> convertObjectToMap(SseEventName event, Object data) {
        Map<String, String> fcmMap = new HashMap<>();
        fcmMap.put("type", event.getDescription());
        if (data != null) {
           try{
               Map<String, String> dataMap = objectMapper.convertValue(data, new TypeReference<Map<String, String>>() {});
               fcmMap.putAll(dataMap);
           } catch (Exception e){
               log.warn("단순 맵 변환 실패, JSON String으로 대체:{}", e.getMessage());
               try{
                   fcmMap.put("data", objectMapper.writeValueAsString(data));
               } catch (JsonProcessingException ignored) {
               }
           }
        }
        return fcmMap;
    }

    private MeetingMatch findMatch(Long matchId){
        return meetingMatchRepository.findById(matchId)
                .orElseThrow(()->new IllegalArgumentException("알림: 존재하지 않는 매칭 아이디입니다."));
    }

    private String formatTime(Instant instant){
        return DateTimeFormatter.ofPattern("M월 d일(E) HH:mm")
                .withZone(ZoneId.of("Asia/Seoul"))
                .format(instant);
    }

    private void broadcastToMeetingMembers(Long meetingId,SseEventName event, String title,String message, Object data){
        List<MeetingMember> members = meetingMemberService.getActiveMembers(meetingId);

        for(MeetingMember member:members){
            sendNotification(member.getUser().getUserId(),event,title,message,data);
        }
    }
    private void broadcastToMeetingMembersExcept(Long meetingId,Long excludeUserId,SseEventName event, String title,String message, Object data){
        List<MeetingMember> members = meetingMemberService.getActiveMembers(meetingId);

        for(MeetingMember member:members){
            if(!member.getUser().getUserId().equals(excludeUserId))
                sendNotification(member.getUser().getUserId(),event,title,message,data);
        }
    }

    private  boolean isMemberOfMeeting(Long meetingId, Long userId){
        return meetingMemberService.isMember(meetingId,userId);
    }


}