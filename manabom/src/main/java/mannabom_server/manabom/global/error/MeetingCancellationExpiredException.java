package mannabom_server.manabom.global.error;

public class MeetingCancellationExpiredException extends  IllegalStateException{
    public MeetingCancellationExpiredException(String message) {
        super(message);
    }
}
