package a75f.io.api.haystack.util;

public class DatabaseEvent {
    private DatabaseAction mDatabaseAction;
    private byte[] mBytes;


    public DatabaseEvent() { /* Default constructor */ }


    public DatabaseEvent(DatabaseAction mAction) {
        this.mDatabaseAction = mAction;
        this.mBytes = null;
    }


    public DatabaseEvent(DatabaseAction mAction, byte[] bytes) {
        this.mDatabaseAction = mAction;
        this.mBytes = bytes;
    }


    public DatabaseAction getSerialAction() {
        return mDatabaseAction;
    }


    public void setSerialAction(DatabaseAction serialAction) {
        this.mDatabaseAction = serialAction;
    }


    public byte[] getBytes() {
        return mBytes;
    }


    public void setBytes(byte[] bytes) {
        this.mBytes = bytes;
    }
}