package a75f.io.logic.messaging;

import com.google.gson.JsonObject;
import com.here.oksse.OkSse;
import com.here.oksse.ServerSentEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logic.BuildConfig;
import a75f.io.logic.Globals;
import a75f.io.logic.cloud.RenatusServicesEnvironment;
import a75f.io.logic.pubnub.PbSubscriptionHandler;
import okhttp3.Request;

public class MessagingClient {
    private static MessagingClient instance = null;

    private ServerSentEvent sse = null;

    private final PriorityBlockingQueue<MessageToAck> messagesToAck = new PriorityBlockingQueue<>();

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

            if (ccuId != null) {
                String bearerToken = CCUHsApi.getInstance().getJwt();
                PbSubscriptionHandler.getInstance().close();
                this.openMessagingConnection(bearerToken, siteId.substring(1), ccuId.substring(1));
                Globals.getInstance().scheduleMessagingAckJob();
            }
        }
    }

    public boolean isSubscribed() {
        return sse != null;
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

    private void openMessagingConnection(String bearerToken, String siteId, String ccuId) {
        if (sse != null) {
            return;
        }

        String messagingUrl = RenatusServicesEnvironment.instance.getUrls().getMessagingUrl();

        String subscribeUrl = String.format("%s/messages/acknowledgeable?channels=%s,%s&subscriberId=%s",
                messagingUrl,
                BuildConfig.PUBNUB_GLOBAL_CHANNEL,
                siteId,
                ccuId);

        Request request = new Request.Builder()
                .url(subscribeUrl)
                .header("Authorization", "Bearer " + bearerToken)
                .build();

        sse = new OkSse().newServerSentEvent(request, new MessagingListener(siteId, ccuId, messagingUrl, bearerToken));
        sse.setTimeout(1, TimeUnit.MINUTES);
    }

    private void closeMessagingConnection() {
        if (sse != null) {
            sse.close();
            sse = null;
        }
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
