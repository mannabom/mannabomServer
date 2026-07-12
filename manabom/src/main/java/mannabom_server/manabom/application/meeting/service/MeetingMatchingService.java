package mannabom_server.manabom.application.meeting.service;

import jdk.jfr.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.chat.service.ChatRoomService;
import mannabom_server.manabom.application.meeting.dto.request.MeetingMatchingEvent;
import mannabom_server.manabom.application.meeting.dto.response.*;
import mannabom_server.manabom.application.meeting.handler.MatchingEventListener;
import mannabom_server.manabom.application.notification.dto.MatchFailureEvent;
import mannabom_server.manabom.application.notification.dto.MatchFoundEvent;
import mannabom_server.manabom.application.notification.dto.MatchSuccessEvent;
import mannabom_server.manabom.application.notification.service.NotificationService;
import mannabom_server.manabom.domain.meeting.entity.Meeting;
import mannabom_server.manabom.domain.meeting.entity.MeetingMatch;
import mannabom_server.manabom.domain.meeting.entity.MeetingMember;
import mannabom_server.manabom.domain.meeting.enums.MatchingStatus;
import mannabom_server.manabom.domain.meeting.enums.MeetingDecision;
import mannabom_server.manabom.domain.meeting.repository.MeetingMatchRepository;
import mannabom_server.manabom.domain.meeting.repository.MeetingRepository;
import mannabom_server.manabom.infrastructure.redis.meetingmatching.RedisMatchAtomicOps;
import mannabom_server.manabom.infrastructure.redis.meetingmatching.RedisMatchHistory;
import mannabom_server.manabom.infrastructure.redis.meetingmatching.RedisMatchMeta;
import mannabom_server.manabom.infrastructure.redis.meetingmatching.RedisMatchQueue;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Slf4j
@RequiredArgsConstructor
@Service
public class MeetingMatchingService {

    private final RedisMatchQueue queue;
    private final RedisMatchHistory history;
    private final RedisMatchAtomicOps atomic;
    private final RedisMatchMeta meta;

    private final MeetingMatchRepository meetingMatchRepository;
    private final MeetingRepository meetingRepository;

    private final MeetingMemberReadService meetingMemberReadService;
    private final ChatRoomService chatRoomService;
    private final MeetingMemberService meetingMemberService;
    private final MeetingService meetingService;

    private final MeetingMatchingTimeoutScheduler meetingMatchingTimeoutScheduler;

    private final ApplicationEventPublisher eventPublisher;

    private static final long URGENT_MS = 24L*60*60*1000; //24시간
    private static final int SCAN_LIMIT = 50;
    private static final int TARGET_MATCH_COUNT = 20;
    private static final List<String> METRO_CODES = List.of(
            "11","26","27","28","29","30","31","50"
    );

    public List<Long> findOpponent(MeetingMatchingEvent event){
        String targetGender = oppositeGender(event.getGender());
        Long meetingId = event.getMeetingId();

        Set<Long> myHistory = history.getHistory(meetingId);
        List<Long> finalMatchCandidates = new ArrayList<>();
        if(isAnyPreference(event)){
            findAnyType(event, targetGender, meetingId, myHistory,finalMatchCandidates);
        } else if(isMetro(event)){
            findMetroType(event, targetGender, meetingId, myHistory,finalMatchCandidates);
        } else{
            findNonMetroType(event, targetGender, meetingId, myHistory,finalMatchCandidates);
        }
        return finalMatchCandidates;
    }

