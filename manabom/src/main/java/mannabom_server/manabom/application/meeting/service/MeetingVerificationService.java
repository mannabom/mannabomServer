package mannabom_server.manabom.application.meeting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.domain.chat.entity.ChatMember;
import mannabom_server.manabom.domain.chat.entity.ChatRoom;
import mannabom_server.manabom.domain.chat.repository.ChatMemberRepository;
import mannabom_server.manabom.domain.chat.repository.ChatRoomRepository;
import mannabom_server.manabom.domain.meeting.entity.Meeting;
import mannabom_server.manabom.domain.meeting.entity.MeetingParticipant;
import mannabom_server.manabom.domain.meeting.entity.MeetingVerification;
import mannabom_server.manabom.domain.meeting.repository.MeetingParticipantRepository;
import mannabom_server.manabom.domain.meeting.repository.MeetingVerificationRepository;
import mannabom_server.manabom.domain.user.enums.Gender;
import mannabom_server.manabom.domain.user.repository.ProfileRepository;
import mannabom_server.manabom.global.util.LocationUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class MeetingVerificationService {
    private final MeetingVerificationRepository meetingVerificationRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final ProfileRepository profileRepository;

    private final StringRedisTemplate stringRedisTemplate;
    private final SimpMessagingTemplate simpMessagingTemplate;

    private static final String POS_KEY = "meeting:pos:%d:%d";
    private static final String GATHER_KEY = "meeting:gather:%d";
    private static final String FINAL_LOC_KEY = "meeting:final:%d";
    private static final double LIMIT_DISTANCE = 200.0;
    private static final int POS_TTL_MINS = 30;


    public String verifyMeeting(Long chatRoomId, Long userId, double latitude, double longitude){
        ChatRoom room = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(()-> new IllegalArgumentException("존재 하지 않는 채팅방 입니다."));
        MeetingVerification verification = meetingVerificationRepository.findByRoomId(chatRoomId)
                .orElseGet(()-> meetingVerificationRepository.save(MeetingVerification.builder().room(room).build()));
        if(verification.isVerified()){
            processLatecomer(chatRoomId, userId, latitude, longitude);
            return "합류 성공! 보상이 지급됩니다.";
        }
        return processGeneral(chatRoomId, userId, latitude, longitude, verification);
    }

    private void processLatecomer(Long chatRoomId, Long userId, double latitude, double longitude ){
        String finalPos = stringRedisTemplate.opsForValue().get(String.format(FINAL_LOC_KEY,chatRoomId));
        if(finalPos == null) throw new IllegalStateException("인증 유효 기간 (당일 자정)이 지났습니다.");

        String[] s = finalPos.split(",");
        if(LocationUtils.calculateDistance(latitude,longitude,Double.parseDouble(s[0]), Double.parseDouble(s[1]))> LIMIT_DISTANCE)
            throw new IllegalStateException("인증 장소와 너무 멉니다!(200m 이내)");

        //보상
        reward(chatRoomId, List.of(userId));
        simpMessagingTemplate.convertAndSend("/topic/chat/"+ chatRoomId, "LATECOMER_OK:"+userId);
    }

    private String processGeneral(Long chatRoomId, Long userId, double latitude, double longitude, MeetingVerification verification ){
        String posKey = String.format(POS_KEY, chatRoomId, userId);
        String gatherKey = String.format(GATHER_KEY, chatRoomId);

        stringRedisTemplate.opsForValue().set(posKey, latitude+","+longitude, Duration.ofMinutes(POS_TTL_MINS));
        stringRedisTemplate.opsForSet().add(gatherKey,userId.toString());
        stringRedisTemplate.expire(gatherKey, Duration.ofMinutes(POS_TTL_MINS));

        Set<String> userIds = stringRedisTemplate.opsForSet().members(gatherKey);
        List<Long> nearbyIds = new ArrayList<>();

        if(userIds!=null){
            for(String id: userIds ){
                String pos = stringRedisTemplate.opsForValue().get(String.format(POS_KEY, chatRoomId, Long.parseLong(id)));
                if(pos!=null){
                    String[] s = pos.split(",");
                    if(LocationUtils.calculateDistance(latitude, longitude, Double.parseDouble(s[0]), Double.parseDouble(s[1]))< LIMIT_DISTANCE)
                        nearbyIds.add(Long.parseLong(id));
                }
            }
        }
        int totalMembers = chatMemberRepository.countByRoomIdAndStatus(chatRoomId, Status);
        int nearbyCount = nearbyIds.size();
        if(nearbyCount>=(totalMembers/2)){
            List<ChatMember> members = chatMemberRepository.findAllByIdIn(nearbyIds);
            boolean hasMale = members.stream().anyMatch(
                    m-> profileRepository.findByUser(m.getUser()).get().getGender()== Gender.MALE
            );
            boolean hasFemale = members.stream().anyMatch(
                    m-> profileRepository.findByUser(m.getUser()).get().getGender()== Gender.FEMALE
            );

            if(hasMale && hasFemale){
                verification.verify();
                reward(chatRoomId, nearbyIds);

                LocalDateTime now = LocalDateTime.now();
                Duration timeUntilMidnight = Duration.between(now, now.with(LocalTime.MAX));

                stringRedisTemplate.opsForValue().set(String.format(FINAL_LOC_KEY, chatRoomId), latitude + "," + longitude);
                stringRedisTemplate.delete(gatherKey);

                simpMessagingTemplate.convertAndSend("/topic/chat/" + chatRoomId, "VERIFIED_SUCCESS");
                return "모든 조건 충족! 만남 인증이 완료되었습니다. ✨";

            } else {
                simpMessagingTemplate.convertAndSend("/topic/chat/"+chatRoomId, "WAITING:" + nearbyCount + "/" + totalMembers);
                return "인증 대기 중... (현재 " + nearbyCount + "명 현장 모임)";            }
        }
        simpMessagingTemplate.convertAndSend("/topic/chat/"+chatRoomId, "WAITING:" + nearbyCount + "/" + totalMembers);
        return "인증 대기 중... (현재 " + nearbyCount + "명 현장 모임)";
    }

    private void reward(Long chatRoomId, List<Long> userIds){
        for(Long userId : userIds){
            ChatMember m = chatMemberRepository.findByRoomIdAndUserIdAndStatus(chatRoomId,userId)
                    .orElseThrow(()->new IllegalArgumentException("현재 채팅방에 속해있는 멤버가 아닙니다."));
            MeetingParticipant p = meetingParticipantRepository.findByChatMemberId(m.getId())
                    .orElseThrow(()-> new IllegalStateException("인증 참여 정보가 생성되지 않았습니다."));
            if(!p.isVerified()){
                p.verify();
            }

        }
    }
}
