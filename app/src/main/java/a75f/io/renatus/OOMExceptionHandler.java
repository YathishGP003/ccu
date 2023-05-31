package a75f.io.renatus;

public class OOMExceptionHandler {

    public static boolean isOOMCausedByFragmentation(Throwable throwable) {
        if (throwable instanceof OutOfMemoryError) {
            String message = throwable.getMessage();
            return message != null && message.contains("Failed to allocate");
        }
        return false;
    }

}
