package a75f.io.logic.autocommission;

public enum AutoCommissioningState {
    NOT_STARTED{
        @Override
        public String toString() {
            return "NOT_STARTED";
        }
    },
    STARTED{
        @Override
        public String toString() {
            return "STARTED";
        }
    },
    COMPLETED{
        @Override
        public String toString() {
            return "COMPLETED";
        }
    },
    ABORTED{
        @Override
        public String toString() {
            return "ABORTED";
        }
    };

    public static String getEnum() { return  NOT_STARTED+","+STARTED+","+COMPLETED+","+ABORTED; }
}
