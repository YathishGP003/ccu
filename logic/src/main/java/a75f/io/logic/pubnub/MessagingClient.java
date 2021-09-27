package a75f.io.logic.pubnub;

import com.here.oksse.OkSse;
import com.here.oksse.ServerSentEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import a75f.io.logger.CcuLog;
import a75f.io.logic.BuildConfig;
import a75f.io.logic.L;
import a75f.io.logic.cloud.RenatusServicesEnvironment;
import okhttp3.Request;
import okhttp3.Response;

import io.seventyfivef.messaging.api.IncomingMessage;
import io.seventyfivef.messaging.api.Messaging;
import io.seventyfivef.messaging.api.MessagingConfig;

public class MessagingClient {
    private static MessagingClient instance = null;

    private OkSse okSse = null;

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

    public void init(String bearerToken, String siteId, String ccuId) {
        // TODO: Read Dev Setting and instantiate consumer appropriately

        String messagingUrl = RenatusServicesEnvironment.instance.getUrls().getMessagingUrl();

//        String subscribeUrl = String.format("%s/messages/acknowledgeable?channels=%s,%s&subscriberId=%s",
        String subscribeUrl = String.format("%s/messages?channels=%s&subscriberId=%s",
                messagingUrl,
//                BuildConfig.PUBNUB_GLOBAL_CHANNEL,
                siteId.substring(1),
                ccuId.substring(1));

        Request request = new Request.Builder()
                .url(subscribeUrl)
                .header("Authorization", "Bearer " + bearerToken)
                .build();

        okSse = new OkSse();

        ServerSentEvent sse = okSse.newServerSentEvent(request, listener);
        sse.setTimeout(1, TimeUnit.MINUTES);

        Messaging messaging = new Messaging(
                new MessagingConfig(
                        messagingUrl,
                        bearerToken, // Literal token
                        "BEARER_TOKEN", // Token type
                        Arrays.asList(502, 503, 504), // List of HTTP codes to retry on
                        3 // Maximum number of retries
                )
        );

        messaging.subscribeToChannels(new HashSet<>(Arrays.asList(siteId.substring(1))), this::processMessage);
    }

    private boolean processMessage(IncomingMessage incomingMessage) {
        CcuLog.i(L.TAG_CCU_MESSAGING, "Processed Message!!!" + incomingMessage.getJsonString());

        return true;
    }

    private ServerSentEvent.Listener listener = new ServerSentEvent.Listener() {
        @Override
        public void onOpen(ServerSentEvent sse, Response response) {
            // When the channel is opened
        }

        @Override
        public void onMessage(ServerSentEvent sse, String id, String event, String message) {
//            CcuLog.i(L.TAG_CCU_MESSAGING, "Message Received!!!");
//            CcuLog.i(L.TAG_CCU_MESSAGING, message);

//            PbMessageHandler.getInstance().handlePunubMessage(message, message.getTimetoken(),
//                    appContext);
        }

        @Override
        public void onComment(ServerSentEvent sse, String comment) {
            // When a comment is received
        }

        @Override
        public Request onPreRetry(ServerSentEvent sse, Request originalRequest) {
            return originalRequest;
        }

        @Override
        public boolean onRetryTime(ServerSentEvent sse, long milliseconds) {
            return true; // True to use the new retry time received by SSE
        }

        @Override
        public boolean onRetryError(ServerSentEvent sse, Throwable throwable, Response response) {
            return true; // True to retry, false otherwise
        }

        @Override
        public void onClosed(ServerSentEvent sse) {
            // Channel closed
            CcuLog.i(L.TAG_CCU_MESSAGING, "SSE Socket Closed");
        }
    };
}