    private void findAnyType(MeetingMatchingEvent event, String gender, Long meetingId, Set<Long> myHistory, List<Long> finalMatchCandidates){
        long now = System.currentTimeMillis();
        //0순위 같은 시도 전체 + 오래 기다림
        Set<String> p0 = queue.longWaitingCandidates(event.getSidoCode(),event.getMemberCount(),gender,now-URGENT_MS,SCAN_LIMIT);
        accumulateMatchCandidate(p0,meetingId, myHistory,finalMatchCandidates,TARGET_MATCH_COUNT);
        if(finalMatchCandidates.size()>= TARGET_MATCH_COUNT) return;

        //1순위 같은 시도 전체 + 나이 비슷d
        Set<String> p1 = queue.similarAgeInSidoCandidates(event.getSidoCode(), event.getMemberCount(), gender,event.getAvgage(),SCAN_LIMIT);
        accumulateMatchCandidate(p1,meetingId,myHistory,finalMatchCandidates,TARGET_MATCH_COUNT);
        if(finalMatchCandidates.size()>= TARGET_MATCH_COUNT) return;

        //2순위 같은 시도 전체 + 나이 안비슷
        Set<String> p2 = queue.anyAgeInSidoCandidates(event.getSidoCode(), event.getMemberCount(), gender,SCAN_LIMIT);
        accumulateMatchCandidate(p2,meetingId, myHistory, finalMatchCandidates,TARGET_MATCH_COUNT);
    }

    private void findMetroType(MeetingMatchingEvent event, String gender, Long meetingId, Set<Long> myHistory, List<Long> finalMatchCandidates){
        long now = System.currentTimeMillis();
        //0순위 같은 시도 + 오래 기다림
        Set<String> p0 = queue.longWaitingCandidates(event.getSidoCode(),event.getMemberCount(),gender,now-URGENT_MS,SCAN_LIMIT);
        accumulateMatchCandidate(p0,meetingId,myHistory, finalMatchCandidates,TARGET_MATCH_COUNT);
        if(finalMatchCandidates.size()>= TARGET_MATCH_COUNT) return;

        //1순위 같은 시도 같은 시군구(전체도 포함)  나이 비슷
        Set<String> p1 = queue.similarAgeInSigunguCandidates(event.getSidoCode(),event.getSigunguCode(), event.getMemberCount(),gender,event.getAvgage(),SCAN_LIMIT);
        accumulateMatchCandidate(p1,meetingId,myHistory, finalMatchCandidates,TARGET_MATCH_COUNT);
        if(finalMatchCandidates.size()>= TARGET_MATCH_COUNT) return;

        //2순위 같은 시도 다른 시군구 나이 비슷
        Set<String> p2 = queue.similarAgeInSidoCandidates(event.getSidoCode(), event.getMemberCount(), gender,event.getAvgage(),SCAN_LIMIT);
        accumulateMatchCandidate(p2,meetingId,myHistory, finalMatchCandidates,TARGET_MATCH_COUNT);
        if(finalMatchCandidates.size()>= TARGET_MATCH_COUNT) return;

        //3순위 같은 시도 같은 시군구 나이 무관
        Set<String> p3 =queue.anyAgeInSigunguCandidates(event.getSidoCode(),event.getSigunguCode(),event.getMemberCount(),gender,SCAN_LIMIT);
        accumulateMatchCandidate(p3,meetingId,myHistory, finalMatchCandidates,TARGET_MATCH_COUNT);
        if(finalMatchCandidates.size()>= TARGET_MATCH_COUNT) return;

        //4순위 같은 시도 다른 시군구 나이 무관
        Set<String> p4 =queue.anyAgeInSidoCandidates(event.getSidoCode(), event.getMemberCount(), gender, SCAN_LIMIT);
        accumulateMatchCandidate(p4,meetingId,myHistory, finalMatchCandidates,TARGET_MATCH_COUNT);

    }

