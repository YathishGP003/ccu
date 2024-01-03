package a75f.io.logic.autocommission.remoteSession;

public enum RemoteSessionStatus {

    NOT_AVAILABLE{
        @Override
        public String toString() {
            return "notAvailable";
        }
    },
    REGISTERED{
        @Override
        public String toString() {
            return "registered";
        }
    },
    AVAILABLE{
        @Override
        public String toString() {
            return "available";
        }
    },
    IN_PROGRESS{
        @Override
        public String toString() {
            return "remoteSessionInProgress";
        }
    },
    STOPPED{
        @Override
        public String toString() {
            return "stopped";
        }
    },
    TIMEOUT{
        @Override
        public String toString() {
            return "timeout";
        }
    };
    public static String getEnum() {
        return  NOT_AVAILABLE+","+REGISTERED+","+AVAILABLE+","
            +IN_PROGRESS+","+STOPPED+","+TIMEOUT ;
    }

}
