package mannabom_server.manabom.application.meeting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.chat.service.ChatRoomService;
import mannabom_server.manabom.application.meeting.dto.common.*;
import mannabom_server.manabom.application.meeting.dto.request.MeetingRoomCreateRequest;
import mannabom_server.manabom.application.meeting.dto.request.MeetingRoomJoinByCodeRequest;
import mannabom_server.manabom.application.meeting.dto.request.MeetingRoomJoinRequest;
import mannabom_server.manabom.application.meeting.dto.request.MeetingRoomsSearchRequest;
import mannabom_server.manabom.application.meeting.dto.response.MeetingRoomCreateDataDto;
import mannabom_server.manabom.application.meeting.dto.response.MeetingRoomSearchItemDto;
import mannabom_server.manabom.application.meeting.dto.response.MemberProfilesDto;
import mannabom_server.manabom.application.meeting.dto.response.MyMeetingStatusDataDto;
import mannabom_server.manabom.domain.meeting.entity.Meeting;
import mannabom_server.manabom.domain.meeting.entity.MeetingMember;
import mannabom_server.manabom.domain.meeting.enums.MatchingStatus;
import mannabom_server.manabom.domain.meeting.enums.MeetingBucket;
import mannabom_server.manabom.domain.meeting.enums.Role;
import mannabom_server.manabom.domain.meeting.repository.MeetingRepository;
import mannabom_server.manabom.domain.user.entity.Profile;
import mannabom_server.manabom.domain.user.entity.User;
import mannabom_server.manabom.domain.user.repository.UserRepository;
import mannabom_server.manabom.infrastructure.security.codec.CursorCodec;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MeetingService {
    private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstxyz23456789";
    private static final int CODE_LENGTH = 6;
    private static final SecureRandom random = new SecureRandom();
    private static final int CODE_RETRY_LIMIT = 10;

    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;

    private final ChatRoomService chatRoomService;
    private final MeetingMemberService meetingMemberService;

    private final CursorCodec cursorCodec;
    public User getUserEntity(Long userId){
        return userRepository.findById(userId)
                .orElseThrow(()-> new IllegalArgumentException(" 존재하지 않는 userId입니다."));
    }

    /*미팅 방 생성*/
    @Transactional
    public MeetingRoomCreateDataDto create(MeetingRoomCreateRequest req, Long userId) {
        validateCreateValidation(userId);
        validateAge(req);

        log.info("서비스 계층 미팅 방 생성 시작- User Id : {}", userId);
        User user = getUserEntity(userId);

        for (int attempt = 1; attempt <= CODE_RETRY_LIMIT; attempt++) {
            String inviteCode = generateInviteCode();
            try {
                Meeting meeting = Meeting.create(
                        user,
                        req.getRoomName(),
                        user.getProfile().getGender(),
                        req.getRegion().getSido(),
                        req.getRegion().getSigungu(),
                        req.getAgeRange().getMin(),
                        req.getAgeRange().getMax(),
                        req.getMaxMembers(),
                        inviteCode
                );
                Meeting savedMeeting = meetingRepository.save(meeting);
                log.info("미팅 방 생성 완료- Meeting ID : {}", savedMeeting.getId());

                meetingMemberService.addLeader(savedMeeting, user);
                Long chatRoomId = chatRoomService.createMeetingChatRoom(savedMeeting);
                log.info("미팅 채팅방 생성 완료- ChatRoom ID : {}", chatRoomId);

                return buildResponseForCreate(savedMeeting, chatRoomId, true);
            } catch (DataIntegrityViolationException e) {
                log.warn("중복 미팅방 코드 발생, 재시도 attempt={}/{}", attempt, CODE_RETRY_LIMIT);
            }

        }
        throw new IllegalArgumentException("미팅방 생성 실패: 초대 코드 생성에 실패했습니다. 잠시 후 다시 시도해주세요");

    }

    public void validateCreateValidation(Long userId){
        /*결제 조건*/


        /*현재 참여중인 미팅 유무*/
        if(meetingMemberService.isActivate(userId).isPresent())
            throw new IllegalArgumentException("이미 미팅 참여중입니다.");
    }
    public MeetingChatRoomInfo buildBaseChatRoomInfo(Meeting meeting, Long chatRoomId, boolean isLeader, List<MeetingChatRoomInfo.TeamMember> teamMembers) {

        MeetingInfo meetingInfo = MeetingInfo.of(meeting);

        return MeetingChatRoomInfo.of(
                chatRoomId,
                meetingInfo,
                isLeader,
                teamMembers
        );
    }

    /*미팅 방생성응답 생성*/
    public MeetingRoomCreateDataDto buildResponseForCreate(Meeting meeting, Long chatRoomId, boolean isLeader) {
        MeetingRoomCreateDataDto data =
                MeetingRoomCreateDataDto.of(
                        buildBaseChatRoomInfo(meeting, chatRoomId, isLeader, List.of())
                );
        return data;

    }



    /*미팅 방 입장 응답 dto 생성*/
    public MeetingRoomCreateDataDto buildResponseForEnter(Meeting meeting, Long chatRoomId, boolean isLeader) {
        List<MeetingChatRoomInfo.TeamMember> teamMembers = getActiveTeamMember(meeting);

        MeetingRoomCreateDataDto data =
                MeetingRoomCreateDataDto.of(
                        buildBaseChatRoomInfo(meeting, chatRoomId, isLeader, teamMembers)
                );

        return data;

    }

    /*미팅방 상태 응답 dto 생성*/
    public MyMeetingStatusDataDto buildResponseDtoForCheck(Meeting meeting, Long chatRoomId, boolean isLeader) {
        List<MeetingChatRoomInfo.TeamMember> teamMembers = getActiveTeamMember(meeting);

        return MyMeetingStatusDataDto.builder()
                .meetingChatRoomInfo(buildBaseChatRoomInfo(meeting, chatRoomId, isLeader, teamMembers))
                .hasActiveRoom(true)
                .build();

    }

    public List<MeetingChatRoomInfo.TeamMember> getActiveTeamMember(Meeting meeting) {
        List<MeetingMember> members = meeting.getActiveMembers();
        return members.stream().map(mm -> MeetingChatRoomInfo.TeamMember.of(
                mm.getUser(),
                mm.getRole() == Role.LEADER
        )).toList();
    }

    /*미팅 초대 코드 생성*/
    public String generateInviteCode() {

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            int randomIndex = random.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(randomIndex));
        }

        return sb.toString();
    }

    public void validateAge(MeetingRoomCreateRequest req) {
        if (req.getAgeRange().getMin() > req.getAgeRange().getMax())
            throw new IllegalArgumentException("미팅방 생성: 나이 범위가 올바르지 않습니다.");
    }

    /*미팅 초대 코드로 입장*/
    @Transactional
    public MeetingRoomCreateDataDto enterRoomByCode(MeetingRoomJoinByCodeRequest request, Long userId) {
        User user = getUserEntity(userId);
        Meeting meeting = meetingRepository.findByCodeWithLock(request.getRoomCode())
                .orElseThrow(() -> new IllegalArgumentException("방 코드로 미팅방 입장: 존재 하지 않는 초대 코드입니다."));

        validateJoinCondition(meeting, user);

        meeting.addMember();
        meetingRepository.saveAndFlush(meeting); /*왜 addMember가 반영이 안되지*/
        meetingMemberService.addMember(meeting, user);
        Long chatRoomId = chatRoomService.joinChatRoom(meeting, user);

        return buildResponseForEnter(meeting, chatRoomId, false);

    }


    /*미팅방 입장(빠른 매칭)*/
    @Transactional
    public MeetingRoomCreateDataDto enterRoomById(MeetingRoomJoinRequest request, Long userId) {

        Meeting meeting = meetingRepository.findByIdWithLock(request.getMeetingId())
                    .orElseThrow(() -> new IllegalArgumentException("미팅방 입장: 존재 하지 않는 id입니다. :" + request.getMeetingId()));

        User user = getUserEntity(userId);
        validateJoinCondition(meeting, user);
        meeting.addMember();
        meetingMemberService.addMember(meeting, user);
        Long chatRoomId = chatRoomService.joinChatRoom(meeting, user);

        return buildResponseForEnter(meeting, chatRoomId, false);
    }


    /*입장 가능 조건 확인*/
    private void validateJoinCondition(Meeting meeting, User user) {
        Profile profile = user.getProfile();

        /*결제 조건 확인*/

        /*매칭 상태*/
        if (meeting.getMatchingStatus() != MatchingStatus.RECRUITING)
            throw new IllegalArgumentException("정원이 다 찬 미팅방입니다.");


        /*이미 참여중인지 체크*/
        if (meetingMemberService.isActivate(user.getUserId()).isPresent())
            throw new IllegalArgumentException("이미 참여 중인 미팅방이 있어 입장할 수 없습니다.");
        /*내가 이미 추방당한 방인지*/
        if(meetingMemberService.isKicked(meeting.getId(), user.getUserId()))
            throw new IllegalArgumentException("해당 방에서 추방당했으므로 다시 참여하지 못합니다.");
        /*성별 체크*/
        if (!meeting.getGender().equals(profile.getGender()))
            throw new IllegalArgumentException("잘못된 성별 미팅방이어서 입장할 수 없습니다.");

        /*정원 체크*/
        if (meeting.getMaxMembers() <= meeting.getCurrentMembers() || meeting.getMatchingStatus() == MatchingStatus.FULL) {
            throw new IllegalArgumentException("이미 정원이 다 찬 미팅방입니다.");
        }
    }

    public MyMeetingStatusDataDto checkUserMeetingStatus(Long userId) {
        User user = getUserEntity(userId);
        Optional<MeetingMember> mm = meetingMemberService.isActivate(user.getUserId());
        if (mm.isEmpty()) {
            return MyMeetingStatusDataDto.builder()
                    .hasActiveRoom(false)
                    .meetingChatRoomInfo(null)
                    .build();
        } else {
            MeetingMember meetingMember = mm.get();
            Meeting meeting = meetingMember.getMeeting();
            Long chatRoomId = chatRoomService.getChatRoomId(meeting);
            return buildResponseDtoForCheck(meeting, chatRoomId, meetingMember.getRole() == Role.LEADER);
        }
    }


    /*팀원 프로필 상세 조회*/
    public MemberProfilesDto getTeamMemberProfilesDetail(Long meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("미팅 리스트에서 팀원 프로필 조회: 존재 하지 않는 미팅 아이디 입니다."));

        List<MeetingMember> members = meeting.getActiveMembers();


        return MemberProfilesDto.of(members);


    }

    /*조건별 방 리스트 조회*/
    public MeetingPage<MeetingRoomSearchItemDto> getMeetingList(MeetingRoomsSearchRequest req, int pageSize, String cursorToken){
        String sido = req.getRegion().getSido();
        String sigungu = req.getRegion().getSigungu();
        CursorCodec.MeetingToken token = cursorCodec.decodeAndVerify(cursorToken,sido, sigungu);

        int bucketIndex = token== null ? 0: token.bucketIndex();
        MeetingListCursor cursor = token== null ? null : new MeetingListCursor(
                token.score(),
                token.createdAt(),
                token.id()
        );

        int target = pageSize +1;
        List<Tagged<Meeting>> acc = new ArrayList<>(target);
        while(acc.size()< target && bucketIndex <=MeetingBucket.maxIndex()) {
            int need = target - acc.size();

            MeetingBucket bucket = MeetingBucket.fromIndex(bucketIndex);
            List<Meeting> part = meetingRepository.fetchBucketPage(req, bucket, cursor, need);

            for (Meeting m : part) acc.add(new Tagged<>(bucketIndex, m));

            if (part.size() < need) {
                bucketIndex++;
                cursor = null;
            }
        }
            boolean hasNext = acc.size() > pageSize;
            List<Tagged<Meeting>> page = hasNext ? acc.subList(0,pageSize) : acc;

            String next = null;
            if(hasNext && !page.isEmpty()){
                Tagged<Meeting> last = page.get(page.size()-1);
                next = cursorCodec.encode(buildNextToken(last.bucketIndex(),last.value(),req.getRegion().getSido(),req.getRegion().getSigungu()));
            }

        List<MeetingRoomSearchItemDto> rooms = page.stream()
                .map(m -> {
                            MeetingInfo info = MeetingInfo.ofSummary(m.value());

                            List<TeamMemberPreviewDto> previewDtos = m.value().getActiveMembers().stream()
                                    .map(mm -> TeamMemberPreviewDto.of(mm.getUser())
                                    ).toList();

                            return MeetingRoomSearchItemDto.builder()
                                    .meetingInfo(info)
                                    .membersPreview(previewDtos)
                                    .build();


                        }
                ).toList();

            return new MeetingPage<>(next, hasNext,rooms);
        }



    private CursorCodec.MeetingToken buildNextToken(int bucketIndex, Meeting m, String sido, String sigungu){
        MeetingBucket bucket = MeetingBucket.fromIndex(bucketIndex);

        Integer score = null;
        if(bucket.isScoreOrdered()){
            int max = m.getMaxMembers();
            int cur = m.getCurrentMembers();
            score = (max<=0) ? 0 : (cur*10000)/max;
        }

        return new CursorCodec.MeetingToken(bucketIndex,sido,sigungu,score,m.getCreatedAt(),m.getId());
    }


    private record Tagged<T>(int bucketIndex, T value){}
}