    private void findNonMetroType(MeetingMatchingEvent event, String gender, Long meetingId, Set<Long> myHistory, List<Long> finalMatchCandidates){
        long now = System.currentTimeMillis();
        //0순위 같은 시도 같은 시군구 오래 기다림
        Set<String> p0 = queue.longWaitingInSigunguCandidates(event.getSidoCode(), event.getSigunguCode(), event.getMemberCount(), event.getGender(), now-URGENT_MS,SCAN_LIMIT);
        accumulateMatchCandidate(p0,meetingId,myHistory, finalMatchCandidates,TARGET_MATCH_COUNT);
        if(finalMatchCandidates.size()>= TARGET_MATCH_COUNT) return;

        //1순위 같은 시도 같은 시군구 나이 비슷
        Set<String> p1 = queue.similarAgeInSigunguCandidates(event.getSidoCode(), event.getSigunguCode(), event.getMemberCount(), event.getGender(), event.getAvgage(),SCAN_LIMIT);
        accumulateMatchCandidate(p1,meetingId,myHistory, finalMatchCandidates,TARGET_MATCH_COUNT);
        if(finalMatchCandidates.size()>= TARGET_MATCH_COUNT) return;

        //2순위 같은 시도 같은 시군구 나이 무관
        Set<String> p2 = queue.anyAgeInSigunguCandidates(event.getSidoCode(), event.getSigunguCode(), event.getMemberCount(), event.getGender(), SCAN_LIMIT);
        accumulateMatchCandidate(p2,meetingId,myHistory, finalMatchCandidates,TARGET_MATCH_COUNT);
    }

    private void accumulateMatchCandidate(Set<String> redisCandidates, Long meetingId, Set<Long> myHistory, List<Long> finalMatchCandidate, int limit){
        if(finalMatchCandidate.size()>= limit) return;
        if(redisCandidates==null || redisCandidates.isEmpty()) return;

        List<Long> parsedList = new ArrayList<>(redisCandidates.stream()
                .map(Long::parseLong)
                .filter(id -> !id.equals(meetingId))
                .filter(id -> !myHistory.contains(id))
                .filter(id -> !finalMatchCandidate.contains(id))
                .toList());

        Collections.shuffle(parsedList);


       int need = limit - finalMatchCandidate.size();
       for(Long candidate: parsedList){
           if(need<=0) break;
           finalMatchCandidate.add(candidate);
           need--;
       }
    }




    //매칭확정
    public boolean confirmAndRemoveOpponent(Long opponentId, MeetingMatchingEvent me, String opponentSigunguCode ){
        String targetGender = oppositeGender(me.getGender());

        List<String> keys = List.of(
                queue.timeKey(me.getSidoCode(), me.getMemberCount(), targetGender),
                queue.ageSigKey(me.getSidoCode(),opponentSigunguCode, me.getMemberCount(), targetGender),
                queue.timeSigKey(me.getSidoCode(),opponentSigunguCode,me.getMemberCount(),targetGender),
                queue.ageSidoKey(me.getSidoCode(),me.getMemberCount(),targetGender)
        );

        return atomic.atomicRemove(opponentId, keys);
    }

    public MeetingMatchingEvent getMatchingMetaInfo(Long meetingId){
        return meta.get(meetingId);
    }

    public void addToQueue(MeetingMatchingEvent event){
        meta.saveMeta(event);
        queue.add(event);
        log.info("대기 큐에 입장 {}",event.getMeetingId());
    }

    @Transactional
    public void processMatchSuccess(MeetingMatchingEvent me, MeetingMatchingEvent opponent){
        queue.remove(me);

        meta.delete(me.getMeetingId());
        meta.delete(opponent.getMeetingId());

        history.addHistory(me.getMeetingId(), opponent.getMeetingId());
        Meeting meeting1 = meetingRepository.findById(me.getMeetingId())
                .orElseThrow(()-> new IllegalArgumentException("미팅 매칭 성공: 존재하지 않는 미팅 id입니다."+me.getMeetingId()));
        Meeting meeting2 = meetingRepository.findById(opponent.getMeetingId())
                .orElseThrow(()-> new IllegalArgumentException("미팅 매칭 성공: 존재하지 않는 미팅 id입니다."+opponent.getMeetingId()));
        meeting1.changeToPendingStatus();
        meeting2.changeToPendingStatus();
        MeetingMatch meetingMatch = MeetingMatch.builder()
                .meeting1(meeting1)
                .meeting2(meeting2)
                .build();

        meetingMatchRepository.save(meetingMatch);

        meetingMatchingTimeoutScheduler.scheduleExpiration(meetingMatch.getId());

        checkRejectCount(meeting1, meetingMatch);
        checkRejectCount(meeting2, meetingMatch);

        if(meetingMatch.getMatchingStatus() == MatchingStatus.SUCCEEDED){
            handleMatchSuccess(meetingMatch,false);
        }
        else eventPublisher.publishEvent(new MatchFoundEvent(meetingMatch.getId(), meetingMatch.getDecisionDeadLine()));
    }

