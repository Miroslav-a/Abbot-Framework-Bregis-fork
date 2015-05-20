package abbot.editor.recorder;

import java.lang.annotation.Documented;

public class RecordingFailedException extends RuntimeException {
    public RecordingFailedException(String msg, Throwable cause) {
        super(msg, cause);
    }
    public RecordingFailedException(Throwable thr) {
        super(thr.getMessage(), thr);
    }
    
    @Deprecated()
    public Throwable getReason() {
        return getCause();
    }
}
