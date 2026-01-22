package mannabom_server.manabom.domain.meeting.repository;


import com.querydsl.core.BooleanBuilder;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.meeting.dto.common.MeetingListCursor;
import mannabom_server.manabom.application.meeting.dto.request.MeetingRoomsSearchRequest;
import mannabom_server.manabom.domain.meeting.entity.Meeting;
import mannabom_server.manabom.domain.meeting.entity.QMeeting;
import mannabom_server.manabom.domain.meeting.entity.QMeetingMember;
import mannabom_server.manabom.domain.meeting.enums.MatchingStatus;
import mannabom_server.manabom.domain.meeting.enums.MeetingBucket;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
@RequiredArgsConstructor
public class MeetingRepositoryCustomImpl implements  MeetingRepositoryCustom{

    private final JPAQueryFactory factory;


    @Override
    public List<Meeting> fetchBucketPage(MeetingRoomsSearchRequest request, MeetingBucket meetingBucket, MeetingListCursor cursor, int limit) {
        QMeeting meeting = QMeeting.meeting;
        BooleanBuilder where = new BooleanBuilder();

        applyCommonFilters(where,meeting,request);

        switch(meetingBucket){
            case FAST_1 -> {
                where.and(meeting.matchingStatus.eq(MatchingStatus.FASTMATCHING));
                where.and(exactRegion(meeting,request.getRegion().getSido(), request.getRegion().getSigungu()));
            }
            case FAST_2 -> {
                where.and(meeting.matchingStatus.eq(MatchingStatus.FASTMATCHING));
                where.and(matchSameSidoDiffSigungu(meeting,request.getRegion().getSido(), request.getRegion().getSigungu()));
            }
            case REC_1 -> {
                where.and(meeting.matchingStatus.eq(MatchingStatus.RECRUITING));
                where.and(exactRegion(meeting,request.getRegion().getSido(), request.getRegion().getSigungu()));
            }
            case REC_2 -> {
                where.and(meeting.matchingStatus.eq(MatchingStatus.RECRUITING));
                where.and(matchSameSidoDiffSigungu(meeting,request.getRegion().getSido(), request.getRegion().getSigungu()));

            }
            case REC_3 -> {
                where.and(meeting.matchingStatus.eq(MatchingStatus.RECRUITING));
                where.and(meeting.regionSido.ne(request.getRegion().getSido()));
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
        return meeting.regionSido.eq(sido).and(meeting.regionSigungu.eq(sigungu));
    }

    private BooleanExpression matchSameSidoDiffSigungu(QMeeting meeting, String sido, String sigungu){
        return meeting.regionSido.eq(sido).and(meeting.regionSigungu.ne(sigungu));
    }

    private OrderSpecifier<?>[] orderByScoreOldest(QMeeting meeting){
        return new OrderSpecifier[]{
                occupancyScore(meeting).desc(),
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
