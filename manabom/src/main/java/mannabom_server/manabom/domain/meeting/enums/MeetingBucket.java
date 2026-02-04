package mannabom_server.manabom.domain.meeting.enums;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum MeetingBucket {
    FAST_1(true),
    FAST_2(true),
    REC_1(true),
    REC_2(true),
    REC_3(false);

    private final boolean scoreOrdered;

    public int index(){
        return ordinal();
    }
    public static int maxIndex(){
        return values().length-1;
    }

    public static MeetingBucket fromIndex(int idx){
        return values()[idx];
    }

    public boolean isScoreOrdered(){
        return scoreOrdered;
    }




}