    private void checkRejectCount(Meeting meeting, MeetingMatch meetingMatch) {
        if(meeting.getRemainingRejectCount().equals(0))
            meetingMatch.autoAccept(meeting.getId());
    }

    @Transactional
    public void rollbackMatch(MeetingMatchingEvent me, MeetingMatchingEvent opponent){
        log.warn("매칭 롤백 수행: {} <-> {}", me.getMeetingId(), opponent.getMeetingId());
        history.removeHistory(me.getMeetingId(), opponent.getMeetingId());
        addToQueue(opponent);
        addToQueue(me);

        Meeting meeting1 = meetingRepository.findById(me.getMeetingId())
                .orElseThrow(()-> new IllegalArgumentException("미팅 매칭 롤백: 존재하지 않는 미팅 id입니다."+me.getMeetingId()));
        Meeting meeting2 = meetingRepository.findById(opponent.getMeetingId())
                .orElseThrow(()-> new IllegalArgumentException("미팅 매칭 롤백: 존재하지 않는 미팅 id입니다."+opponent.getMeetingId()));
        meeting1.restoreStatus();
        meeting2.restoreStatus();

    }




    /*두 팀이 동시에 수락 또는 거절 누를 수 없게 lock*/
    @Transactional
    public AcceptMatchDataDto acceptMatch(Long matchId,Long userId){
        MeetingMatch match = meetingMatchRepository.findByIdWithLockAndMeeting(matchId)
                .orElseThrow(()-> new IllegalArgumentException("매칭수락: 존재하지 않는 매칭 아이디 입니다."+matchId));

        Meeting myMeeting = getMyMeeting(match.getMeeting1(), match.getMeeting2(), userId);
        match.accept(myMeeting.getId());
        if(match.getMatchingStatus()== MatchingStatus.SUCCEEDED){
            log.info("매칭 성사! 채팅방 생성을 위한 추가 정보 로딩... matchId={}", matchId);
            MatchedChatRoomInfo info = handleMatchSuccess(match,false);
            //상대방 알림
            return AcceptMatchDataDto.ofMatched(info);
        }
        return AcceptMatchDataDto.ofWaiting(match.getDecisionDeadLine());
    }

    private Meeting getMyMeeting(Meeting meeting1, Meeting meeting2, Long userId){
        if(meetingMemberService.isLeader(meeting1.getId(), userId))
            return meeting1;
        else if(meetingMemberService.isLeader(meeting2.getId(), userId))
            return meeting2;
        else throw new IllegalArgumentException("매칭 수락: 리더만이 매칭을 수락할 수 있습니다." + userId);
    }

    @Transactional
    public RejectMatchDataDto rejectMatch(Long matchId, Long userId){
        MeetingMatch match = meetingMatchRepository.findByIdWithLockAndMeeting(matchId)
                .orElseThrow(()-> new IllegalArgumentException("매칭 거절: 존재하지 않는 매칭 아이디 입니다."+matchId));

        Meeting myMeeting = getMyMeeting(match.getMeeting1(), match.getMeeting2(),userId);

        match.reject(myMeeting.getId());

        //둘 다 수락 - 정상 사용자라면 이럴 일 없지만
        if(match.getMatchingStatus() == MatchingStatus.SUCCEEDED){
            handleMatchSuccess(match,false);
        }
        if(match.getMatchingStatus()==MatchingStatus.FAILED){
            handleMatchFailure(match ,userId,false);
        }
        int count = myMeeting.getRemainingRejectCount();
        return new RejectMatchDataDto(match.getMatchingStatus(),count, count > 0);


    }

    private void reQueue(Meeting meeting) {
        MeetingMatchingEvent event = MeetingMatchingEvent.from(meeting);
        addToQueue(event);
    }

