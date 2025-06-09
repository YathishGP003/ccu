package a75f.io.api.haystack.sync;

import androidx.annotation.NonNull;

public class CareTakerResponse {

    private int responseCode;
    private String responseMessage;
    private ErrorResponse errorResponse;
    public CareTakerResponse(int responseCode) {
        this.responseCode = responseCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }

    public ErrorResponse getErrorResponse() {
        return errorResponse;
    }

    public void setErrorResponse(ErrorResponse errorResponse) {
        this.errorResponse = errorResponse;
    }

    @Override
    public String toString() {
        return "CareTakerResponse{" +
                "responseMessage=" + responseMessage +
                ", errorResponse=" + errorResponse +
                '}';
    }

    public int getResponseCode() {
        return responseCode;
    }
}
