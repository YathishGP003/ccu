package a75f.io.api.haystack.util;

public class DatabaseEvent {
    private final DatabaseAction mDatabaseAction;
    private byte[] mBytes;


    public DatabaseEvent(DatabaseAction mAction) {
        this.mDatabaseAction = mAction;
        this.mBytes = null;
    }


    public DatabaseAction getSerialAction() {
        return mDatabaseAction;
    }


    public byte[] getBytes() {
        return mBytes;
    }


    public void setBytes(byte[] bytes) {
        this.mBytes = bytes;
    }
}