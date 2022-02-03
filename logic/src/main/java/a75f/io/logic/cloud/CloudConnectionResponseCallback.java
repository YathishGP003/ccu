package a75f.io.logic.cloud;

public interface CloudConnectionResponseCallback {
    void onSuccessResponse(boolean isOk);
    void onErrorResponse(boolean isOk);
}