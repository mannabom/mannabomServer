package mannabom_server.manabom.infrastructure.security.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.domain.meeting.enums.MeetingBucket;
import mannabom_server.manabom.global.error.InvalidCursorException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class CursorCodec {
    private final ObjectMapper objectMapper;

    private final Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
    private final Base64.Decoder dec = Base64.getUrlDecoder();

    @Value("meeting.cursor.secret")
    private String secret;

    private static final String HMAC_ALGO = "HmacSHA256";


    public String encode(MeetingToken token){
        try{
            byte[] payloadJson = objectMapper.writeValueAsBytes(token);
            String payloadB64 = enc.encodeToString(payloadJson);

            byte[] sig = hmac(payloadB64.getBytes(StandardCharsets.UTF_8));
            String sigB64 = enc.encodeToString(sig);

            return payloadB64+"."+sigB64;
        } catch (Exception e) {
            throw new IllegalStateException("cursor encode Failed",e);
        }
    }
    public MeetingToken decodeAndVerify(String cursorToken, String expectedSidoCode, String expectedSigunguCode){
        if(cursorToken==null || cursorToken.isBlank()) return null;
        try{
            String[] parts = cursorToken.split("\\.");
            String payloadB64 = parts[0];


            byte[] expectedSig = hmac(payloadB64.getBytes(StandardCharsets.UTF_8));
            byte[] sig = dec.decode(parts[1]);

            if(!MessageDigest.isEqual(expectedSig,sig))
                throw new InvalidCursorException("커서의 시그니처가 유효하지 않습니다.");


            byte[] payloadJson = dec.decode(payloadB64);
            MeetingToken token = objectMapper.readValue(payloadJson, MeetingToken.class);

            if(token.bucketIndex() < 0 || token.bucketIndex() > MeetingBucket.maxIndex())
                throw new InvalidCursorException("커서의 버킷 인덱스가 범위를 벗어났습니다.");

            if(token.createdAt() == null || token.id() == null){
                throw new InvalidCursorException("누락된 커서 정보들이 있습니다.");
            }
            MeetingBucket bucket = MeetingBucket.fromIndex(token.bucketIndex());

            if(bucket.isScoreOrdered() && token.score()==null){
                throw new InvalidCursorException("커서에 점수 정보가 누락되었습니다.");
            }

            if(token.sidoCode()==null || !token.sidoCode().equals(expectedSidoCode))
                throw new InvalidCursorException("커서에 시도 정보가 누락되었거나 일치하지않습니다.");

            if(token.sigunguCode()==null || !token.sigunguCode().equals(expectedSigunguCode))
                throw new InvalidCursorException("커서에 시군구 정보가 누락되었거나 일치하지 않습니다.");

            return token;

        }catch (InvalidCursorException e){ throw e;}
        catch (Exception e) {
            throw new InvalidCursorException("cursor decode failed"+e);
        }
    }
    private byte[] hmac(byte[] data){
        try{
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),HMAC_ALGO));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC failed",e);
        }
    }

    public record MeetingToken(
            int bucketIndex,
            String sidoCode,
            String sigunguCode,
            Integer score,
            Instant createdAt,
            Long id
    ){}

}
