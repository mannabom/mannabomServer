package mannabom_server.manabom.infrastructure.security.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.infrastructure.security.jwt.JwtUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Component
@RequiredArgsConstructor
@Slf4j
public class StompAuthChannelInterceptor implements ChannelInterceptor {
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    private static final Pattern ROOM_DESTINATION_PATTERN = Pattern.compile("^/topic/rooms/(\\d+).*");
    private static final String USER_LOCATION_PREFIX = "user:location:";

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor acc = StompHeaderAccessor.wrap(message);
        StompCommand command = acc.getCommand();

        //웹 소켓 연결 시 인증
        if (StompCommand.CONNECT.equals(command)) {
            String auth = acc.getFirstNativeHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                log.error("웹소켓 연결: access 토큰이 존재하지 않거나 Bearer로 시작 안함");
                throw new IllegalArgumentException("토큰 없음");
            }
            String token = auth.substring(7);
            if(!StringUtils.hasText(token)) {
                log.error("웹소켓 연결: Bearer뒤에 access 토큰이 없음");
                throw new IllegalArgumentException("Empty token");
            }
            if(jwtUtil.validateToken(token)){
                Long userId = jwtUtil.getUserIdFromToken(token);
                acc.setUser(new UserPrincipal(userId));
            }
        }

        //방 입장(웹소켓 구독)은 해당 방에 존재하는지
        if(StompCommand.SUBSCRIBE.equals(command)){
            Principal principal = acc.getUser();
            if(principal!=null){
                String userId = principal.getName();
                String dest = acc.getDestination();
                Long roomId = parseRoomId(dest);

                redisTemplate.opsForValue().set(USER_LOCATION_PREFIX+userId,String.valueOf(roomId));
                log.debug("웹소켓 구독: 유저 {} 가 방 {} 에 입장하여 Redis에 위치를 기록했습니다.", userId, roomId);
            }

        }


        if(StompCommand.DISCONNECT.equals(command)){
            Principal principal = acc.getUser();
            if(principal!=null){
                String userId = principal.getName();
                redisTemplate.delete(USER_LOCATION_PREFIX+userId);
                log.debug("유저 {} 접속 종료로 Redis 위치 정보를 삭제합니다.", userId);
            }
        }



        return message;
    }


    public record UserPrincipal(Long userId) implements Principal {
        @Override
        public String getName(){
            return String.valueOf(userId);
        }
    }


    public Long parseRoomId(String destination){
        if(destination==null) throw new IllegalArgumentException("웹소켓 구독: 잘못된 destination 형식");

        Matcher m = ROOM_DESTINATION_PATTERN.matcher(destination);
        if(!m.matches()){
            log.error("웹소켓 구독: 잘못된 destination 형식, destination={}",destination);
            throw new IllegalArgumentException("웹소켓 구독: 잘못된 destination 형식"+destination);
        }
        return Long.parseLong(m.group(1));

    }
}

