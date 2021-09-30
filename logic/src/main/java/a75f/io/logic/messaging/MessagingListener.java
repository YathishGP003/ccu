package a75f.io.logic.messaging;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.here.oksse.ServerSentEvent;

import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.pubnub.PbMessageHandler;
import okhttp3.Request;
import okhttp3.Response;

public  class MessagingListener implements ServerSentEvent.Listener {
    public MessagingListener() {
        super();
    }

    @Override
    public void onOpen(ServerSentEvent sse, Response response) {
        CcuLog.i(L.TAG_CCU_MESSAGING, "SSE Connection Opened");
    }

    @Override
    public void onMessage(ServerSentEvent sse, String id, String event, String message) {
        CcuLog.i(L.TAG_CCU_MESSAGING, message);

        JsonObject payload = JsonParser.parseString(message).getAsJsonObject();
        Long timetoken = payload.get("timetoken").getAsLong();
        JsonElement messageContents = payload.getAsJsonObject().get("message");

        // Send to message handler
        PbMessageHandler.getInstance().handlePubnubMessage(messageContents, timetoken, Globals.getInstance().getApplicationContext());

        MessagingClient.getInstance().queueMessageIdToAck(payload);
    }

    @Override
    public void onComment(ServerSentEvent sse, String comment) {
        CcuLog.w(L.TAG_CCU_MESSAGING, "Received Unexpected Comment: " + comment);
    }

    @Override
    public Request onPreRetry(ServerSentEvent sse, Request originalRequest) {
        CcuLog.i(L.TAG_CCU_MESSAGING, "Pre-Retry Fired");
        return originalRequest;
    }

    @Override
    public boolean onRetryTime(ServerSentEvent sse, long milliseconds) {
        return true;
    }

    @Override
    public boolean onRetryError(ServerSentEvent sse, Throwable throwable, Response response) {
        CcuLog.w(L.TAG_CCU_MESSAGING, "SSE connection error. Attempting to reconnect", throwable);
        return true;
    }

    @Override
    public void onClosed(ServerSentEvent sse) {
        CcuLog.i(L.TAG_CCU_MESSAGING, "SSE Socket Closed");
    }
}
