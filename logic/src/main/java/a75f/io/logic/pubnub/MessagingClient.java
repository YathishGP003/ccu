git package a75f.io.logic.pubnub;

import android.content.Context;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.here.oksse.OkSse;
import com.here.oksse.ServerSentEvent;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import a75f.io.logic.BuildConfig;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.cloud.RenatusServicesEnvironment;
import okhttp3.Request;
import okhttp3.Response;

public class MessagingClient {
    private static MessagingClient instance = null;

    private MessagingService messagingService = null;
    private ServerSentEvent sse = null;

    private String siteId = null;
    private String ccuId = null;

    public static MessagingClient getInstance() {
        if (instance == null) {
            synchronized(MessagingClient.class) {
                if (instance == null) {
                    instance = new MessagingClient();
                }
            }
        }
        return instance;
    }

    public void init() {
        boolean useMessagingApi = Globals.getInstance().isAckdMessagingEnabled();

        String siteId = CCUHsApi.getInstance().getSiteIdRef().toString();

        if (!useMessagingApi) {
            this.closeMessagingConnection();
            PbSubscriptionHandler.getInstance().registerSite(Globals.getInstance().getApplicationContext(), siteId);
        } else {
            String ccuId = CCUHsApi.getInstance().getCcuId();
            String bearerToken = CCUHsApi.getInstance().getJwt();

            PbSubscriptionHandler.getInstance().close();
            this.openMessagingConnection(bearerToken, siteId, ccuId);
        }
    }

    private void openMessagingConnection(String bearerToken, String siteId, String ccuId) {
        if (sse != null) {
            return;
        }

        String messagingUrl = RenatusServicesEnvironment.instance.getUrls().getMessagingUrl();

        String subscribeUrl = String.format("%s/messages/acknowledgeable?channels=%s,%s&subscriberId=%s",
//        String subscribeUrl = String.format("%s/messages?channels=%s,%s&subscriberId=%s",
                messagingUrl,
                BuildConfig.PUBNUB_GLOBAL_CHANNEL,
                siteId.substring(1),
                ccuId.substring(1));

        Request request = new Request.Builder()
                .url(subscribeUrl)
                .header("Authorization", "Bearer " + bearerToken)
                .build();

//        sse = new OkSse().newServerSentEvent(request, listener);
        sse = new OkSse().newServerSentEvent(request, new MessagingListener(siteId, ccuId));
        sse.setTimeout(1, TimeUnit.MINUTES);

        this.siteId = siteId;
        this.ccuId = ccuId;

        messagingService = ServiceGenerator.getInstance().createService(messagingUrl, bearerToken);
    }

    private void closeMessagingConnection() {
        if (sse != null) {
            sse.close();
            sse = null;
            messagingService = null;
        }
    }

    private class MessagingListener implements ServerSentEvent.Listener {
        private final String siteId;
        private final String ccuId;

        public MessagingListener(String siteId, String ccuId) {
            super();
            this.siteId = siteId;
            this.ccuId = ccuId;
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

            PbMessageHandler.getInstance().handlePubnubMessage(messageContents, timetoken, Globals.getInstance().getApplicationContext());

            messagingService.acknowledgeMessages(siteId, ccuId, new AcknowledgeRequest(Collections.singletonList(timetoken.toString())));
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
            CcuLog.w(L.TAG_CCU_MESSAGING, "SSE connection error. Attempting to reconnect");
            return true;
        }

        @Override
        public void onClosed(ServerSentEvent sse) {
            CcuLog.i(L.TAG_CCU_MESSAGING, "SSE Socket Closed");
        }
    }

    private ServerSentEvent.Listener listener = new ServerSentEvent.Listener() {
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

            PbMessageHandler.getInstance().handlePubnubMessage(messageContents, timetoken, Globals.getInstance().getApplicationContext());

            messagingService.acknowledgeMessages(siteId, ccuId, new AcknowledgeRequest(Collections.singletonList(timetoken.toString())));
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
            CcuLog.w(L.TAG_CCU_MESSAGING, "SSE connection error. Attempting to reconnect");
            return true;
        }

        @Override
        public void onClosed(ServerSentEvent sse) {
            CcuLog.i(L.TAG_CCU_MESSAGING, "SSE Socket Closed");
        }
    };
}
