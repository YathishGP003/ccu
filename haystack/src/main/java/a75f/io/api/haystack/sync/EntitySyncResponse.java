package a75f.io.api.haystack.sync;

public class EntitySyncResponse {
    private int respCode;
    private String respString;
    private String errRespString;
    public int getRespCode() {
        return respCode;
    }
    public void setRespCode(int respCode) {
        this.respCode = respCode;
    }
    public String getRespString() {
        return respString;
    }
    public void setRespString(String respString) {
        this.respString = respString;
    }
    public String getErrRespString() {
        return errRespString;
    }
    public void setErrRespString(String errRespString) {
        this.errRespString = errRespString;
    }
}
