package mannabom_server.manabom.global.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidCursorException extends  RuntimeException {
    public InvalidCursorException(String message){
        super(message);
    }
}
