package mannabom_server.manabom.application.meeting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.domain.chat.entity.ChatMember;
import mannabom_server.manabom.domain.chat.entity.ChatRoom;
import mannabom_server.manabom.domain.chat.enums.ChatMemberStatus;
import mannabom_server.manabom.domain.chat.repository.ChatMemberRepository;
import mannabom_server.manabom.domain.chat.repository.ChatRoomRepository;
import mannabom_server.manabom.domain.meeting.entity.MeetingParticipant;
import mannabom_server.manabom.domain.meeting.entity.MeetingVerification;
import mannabom_server.manabom.domain.meeting.repository.MeetingParticipantRepository;
import mannabom_server.manabom.domain.meeting.repository.MeetingVerificationRepository;
import mannabom_server.manabom.domain.user.enums.Gender;
import mannabom_server.manabom.domain.user.repository.ProfileRepository;
import mannabom_server.manabom.global.util.LocationUtils;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.transaction.annotation.Transactional;

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
    private final RedissonClient redissonClient;
    private final SimpMessagingTemplate simpMessagingTemplate;

    private static final String POS_KEY = "meeting:pos:%d:%d";
    private static final String GATHER_KEY = "meeting:gather:%d";
    private static final String FINAL_LOC_KEY = "meeting:final:%d";
    private static final double LIMIT_DISTANCE = 100.0;
    private static final Duration VERIFICATION_TTL = Duration.ofHours(9);

    @Transactional
    public String verifyMeeting(Long chatRoomId, Long userId, double latitude, double longitude){
        ChatRoom room = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(()-> new IllegalArgumentException("존재 하지 않는 채팅방 입니다."));
        getActiveChatMember(chatRoomId, userId);

        MeetingVerification verification = meetingVerificationRepository.findByRoomId(chatRoomId)
                .orElseGet(()-> meetingVerificationRepository.save(MeetingVerification.builder().room(room).build()));
        Instant now = Instant.now();

        verification.startIfNeeded(now, VERIFICATION_TTL);
        if(verification.isExpired(now)){
            throw new IllegalStateException("만남인증 가능 시간이 지났습니다.");
        }
        if(verification.isVerified()){
            processLatecomer(chatRoomId, userId, latitude, longitude);
            return "합류 성공! 보상이 지급됩니다.";
        }
        return processGeneral(chatRoomId, userId, latitude, longitude, verification);
    }

    @Transactional(readOnly = true)
    public MeetingVerificationStatusData getStatus(Long chatRoomId, Long userId){
        chatRoomRepository.findById(chatRoomId)
                .orElseThrow(()-> new IllegalArgumentException("존재 하지 않는 채팅방 입니다."));
        getActiveChatMember(chatRoomId, userId);

        Optional<MeetingVerification> verificationOpt = meetingVerificationRepository.findByRoomId(chatRoomId);
        int totalMembers = chatMemberRepository.countChatMemberByRoomIdAndStatus(chatRoomId, ChatMemberStatus.ACTIVATE);
        int requiredCount = requiredCount(totalMembers);
        if(verificationOpt.isEmpty()){
            return MeetingVerificationStatusData.notStarted(totalMembers, requiredCount, BestCluster.empty(), false);
        }

        MeetingVerification verification = verificationOpt.get();
        boolean mySubmitted = stringRedisTemplate.hasKey(String.format(POS_KEY, chatRoomId, userId));
        FinalLocation myLocation = getUserLocation(chatRoomId, userId).orElse(null);
        BestCluster bestCluster = findBestCluster(chatRoomId);

        Instant now = Instant.now();
        long remainingSeconds = Math.max(0, verification.remainingTime(now).toSeconds());
        FinalLocation finalLocation = getFinalLocation(chatRoomId).orElse(null);

        if(verification.isVerified()){
            return MeetingVerificationStatusData.verified(
                    verification,
                    remainingSeconds,
                    totalMembers,
                    requiredCount,
                    bestCluster,
                    mySubmitted,
                    myLocation,
                    finalLocation
            );
        }

        if(verification.isExpired(now)){
            return MeetingVerificationStatusData.expired(
                    verification,
                    totalMembers,
                    requiredCount,
                    bestCluster,
                    mySubmitted,
                    myLocation
            );
        }

        return MeetingVerificationStatusData.inProgress(
                verification,
                remainingSeconds,
                totalMembers,
                requiredCount,
                bestCluster,
                mySubmitted,
                myLocation
        );
    }
    private ChatMember getActiveChatMember(Long chatRoomId, Long userId){
        return chatMemberRepository.findByRoomIdAndUser_UserIdAndStatus(
                chatRoomId,
                userId,
                ChatMemberStatus.ACTIVATE
        ).orElseThrow(() -> new IllegalArgumentException("현재 채팅방에 속해있는 멤버가 아닙니다."));
    }


    private void processLatecomer(Long chatRoomId, Long userId, double latitude, double longitude ){
        if(isAlreadyVerifiedParticipant(chatRoomId, userId)){
            throw new IllegalStateException("이미 만남인증에 참여 완료되었습니다.");
        }

        String finalPos = stringRedisTemplate.opsForValue().get(String.format(FINAL_LOC_KEY,chatRoomId));
        if(finalPos == null) throw new IllegalStateException("인증 유효 기간이 지났습니다.");

        String[] s = finalPos.split(",");
        if(LocationUtils.calculateDistance(latitude,longitude,Double.parseDouble(s[0]), Double.parseDouble(s[1]))> LIMIT_DISTANCE)
            throw new IllegalStateException("인증 장소와 너무 멉니다!(100m 이내)");

        Duration positionTtl = verificationRemainingTime(chatRoomId);
        if(positionTtl.isZero() || positionTtl.isNegative()){
            throw new IllegalStateException("만남인증 가능 시간이 지났습니다.");
        }
        saveUserLocation(chatRoomId, userId, latitude, longitude, positionTtl);

        //보상
        reward(chatRoomId, List.of(userId));
        simpMessagingTemplate.convertAndSend("/topic/chat/"+ chatRoomId, "LATECOMER_OK:"+userId);
    }

    private String processGeneral(Long chatRoomId, Long userId, double latitude, double longitude, MeetingVerification verification ){
        String posKey = String.format(POS_KEY, chatRoomId, userId);
        String gatherKey = String.format(GATHER_KEY, chatRoomId);
        Duration positionTtl = verification.remainingTime(Instant.now());
        if(positionTtl.isZero() || positionTtl.isNegative()){
            throw new IllegalStateException("만남인증 가능 시간이 지났습니다.");
        }

        saveUserLocation(chatRoomId, userId, latitude, longitude, positionTtl);
        stringRedisTemplate.opsForSet().add(gatherKey,userId.toString());
        redissonClient.getSet(gatherKey).expire(positionTtl);

        int totalMembers = chatMemberRepository.countChatMemberByRoomIdAndStatus(chatRoomId, ChatMemberStatus.ACTIVATE);
        int requiredCount = requiredCount(totalMembers);
        BestCluster bestCluster = findBestCluster(chatRoomId);
        int nearbyCount = bestCluster.count();

        if(nearbyCount >= requiredCount){
            if(bestCluster.hasMale() && bestCluster.hasFemale()){
                verification.verify();
                reward(chatRoomId, bestCluster.userIds());
                Duration finalLocationTtl = verification.remainingTime(Instant.now());
                if(finalLocationTtl.isZero() || finalLocationTtl.isNegative()){
                    throw new IllegalStateException("만남인증 가능 시간이 지났습니다.");
                }

                stringRedisTemplate.opsForValue().set(
                        String.format(FINAL_LOC_KEY, chatRoomId),
                        bestCluster.latitude() + "," + bestCluster.longitude()
                );
                redissonClient.getBucket(String.format(FINAL_LOC_KEY, chatRoomId)).expire(finalLocationTtl);
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
            ChatMember m = getActiveChatMember(chatRoomId, userId);
            MeetingParticipant p = meetingParticipantRepository.findByChatMemberId(m.getId())
                    .orElseGet(() -> meetingParticipantRepository.save(
                            MeetingParticipant.builder()
                                    .chatMember(m)
                                    .build()
                    ));
            if(!p.isVerified()){
                p.verify();
            }

        }
    }

    private boolean isAlreadyVerifiedParticipant(Long chatRoomId, Long userId){
        ChatMember chatMember = getActiveChatMember(chatRoomId, userId);
        return meetingParticipantRepository.findByChatMemberId(chatMember.getId())
                .map(MeetingParticipant::isVerified)
                .orElse(false);
    }

    private int requiredCount(int totalMembers){
        return Math.max(2, (int) Math.ceil(totalMembers / 2.0));
    }

    private BestCluster findBestCluster(Long chatRoomId){
        List<LocationSubmission> submissions = getLocationSubmissions(chatRoomId);
        if(submissions.isEmpty()){
            return BestCluster.empty();
        }

        BestCluster bestCluster = BestCluster.empty();
        for(LocationSubmission center : submissions){
            List<LocationSubmission> nearby = submissions.stream()
                    .filter(submission -> LocationUtils.calculateDistance(
                            center.latitude(),
                            center.longitude(),
                            submission.latitude(),
                            submission.longitude()
                    ) <= LIMIT_DISTANCE)
                    .toList();

            if(nearby.size() > bestCluster.count()){
                bestCluster = toBestCluster(chatRoomId, nearby);
            }
        }
        return bestCluster;
    }

    private BestCluster toBestCluster(Long chatRoomId, List<LocationSubmission> submissions){
        List<Long> userIds = submissions.stream()
                .map(LocationSubmission::userId)
                .toList();
        List<ChatMember> members = userIds.stream()
                .map(id -> getActiveChatMember(chatRoomId, id))
                .toList();
        boolean hasMale = members.stream().anyMatch(
                m-> profileRepository.findByUser(m.getUser()).get().getGender()== Gender.MALE
        );
        boolean hasFemale = members.stream().anyMatch(
                m-> profileRepository.findByUser(m.getUser()).get().getGender()== Gender.FEMALE
        );
        double latitude = submissions.stream()
                .mapToDouble(LocationSubmission::latitude)
                .average()
                .orElse(0.0);
        double longitude = submissions.stream()
                .mapToDouble(LocationSubmission::longitude)
                .average()
                .orElse(0.0);

        return new BestCluster(userIds, latitude, longitude, hasMale, hasFemale);
    }

    private List<LocationSubmission> getLocationSubmissions(Long chatRoomId){
        String gatherKey = String.format(GATHER_KEY, chatRoomId);
        Set<String> userIds = stringRedisTemplate.opsForSet().members(gatherKey);
        List<LocationSubmission> submissions = new ArrayList<>();

        if(userIds == null){
            return submissions;
        }

        for(String id: userIds){
            Long parsedUserId = Long.parseLong(id);
            String pos = stringRedisTemplate.opsForValue().get(String.format(POS_KEY, chatRoomId, parsedUserId));
            if(pos != null){
                String[] s = pos.split(",");
                submissions.add(new LocationSubmission(
                        parsedUserId,
                        Double.parseDouble(s[0]),
                        Double.parseDouble(s[1])
                ));
            }
        }

        return submissions;
    }

    private Optional<FinalLocation> getFinalLocation(Long chatRoomId){
        String finalPos = stringRedisTemplate.opsForValue().get(String.format(FINAL_LOC_KEY,chatRoomId));
        if(finalPos == null){
            return Optional.empty();
        }
        String[] s = finalPos.split(",");
        return Optional.of(new FinalLocation(Double.parseDouble(s[0]), Double.parseDouble(s[1])));
    }

    private Optional<FinalLocation> getUserLocation(Long chatRoomId, Long userId){
        String pos = stringRedisTemplate.opsForValue().get(String.format(POS_KEY, chatRoomId, userId));
        if(pos == null){
            return Optional.empty();
        }
        String[] s = pos.split(",");
        return Optional.of(new FinalLocation(Double.parseDouble(s[0]), Double.parseDouble(s[1])));
    }

    private void saveUserLocation(Long chatRoomId, Long userId, double latitude, double longitude, Duration ttl){
        stringRedisTemplate.opsForValue().set(
                String.format(POS_KEY, chatRoomId, userId),
                latitude + "," + longitude
        );
        redissonClient.getBucket(String.format(POS_KEY, chatRoomId, userId)).expire(ttl);
    }

    private Duration verificationRemainingTime(Long chatRoomId){
        MeetingVerification verification = meetingVerificationRepository.findByRoomId(chatRoomId)
                .orElseThrow(() -> new IllegalStateException("만남인증 정보가 존재하지 않습니다."));
        return verification.remainingTime(Instant.now());
    }

    public record MeetingVerificationStatusData(
            String status,
            Instant startedAt,
            Instant expiresAt,
            long remainingSeconds,
            int totalMembers,
            int requiredCount,
            int bestClusterCount,
            boolean hasMale,
            boolean hasFemale,
            boolean mySubmitted,
            boolean verified,
            FinalLocation myLocation,
            FinalLocation finalLocation,
            FinalLocation bestClusterLocation
    ) {
        static MeetingVerificationStatusData notStarted(int totalMembers, int requiredCount, BestCluster bestCluster, boolean mySubmitted){
            return new MeetingVerificationStatusData(
                    "NOT_STARTED",
                    null,
                    null,
                    0,
                    totalMembers,
                    requiredCount,
                    bestCluster.count(),
                    bestCluster.hasMale(),
                    bestCluster.hasFemale(),
                    mySubmitted,
                    false,
                    null,
                    null,
                    bestCluster.locationOrNull()
            );
        }

        static MeetingVerificationStatusData inProgress(
                MeetingVerification verification,
                long remainingSeconds,
                int totalMembers,
                int requiredCount,
                BestCluster bestCluster,
                boolean mySubmitted,
                FinalLocation myLocation
        ){
            return new MeetingVerificationStatusData(
                    "IN_PROGRESS",
                    verification.getStartedAt(),
                    verification.getExpiresAt(),
                    remainingSeconds,
                    totalMembers,
                    requiredCount,
                    bestCluster.count(),
                    bestCluster.hasMale(),
                    bestCluster.hasFemale(),
                    mySubmitted,
                    false,
                    myLocation,
                    null,
                    bestCluster.locationOrNull()
            );
        }

        static MeetingVerificationStatusData verified(
                MeetingVerification verification,
                long remainingSeconds,
                int totalMembers,
                int requiredCount,
                BestCluster bestCluster,
                boolean mySubmitted,
                FinalLocation myLocation,
                FinalLocation finalLocation
        ){
            return new MeetingVerificationStatusData(
                    "VERIFIED",
                    verification.getStartedAt(),
                    verification.getExpiresAt(),
                    remainingSeconds,
                    totalMembers,
                    requiredCount,
                    bestCluster.count(),
                    bestCluster.hasMale(),
                    bestCluster.hasFemale(),
                    mySubmitted,
                    true,
                    myLocation,
                    finalLocation,
                    bestCluster.locationOrNull()
            );
        }

        static MeetingVerificationStatusData expired(
                MeetingVerification verification,
                int totalMembers,
                int requiredCount,
                BestCluster bestCluster,
                boolean mySubmitted,
                FinalLocation myLocation
        ){
            return new MeetingVerificationStatusData(
                    "EXPIRED",
                    verification.getStartedAt(),
                    verification.getExpiresAt(),
                    0,
                    totalMembers,
                    requiredCount,
                    bestCluster.count(),
                    bestCluster.hasMale(),
                    bestCluster.hasFemale(),
                    mySubmitted,
                    false,
                    myLocation,
                    null,
                    bestCluster.locationOrNull()
            );
        }
    }

    public record FinalLocation(double latitude, double longitude) {
    }

    private record LocationSubmission(Long userId, double latitude, double longitude) {
    }

    private record BestCluster(List<Long> userIds, double latitude, double longitude, boolean hasMale, boolean hasFemale) {
        static BestCluster empty(){
            return new BestCluster(List.of(), 0.0, 0.0, false, false);
        }

        int count(){
            return userIds.size();
        }

        FinalLocation locationOrNull(){
            if(userIds.isEmpty()){
                return null;
            }
            return new FinalLocation(latitude, longitude);
        }
    }
}
