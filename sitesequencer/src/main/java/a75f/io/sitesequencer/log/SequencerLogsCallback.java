package a75f.io.sitesequencer.log;

import a75f.io.alerts.log.LogLevel;
import a75f.io.alerts.log.LogOperation;
import a75f.io.sitesequencer.SiteSequencerDefinition;

public interface SequencerLogsCallback {
    void logVerbose(LogLevel logLevel, LogOperation operationName, String message, String result);

    void logWarn(LogLevel logLevel, LogOperation operationName,  String message, String result);

    void logInfo(LogLevel logLevel, LogOperation operationName,  String message, String result);

    void logInfo(LogLevel logLevel, LogOperation operationName,  String message, String result, String resultJson);

    void logError(LogLevel logLevel, LogOperation operationName,  String message, String result);

    void logDebug(LogLevel logLevel, LogOperation operationName,  String message, String result);
}


