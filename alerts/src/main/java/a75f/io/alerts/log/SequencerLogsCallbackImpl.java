package a75f.io.alerts.log;

import java.util.Date;

public class SequencerLogsCallbackImpl implements SequencerLogsCallback{

    SequenceLogs sequenceLogs;
    public SequencerLogsCallbackImpl(SequenceLogs sequenceLogs){
        this.sequenceLogs = sequenceLogs;
    }

    @Override
    public void logVerbose(LogLevel logLevel, LogOperation operationName, String message, String result) {
        sequenceLogs.addLog(new SequenceMethodLog(logLevel, operationName, message, result, new Date().toString(), new Date().toString(), null));
    }

    @Override
    public void logWarn(LogLevel logLevel, LogOperation operationName, String message, String result) {
        sequenceLogs.addLog(new SequenceMethodLog(logLevel, operationName, message, result, new Date().toString(), new Date().toString(), null));
    }

    @Override
    public void logInfo(LogLevel logLevel, LogOperation operationName, String message, String result) {
        sequenceLogs.addLog(new SequenceMethodLog(logLevel, operationName, message, result, new Date().toString(), new Date().toString(), null));
    }

    @Override
    public void logInfo(LogLevel logLevel, LogOperation operationName, String message, String result, String resultJson) {
        sequenceLogs.addLog(new SequenceMethodLog(logLevel, operationName, message, result, new Date().toString(), new Date().toString(), resultJson));
    }

    @Override
    public void logError(LogLevel logLevel, LogOperation operationName, String message, String result) {
        sequenceLogs.addLog(new SequenceMethodLog(logLevel, operationName, message, result, new Date().toString(), new Date().toString(), null));
    }

    @Override
    public void logDebug(LogLevel logLevel, LogOperation operationName, String message, String result) {
        sequenceLogs.addLog(new SequenceMethodLog(logLevel, operationName, message, result, new Date().toString(), new Date().toString(), null));
    }
}
