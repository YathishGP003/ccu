package a75f.io.alerts.log;


public interface SequencerLogsCallback {
    void logVerbose(LogLevel logLevel, LogOperation operationName, String message, String result);

    void logWarn(LogLevel logLevel, LogOperation operationName,  String message, String result);

    void logInfo(LogLevel logLevel, LogOperation operationName,  String message, String result);

    void logError(LogLevel logLevel, LogOperation operationName,  String message, String result);

    void logDebug(LogLevel logLevel, LogOperation operationName,  String message, String result);
}