    public MatchingResultDataDto getMatchingResult(Long matchId, Long userId){
        MeetingMatch match = meetingMatchRepository.findByIdWithMeeting(matchId)
                .orElseThrow(()-> new IllegalArgumentException("매칭 결과 조회: 존재하지 않는 매칭 아이디 입니다."+ matchId));
        Meeting opponent;
        Meeting meeting1 = match.getMeeting1();
        Meeting meeting2 = match.getMeeting2();

        if(meetingMemberService.isMember(meeting1.getId(),userId))
            opponent= meeting2;
        else if(meetingMemberService.isMember(meeting2.getId(),userId))
            opponent= meeting1;
        else throw new IllegalArgumentException("매칭 결과:해당 매칭에 속하지 않은 유저 아이디 입니다."+ userId);

        List<TeamMemberProfilesDto.TeamMemberDetailDto> members = meetingMemberReadService.getActiveTeammMemberDetails(opponent.getId());
        return MatchingResultDataDto.of(match,opponent,members);

    }

    @Transactional
    public void processAutoDecision(Long matchId){
        MeetingMatch match = meetingMatchRepository.findByIdWithLockAndMeeting(matchId)
                .orElseThrow(()-> new IllegalArgumentException("응답 만료: 존재하지 않는 매칭 아이디입니다. "+matchId));

        if(match.getMatchingStatus()!=MatchingStatus.PENDING){
            return;
        }
        match.processExpiration();
        if(match.getMatchingStatus().equals(MatchingStatus.SUCCEEDED)){
            handleMatchSuccess(match,true);

        }
        else if(match.getMatchingStatus().equals(MatchingStatus.FAILED)) {
            handleMatchFailure(match,null,true);
        }

    }

    private MatchedChatRoomInfo handleMatchSuccess(MeetingMatch match, boolean isByTimeout){
        MatchedChatRoomInfo info =  chatRoomService.createMatchingChatRoom(match);
        eventPublisher.publishEvent(new MatchSuccessEvent(match.getId(),info.getRoomId(),isByTimeout));
        return info;
    }

    private void handleMatchFailure(MeetingMatch match, Long triggerUserId, boolean isByTimeout){
        reQueue(match.getMeeting2());
        reQueue(match.getMeeting1());

        match.getMeeting1().restoreStatus();
        match.getMeeting2().restoreStatus();

        eventPublisher.publishEvent(new MatchFailureEvent(match.getId(), triggerUserId,isByTimeout));
    }


    private String oppositeGender(String gender){
        return gender.equals("MALE") ? "FEMALE" : "MALE";
    }

    private boolean isAnyPreference(MeetingMatchingEvent event)
    {   String code = event.getSigunguCode();
        return code != null && code.endsWith("000");
    }

    private boolean isMetro(MeetingMatchingEvent event){
        return METRO_CODES.contains(event.getSidoCode());
    }

    public void handleMatchMemberLeave(Long matchId, Long userId){
        MeetingMatch match = meetingMatchRepository.findById(matchId)
                .orElseThrow(()-> new IllegalArgumentException("존재하지 않는 미팅 매칭 아이디입니다."));

        MeetingMember mm = meetingMemberService.isActivate(userId)
                .orElseThrow(()-> new IllegalArgumentException("유저가 참여하고 있는 매칭방이 없습니다."));

        Meeting myMeeting;
        Meeting opponent;
        Long userMeetingId = mm.getMeeting().getId();
        if(match.getMeeting1().getId().equals(userMeetingId)){
            myMeeting = match.getMeeting1();
            opponent = match.getMeeting2();
        }
        else if (match.getMeeting2().getId().equals(userMeetingId)){
            myMeeting = match.getMeeting2();
            opponent = match.getMeeting1();
        } else {
            throw new IllegalArgumentException("해당 매칭에 속한 유저가 아닙니다.");
        }

        meetingService.handleMemberLeave(myMeeting.getId(), userId);
        if((myMeeting.getCurrentMembers()+ opponent.getCurrentMembers())*2< myMeeting.getMaxMembers()+ opponent.getMaxMembers()){
            // 다 환불
        }
    }

}
