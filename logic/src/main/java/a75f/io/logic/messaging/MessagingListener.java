package a75f.io.logic.messaging;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.here.oksse.ServerSentEvent;

import java.util.Collections;
import java.util.HashSet;

import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.pubnub.PbMessageHandler;
import a75f.io.logic.pubnub.RemoteCommandUpdateHandler;
import io.reactivex.rxjava3.core.Single;
import okhttp3.Request;
import okhttp3.Response;

public class MessagingListener implements ServerSentEvent.Listener {
    private final String siteId;
    private final String ccuId;
    private final String messagingUrl;
    private final String bearerToken;

    private MessagingService messagingService;

    public MessagingListener(String siteId, String ccuId, String messagingUrl, String bearerToken) {
        super();

        this.siteId = siteId;
        this.ccuId = ccuId;
        this.messagingUrl = messagingUrl;
        this.bearerToken = bearerToken;
    }

    @Override
    public void onOpen(ServerSentEvent sse, Response response) {
        CcuLog.i(L.TAG_CCU_MESSAGING, "SSE Connection Opened");

        if (messagingService == null) {
            messagingService = new ServiceGenerator().createService(messagingUrl, bearerToken);
        }
    }

    @Override
    public void onMessage(ServerSentEvent sse, String id, String event, String message) {
        CcuLog.d(L.TAG_CCU_MESSAGING, message);

        JsonObject payload = JsonParser.parseString(message).getAsJsonObject();
        Long timetoken = payload.get("timetoken").getAsLong();
        JsonElement messageContents = payload.getAsJsonObject().get("message");

        // Special case for "Restart CCU" and "Restart Tablet" remote commands:
        // The message needs to be synchronously acknowledged before it's acted upon to prevent the app
        // from entering a restart-loop, where un-ack'd restart messages are continuously received at start-up
        if (isRestartCommand(messageContents)) {
            acknowledge(payload);
        } else {
            acknowledgeAsync(payload);
        }

        // Send to message handler
        PbMessageHandler.getInstance().handlePubnubMessage(messageContents, timetoken, Globals.getInstance().getApplicationContext());
    }

    @Override
    public void onComment(ServerSentEvent sse, String comment) {
        CcuLog.w(L.TAG_CCU_MESSAGING, "Received Unexpected Comment: " + comment);
    }

    @Override
    public Request onPreRetry(ServerSentEvent sse, Request originalRequest) {
        CcuLog.w(L.TAG_CCU_MESSAGING, "Pre-Retry Triggered");
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

    private boolean isRestartCommand(JsonElement messageContents) {
        if (messageContents == null || !messageContents.isJsonObject()) {
            return false;
        }

        JsonObject messageObject = messageContents.getAsJsonObject();

        // Determine if the message is a remote Command of Type "Restart CCU" or "Restart Tablet"
        return messageObject != null
                && messageObject.has("command")
                && RemoteCommandUpdateHandler.CMD.equals(messageObject.get("command").getAsString())
                && (RemoteCommandUpdateHandler.RESTART_CCU.equals(messageObject.get(RemoteCommandUpdateHandler.CMD_TYPE).getAsString())
                    || RemoteCommandUpdateHandler.RESTART_TABLET.equals(messageObject.get(RemoteCommandUpdateHandler.CMD_TYPE).getAsString()));

    }

    private void acknowledge(JsonElement payload) {
        if (payload == null || !payload.isJsonObject()) {
            return;
        }

        JsonObject payloadObject = payload.getAsJsonObject();
        if (!payloadObject.has("messageId")) {
            return;
        }

        String messageId = payloadObject.get("messageId").getAsString();

        Single<retrofit2.Response<Void>> ackResponse = messagingService.acknowledgeMessages(siteId, ccuId, new AcknowledgeRequest(new HashSet<>(Collections.singletonList(messageId))));
        if (ackResponse.blockingGet().isSuccessful()) {
            CcuLog.d(L.TAG_CCU_MESSAGING, "ACK Succeeded for Message:  " + messageId);
        } else {
            CcuLog.w(L.TAG_CCU_MESSAGING, "Failed to ACK Message: " + messageId);
        }
    }

    /**
     * Pushes the received messageId into the Acknowledgement queue to be picked up asynchronously
     * by the Message Ack Job
     */
    private void acknowledgeAsync(JsonObject payload) {
        MessagingClient.getInstance().queueMessageIdToAck(payload);
    }
}
