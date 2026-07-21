package mannabom_server.manabom.application.meeting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.chat.service.ChatRoomService;
import mannabom_server.manabom.application.meeting.dto.common.*;
import mannabom_server.manabom.application.meeting.dto.request.*;
import mannabom_server.manabom.application.meeting.dto.response.*;
import mannabom_server.manabom.application.region.service.RegionService;
import mannabom_server.manabom.domain.meeting.entity.Meeting;
import mannabom_server.manabom.domain.meeting.entity.MeetingMember;
import mannabom_server.manabom.domain.meeting.enums.MeetingStatus;
import mannabom_server.manabom.domain.meeting.enums.MeetingBucket;
import mannabom_server.manabom.domain.meeting.enums.MeetingRole;
import mannabom_server.manabom.domain.meeting.repository.MeetingRepository;
import mannabom_server.manabom.domain.region.entity.Region;
import mannabom_server.manabom.domain.user.entity.Profile;
import mannabom_server.manabom.domain.user.entity.User;
import mannabom_server.manabom.domain.user.repository.ProfileRepository;
import mannabom_server.manabom.domain.user.repository.UserRepository;
import mannabom_server.manabom.infrastructure.security.codec.CursorCodec;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ProfileRepository profileRepository;


    private final MeetingMemberReadService meetingMemberReadService;
    private final ChatRoomService chatRoomService;
    private final MeetingMemberService meetingMemberService;
    private final MeetingCancellationService meetingCancellationService;
    private final RegionService regionService;

    private final CursorCodec cursorCodec;
    private final ApplicationEventPublisher eventPublisher;

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
        Profile profile = profileRepository.findByUser(user)
                .orElseThrow(()-> new IllegalArgumentException("사용자의 프로필을 찾을 수 없습니다."));
        for (int attempt = 1; attempt <= CODE_RETRY_LIMIT; attempt++) {
            String inviteCode = generateInviteCode();
            try {
                Meeting meeting = Meeting.create(
                        user,
                        req.getRoomName(),
                        profile.getGender(),
                        regionService.resolveRegion(req.getRegion().getSido(),req.getRegion().getSigungu()),
                        req.getAgeRange().getMin(),
                        req.getAgeRange().getMax(),
                        req.getMaxMembers(),
                        inviteCode,
                        profile.computeAge()
                );
                Meeting savedMeeting = meetingRepository.save(meeting);
                log.info("미팅 방 생성 완료- Meeting ID : {}", savedMeeting.getId());

                meetingMemberService.addLeader(savedMeeting, user);
                Long chatRoomId = chatRoomService.createMeetingChatRoom(savedMeeting, user);
                log.info("미팅 채팅방 생성 완료- ChatRoom ID : {}", chatRoomId);

                return buildResponseForCreate(savedMeeting, chatRoomId);
            } catch (DataIntegrityViolationException e) {
                log.warn("중복 미팅방 코드 발생, 재시도 attempt={}/{}", attempt, CODE_RETRY_LIMIT);
            }

        }
        throw new IllegalArgumentException("미팅방 생성 실패: 초대 코드 생성에 실패했습니다. 잠시 후 다시 시도해주세요");

    }

    private void validateCreateValidation(Long userId){
        /*결제 조건*/


        /*현재 참여중인 미팅 유무*/
        if(meetingMemberService.isActivate(userId).isPresent())
            throw new IllegalArgumentException("이미 미팅 참여중입니다.");
    }
    private MeetingChatRoomInfo buildBaseChatRoomInfo(Meeting meeting, Long chatRoomId, boolean isLeader, List<MeetingChatRoomInfo.TeamMember> teamMembers) {

        MeetingInfo meetingInfo = MeetingInfo.of(meeting);

        return MeetingChatRoomInfo.of(
                chatRoomId,
                meetingInfo,
                isLeader,
                teamMembers
        );
    }

    /*미팅 방생성응답 생성*/
    private MeetingRoomCreateDataDto buildResponseForCreate(Meeting meeting, Long chatRoomId) {
        return MeetingRoomCreateDataDto.of(
                buildBaseChatRoomInfo(meeting, chatRoomId, true, List.of())
        );

    }



    /*미팅 방 입장 응답 dto 생성*/
    private MeetingRoomCreateDataDto buildResponseForEnter(Meeting meeting, Long chatRoomId) {
        List<MeetingChatRoomInfo.TeamMember> teamMembers = meetingMemberReadService.getActiveTeamMembers(meeting.getId());

        return MeetingRoomCreateDataDto.of(
                buildBaseChatRoomInfo(meeting, chatRoomId, false, teamMembers)
        );

    }

    /*미팅방 상태 응답 dto 생성*/
    private MyMeetingStatusDataDto buildResponseDtoForCheck(Meeting meeting, Long chatRoomId, boolean isLeader) {
        List<MeetingChatRoomInfo.TeamMember> teamMembers = meetingMemberReadService.getActiveTeamMembers(meeting.getId());

        return MyMeetingStatusDataDto.builder()
                .meetingChatRoomInfo(buildBaseChatRoomInfo(meeting, chatRoomId, isLeader, teamMembers))
                .hasActiveRoom(true)
                .build();

    }


    /*미팅 초대 코드 생성*/
    private String generateInviteCode() {

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            int randomIndex = random.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(randomIndex));
        }

        return sb.toString();
    }

    private void validateAge(MeetingRoomCreateRequest req) {
        if (req.getAgeRange().getMin() > req.getAgeRange().getMax())
            throw new IllegalArgumentException("미팅방 생성: 나이 범위가 올바르지 않습니다.");
    }

    /*미팅 초대 코드로 입장*/
    @Transactional
    public MeetingRoomCreateDataDto enterRoomByCode(MeetingRoomJoinByCodeRequest request, Long userId) {
        User user = getUserEntity(userId);
        Profile profile = profileRepository.findByUser(user)
                .orElseThrow(()-> new IllegalArgumentException("방 코드로 미팅방 입장: 사용자의 프로필을 찾을 수 없습니다."));

        Meeting meeting = meetingRepository.findByCodeWithLock(request.getRoomCode())
                .orElseThrow(() -> new IllegalArgumentException("방 코드로 미팅방 입장: 존재 하지 않는 초대 코드입니다."));

        meetingCancellationService.validateNoPendingCancellation(meeting.getId());
        validateJoinCondition(meeting, user,profile);
        meeting.addMember(profile.computeAge());
        meetingRepository.saveAndFlush(meeting); /*왜 addMember가 반영이 안되지*/
        meetingMemberService.addMember(meeting, user);
        Long chatRoomId = chatRoomService.joinChatRoom(meeting, user);

        return buildResponseForEnter(meeting, chatRoomId);

    }


    /*미팅방 입장(빠른 매칭)*/
    @Transactional
    public MeetingRoomCreateDataDto enterRoomById(Long meetingId, Long userId) {
        User user = getUserEntity(userId);
        Profile profile = profileRepository.findByUser(user)
                .orElseThrow(()-> new IllegalArgumentException("방 코드로 미팅방 입장: 사용자의 프로필을 찾을 수 없습니다."));
        Meeting meeting = meetingRepository.findByIdWithLock(meetingId)
                    .orElseThrow(() -> new IllegalArgumentException("미팅방 입장: 존재 하지 않는 id입니다. :" + meetingId));


        meetingCancellationService.validateNoPendingCancellation(meetingId);
        validateJoinCondition(meeting, user,profile);
        meeting.addMember(profile.computeAge());
        meetingMemberService.addMember(meeting, user);
        Long chatRoomId = chatRoomService.joinChatRoom(meeting, user);

        return buildResponseForEnter(meeting, chatRoomId);
    }


    /*입장 가능 조건 확인*/
    private void validateJoinCondition(Meeting meeting, User user,Profile profile) {

        /*결제 조건 확인*/

        /*매칭 상태*/
        MeetingStatus status = meeting.getMeetingStatus();
        if (status != MeetingStatus.RECRUITING
                && status != MeetingStatus.FASTMATCHING) {
            throw new IllegalArgumentException("현재 입장할 수 없는 미팅방입니다.");
        }


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
        if (meeting.getMaxMembers() <= meeting.getCurrentMembers() || meeting.getMeetingStatus() == MeetingStatus.FULL) {
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
            return buildResponseDtoForCheck(meeting, chatRoomId, meetingMember.getMeetingRole() == MeetingRole.LEADER);
        }
    }


    /*팀원 프로필 상세 조회*/
    public TeamMemberProfilesDto getTeamMemberProfilesDetail(Long meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("미팅 리스트에서 팀원 프로필 조회: 존재 하지 않는 미팅 아이디 입니다."));

        List<TeamMemberProfilesDto.TeamMemberDetailDto> members = meetingMemberReadService.getActiveTeammMemberDetails(meetingId);
        return TeamMemberProfilesDto.of(members);
    }

    /*조건별 방 리스트 조회*/
    public MeetingPage<MeetingRoomSearchItemDto> getMeetingList(MeetingRoomsSearchRequest req, int pageSize, String cursorToken){
        Region region = regionService.resolveRegion(req.getRegion().getSido(), req.getRegion().getSigungu());
        CursorCodec.MeetingToken token = cursorCodec.decodeAndVerify(cursorToken,region.getSidoCode(), region.getSigunguCode());

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
                next = cursorCodec.encode(buildNextToken(last.bucketIndex(),last.value(), region.getSidoCode(), region.getSigunguCode()));
            }

        List<MeetingRoomSearchItemDto> rooms = page.stream()
                .map(m -> {
                            MeetingInfo info = MeetingInfo.ofSummary(m.value());
                            List<TeamMemberPreviewDto> previewDtos = meetingMemberReadService.getActiveTeamMemberPreivews(m.value.getId());
                            return MeetingRoomSearchItemDto.builder()
                                    .meetingInfo(info)
                                    .membersPreview(previewDtos)
                                    .build();
                        }
                ).toList();

            return new MeetingPage<>(next, hasNext,rooms);
        }



    private CursorCodec.MeetingToken buildNextToken(int bucketIndex, Meeting m, String sidoCode, String sigunguCode){
        MeetingBucket bucket = MeetingBucket.fromIndex(bucketIndex);

        Integer score = null;
        if(bucket.isScoreOrdered()){
            int max = m.getMaxMembers();
            int cur = m.getCurrentMembers();
            score = (max<=0) ? 0 : (cur*10000)/max;
        }

        return new CursorCodec.MeetingToken(bucketIndex,sidoCode,sigunguCode,score,m.getCreatedAt(),m.getId());
    }


    private record Tagged<T>(int bucketIndex, T value){}

    @Transactional
    public MatchingStartDataDto startMatching( Long userId){
        Meeting meeting = meetingMemberService.findMyLeadingMeeting(userId)
                .orElseThrow(()-> new IllegalArgumentException("매칭 시작: 리더만이 매칭을 시작할 수 있습니다. "+ userId));

        meeting.startMatching();
        meetingRepository.saveAndFlush(meeting);


        //비동기 이벤트 발행
        MeetingMatchingEvent event = MeetingMatchingEvent.from(meeting);
        eventPublisher.publishEvent(event);


        return MatchingStartDataDto.builder()
                .matchingStarted(true)
                .estimatedWaitTime(null)
                .build();
    }

    @Transactional
    public boolean cancelMatching(Long userId){
        Meeting meeting = meetingMemberService.findMyLeadingMeeting(userId)
                .orElseThrow(()-> new IllegalArgumentException("매칭 취소: 리더만이 매칭을 취소할 수 있습니다. "+ userId));

        meeting.cancelMatching();
        return true;
    }

    @Transactional
    public void handleMemberLeave(Long meetingId, Long userId){
        Meeting meeting = meetingRepository.findByIdWithLock(meetingId)
                .orElseThrow(()-> new IllegalArgumentException("존재하지 않는 미팅아이디입니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));
        Profile profile = profileRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("프로필 정보를 찾을 수 없습니다."));

        meetingCancellationService.validateNoPendingCancellation(meetingId);
        validateMeetingStatus(meeting);
        MeetingStatus previousStatus = meeting.getMeetingStatus();
        boolean isLeader = meetingMemberService.isLeader(meetingId, userId);

        meeting.deleteMember(profile.computeAge());
        meetingMemberService.deactivateStatus(meetingId,userId);

        List<MeetingMember> activeMembers = meetingMemberService.getActiveMembers(meetingId);
        if(activeMembers.isEmpty()){
            meeting.delete();
            return;
        }

        if (previousStatus == MeetingStatus.MATCHED) {
            meeting.changeToFastMatchingAfterMemberLeave();
        } else if (previousStatus == MeetingStatus.FULL) {
            meeting.changeToRecruitingAfterMemberLeave();
        }

        if(isLeader){
            meetingMemberService.appointNextLeader(meetingId);
        }
    }
    private void validateMeetingStatus(Meeting meeting){
        MeetingStatus status = meeting.getMeetingStatus();
        if(status== MeetingStatus.MATCHING_PENDING || status==MeetingStatus.MATCHING_WAITING){
            throw new IllegalStateException("매칭이 진행중이므로 나갈 수 없습니다.");
        }
    }





}
