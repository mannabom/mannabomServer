package mannabom_server.manabom.domain.meeting.repository;


import com.querydsl.core.BooleanBuilder;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.meeting.dto.common.MeetingListCursor;
import mannabom_server.manabom.application.meeting.dto.request.MeetingRoomsSearchRequest;
import mannabom_server.manabom.domain.meeting.entity.Meeting;
import mannabom_server.manabom.domain.meeting.entity.QMeeting;
import mannabom_server.manabom.domain.meeting.entity.QMeetingMember;
import mannabom_server.manabom.domain.meeting.enums.MeetingStatus;
import mannabom_server.manabom.domain.meeting.enums.MeetingBucket;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
@RequiredArgsConstructor
public class MeetingRepositoryCustomImpl implements  MeetingRepositoryCustom{

    private final JPAQueryFactory factory;
    private static final List<String> METROPOLITAN_CITIES = List.of(
            "서울특별시","대전광역시","부산광역시","대구광역시","인천광역시","광주광역시","울산광역시","제주특별자치도"
    );

    @Override
    public List<Meeting> fetchBucketPage(MeetingRoomsSearchRequest request, MeetingBucket meetingBucket, MeetingListCursor cursor, int limit) {
        QMeeting meeting = QMeeting.meeting;
        BooleanBuilder where = new BooleanBuilder();

        applyCommonFilters(where,meeting,request);

        String sido = request.getRegion().getSido();
        String sigungu = request.getRegion().getSigungu();

        boolean isAnySigungu = "상관없음".equals(sigungu) || "전체".equals(sigungu);
        boolean isMetro = METROPOLITAN_CITIES.contains(sido);

        switch(meetingBucket){
            case FAST_1 -> {
                where.and(meeting.meetingStatus.eq(MeetingStatus.FASTMATCHING));
                if(isAnySigungu){
                    where.and(meeting.region.sidoName.eq(sido));
                } else
                    where.and(exactRegion(meeting,sido, sigungu));
            }
            case FAST_2 -> {
                if(isAnySigungu||!isMetro){
                    return List.of();
                }
                where.and(meeting.meetingStatus.eq(MeetingStatus.FASTMATCHING));
                where.and(matchSameSidoDiffSigungu(meeting,sido, sigungu));
            }
            case REC_1 -> {
                where.and(meeting.meetingStatus.eq(MeetingStatus.RECRUITING));
                if(isAnySigungu)
                    where.and(meeting.region.sidoName.eq(sido));
                else
                    where.and(exactRegion(meeting,sido, sigungu));
            }
            case REC_2 -> {
                if(isAnySigungu)
                    return List.of();

                where.and(meeting.meetingStatus.eq(MeetingStatus.RECRUITING));
                where.and(matchSameSidoDiffSigungu(meeting,sido, sigungu));
            }
            case REC_3 -> {
                where.and(meeting.meetingStatus.eq(MeetingStatus.RECRUITING));
                where.and(meeting.region.sidoName.ne(sido));
            }
        }

        BooleanExpression after = (cursor==null)? null : afterCursor(meeting,meetingBucket,cursor);
        if(after!=null)
            where.and(after);

        OrderSpecifier<?>[] order = meetingBucket.isScoreOrdered() ?
                orderByScoreOldest(meeting) :
                orderByOldest(meeting);

        return factory
                .selectFrom(meeting)
                .where(where)
                .orderBy(order)
                .limit(limit)
                .fetch();
    }

    private void applyCommonFilters(BooleanBuilder where, QMeeting meeting, MeetingRoomsSearchRequest request){
        where.and(meeting.maxMembers.gt(0));

        if(request.getMemberCounts() !=null && !request.getMemberCounts().isEmpty()){
            where.and(meeting.maxMembers.in(request.getMemberCounts()));
        }

        if(request.getAgeRange().getMin()!=null)
            where.and(meeting.maxAge.goe(request.getAgeRange().getMin()));

        if(request.getAgeRange().getMax()!=null)
            where.and(meeting.minAge.loe(request.getAgeRange().getMax()));
    }


    private BooleanExpression exactRegion(QMeeting meeting, String sido, String sigungu){
        return meeting.region.sidoName.eq(sido).and(
                meeting.region.sigunguName.in(sigungu,"전체")
        );
    }

    private BooleanExpression matchSameSidoDiffSigungu(QMeeting meeting, String sido, String sigungu){
        return meeting.region.sidoName.eq(sido).and(
                meeting.region.sigunguName.notIn(sigungu,"전체")
                );
    }

    private OrderSpecifier<?>[] orderByScoreOldest(QMeeting meeting){
        return new OrderSpecifier[]{
                meeting.occupancyScore.desc(),
                meeting.createdAt.asc(),
                meeting.id.asc()
        };
    }
    private OrderSpecifier<?>[] orderByOldest(QMeeting meeting){
        return new OrderSpecifier[]{
                meeting.createdAt.asc(),
                meeting.id.asc()
        };
    }


    private NumberExpression<Integer> occupancyScore(QMeeting meeting){
        //매개변수가 결과타입, 공식, 0번, 1번
        return meeting.occupancyScore.coalesce(0);
    }


    private BooleanExpression afterCursor(QMeeting meeting, MeetingBucket bucket, MeetingListCursor cursor){
        if(bucket.isScoreOrdered()){
            var score = occupancyScore(meeting);

            BooleanExpression scoreEq = score.eq(cursor.score());
            return score.lt(cursor.score())
                    .or(scoreEq.and(meeting.createdAt.gt(cursor.createdAt())))
                    .or(scoreEq.and(meeting.createdAt.eq(cursor.createdAt()).and(meeting.id.gt(cursor.id()))));
        }
        return meeting.createdAt.gt(cursor.createdAt()).or(meeting.createdAt.eq(cursor.createdAt()).and(meeting.id.gt(cursor.id())));
    }
}
