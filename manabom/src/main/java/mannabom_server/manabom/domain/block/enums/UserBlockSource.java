package mannabom_server.manabom.domain.block.enums;

/**
 * 차단 경로. 신고와 함께 차단한 경우를 따로 집계하기 위해 남긴다.
 */
public enum UserBlockSource {
    DIRECT,
    REPORT
}
