package a75f.io.api.haystack.sync;

import androidx.annotation.NonNull;

public class ErrorResponse {
    private String message;
    private ErrorCode errorCode;

    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
    public void setErrorCode(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    @NonNull
    @Override
    public String toString() {
        return "ErrorResponse {" +
                "message='" + message + '\'' +
                ", errorCode=" + errorCode +
                '}';
    }

}
