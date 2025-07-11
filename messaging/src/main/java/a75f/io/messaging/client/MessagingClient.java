package a75f.io.messaging.client;

import com.google.gson.JsonObject;
import com.here.oksse.OkSse;
import com.here.oksse.ServerSentEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import a75f.io.logic.BuildConfig;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.cloud.RenatusServicesEnvironment;
import a75f.io.logic.jobs.bearertoken.BearerTokenManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class MessagingClient implements BearerTokenManager.OnBearerTokenRefreshListener {
    private static MessagingClient instance = null;

    private ServerSentEvent sse = null;

    private final PriorityBlockingQueue<MessageToAck> messagesToAck = new PriorityBlockingQueue<>();

    private OkSse okSse = null;

    private long lastConnectedTime;

    private MessagingClient() {
        BearerTokenManager.getInstance().setOnBearerTokenRefreshListener(this);
    }
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
        } else {
            String ccuId = CCUHsApi.getInstance().getCcuId();

            if (ccuId != null) {
                this.openMessagingConnection(siteId.substring(1), ccuId.substring(1));
            }
        }
    }

    public boolean isSubscribed() {
        return sse != null &&
               okSse.getClient().connectionPool().connectionCount() > 0;
    }

    public void queueMessageIdToAck(JsonObject message) {
        String channel = message.get("channel").getAsString();
        String messageId = message.get("messageId").getAsString();

        messagesToAck.add(new MessageToAck(channel, messageId));
    }

    /**
     * A thread-safe mechanism for the MessagingAckJob to dequeue all processed message Ids
     * and send a bulk acknowledgement request out to the Messaging API every 60 seconds
     */
    public Map<String, Set<String>> pollMessageIdsToAck() {
        List<MessageToAck> polledMessages = new ArrayList<>();
        messagesToAck.drainTo(polledMessages);

        Map<String, Set<String>> channelsToMessageIdsLookup = new HashMap<>();
        polledMessages.forEach(message -> {
            String channel = message.channel;
            String messageId = message.messageId;

            if (channelsToMessageIdsLookup.containsKey(channel)) {
                channelsToMessageIdsLookup.get(channel).add(messageId);
            } else {
                channelsToMessageIdsLookup.put(channel, new HashSet<>(Collections.singletonList(messageId)));
            }
        });

        return channelsToMessageIdsLookup;
    }

    /**
     * Resets Messaging connection without restarting the MessagingAck thread.
     * Could be called when Messaging client runs into error.
     * Connection will not be opened before complete registration, or if
     * the CCU loses authentication (i.e. expired bearer token)
     */
    public void resetMessagingConnection(boolean isTokenRefreshed) {
        this.closeMessagingConnection();
        String siteId = CCUHsApi.getInstance().getSiteIdRef().toString();
        String ccuId = CCUHsApi.getInstance().getCcuId();

        if (ccuId == null) {
            CcuLog.i(L.TAG_CCU_MESSAGING, "ccuId is null, Hence messaging connection opening process skipped.");
        } else if (!CCUHsApi.getInstance().getAuthorised()) {
            CcuLog.i(L.TAG_CCU_MESSAGING, "JWT is empty or not exist. Hence messaging connection opening process skipped.");
        }else if(isTokenRefreshed){
            /*
         * If token is refreshed, then no need to wait for 5 minutes hence opening the connection immediately
         */
            this.openMessagingConnection(siteId.substring(1), ccuId.substring(1));
        }else {
            long timeSinceLastConnection = System.currentTimeMillis() - lastConnectedTime;

            if (timeSinceLastConnection >= 300000) {
                CcuLog.i(L.TAG_CCU_MESSAGING, "Attempting to reconnect to messaging. "+
                        "Last connected time: "+new Date(lastConnectedTime)+
                        ", milliseconds since the last connection:"+timeSinceLastConnection);

                this.openMessagingConnection(siteId.substring(1), ccuId.substring(1));
            } else {
                CcuLog.i(L.TAG_CCU_MESSAGING, "Messaging connection opening process skipped " +
                        "because "+timeSinceLastConnection+" milliseconds not more than 5 minutes");
            }
        }
    }

    private void openMessagingConnection(String siteId, String ccuId) {
        lastConnectedTime = System.currentTimeMillis();
        CcuLog.i(L.TAG_CCU_MESSAGING, "Opening Messaging Connection, lastConnectedTime: "+new Date(lastConnectedTime));

        if (sse != null) {
            return;
        }

        String messagingUrl = RenatusServicesEnvironment.instance.getUrls().getMessagingUrl();
        String bearerToken = CCUHsApi.getInstance().getJwt();

        String subscribeUrl = String.format("%s/messages/acknowledgeable?channels=%s,%s&subscriberId=%s",
                messagingUrl,
                BuildConfig.MESSAGING_GLOBAL_CHANNEL,
                siteId,
                ccuId);

        Request request = new Request.Builder()
                .url(subscribeUrl)
                .header("Authorization", "Bearer " + bearerToken)
                .build();

        CcuLog.d("CCU_HTTP_REQUEST", "MessagingClient: [GET] " + subscribeUrl + " - Token: " + bearerToken);
        // added readTimeout of 0 to allow for long-lived SSE connections
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .build();
        OkSse okSse = new OkSse(client);
        sse = okSse.newServerSentEvent(request, new MessagingListener(siteId, ccuId, messagingUrl));
        sse.setTimeout(1, TimeUnit.MINUTES);
    }

    private void closeMessagingConnection() {
        CcuLog.i(L.TAG_CCU_MESSAGING, "Closing Messaging Connection");

        if (sse != null) {
            sse.close();
            sse = null;
        }
    }

    @Override
    public void onTokenRefresh() {
        CcuLog.i(L.TAG_CCU_MESSAGING, "BearerToken refreshed. Reset messaging connection.");
        resetMessagingConnection(true);
    }

    private class MessageToAck implements Comparable<MessageToAck> {
        private final String channel;
        private final String messageId;

        public MessageToAck(String channel, String messageId) {
            this.channel = channel;
            this.messageId = messageId;
        }

        public int compareTo(MessageToAck messageToAck) {
            return channel.equals(messageToAck.channel) && messageId.equals(messageToAck.messageId)
                    ? 0
                    : 1;
        }
    }
}
