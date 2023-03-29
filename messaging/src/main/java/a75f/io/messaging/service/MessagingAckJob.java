package a75f.io.messaging.service;

import java.util.Map;
import java.util.Set;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.messaging.client.MessagingClient;

public class MessagingAckJob {
    private final String ccuId;
    private final MessagingService messagingService;

    public MessagingAckJob(String ccuId, String messagingUrl) {
        this.ccuId = ccuId;
        this.messagingService = new ServiceGenerator().createService(messagingUrl);
    }

    public Runnable getJobRunnable() {
        return () -> doJob();
    }

    /**
     * Fetches all processed message Ids and sends a bulk acknowledgement request to the
     * Messaging API
     */
    public void doJob() {
        CcuLog.d(L.TAG_CCU_MESSAGING, "Doing Ack Job");

        if (!MessagingClient.getInstance().isSubscribed()) {
            CcuLog.d(L.TAG_CCU_MESSAGING, "Not subscribed , reset connection");
            MessagingClient.getInstance().resetMessagingConnection();
            return;
        }

        Map<String, Set<String>> channelsToMessageIds = MessagingClient.getInstance().pollMessageIdsToAck();
        if (channelsToMessageIds.isEmpty()) {
            CcuLog.d(L.TAG_CCU_MESSAGING, "ACK Job exited. No messages to ACK.");
            return;
        }

        if (CCUHsApi.getInstance().getAuthorised()) {
            channelsToMessageIds.forEach((channel, messageIds) ->
            {
                    messagingService.acknowledgeMessages(channel, ccuId, new AcknowledgeRequest(messageIds))
                            .subscribe(
                                    response -> {
                                        if (response.isSuccessful()) {
                                            CcuLog.d(L.TAG_CCU_MESSAGING, "ACK Job Succeeded for Messages: " + messageIds);
                                        } else {
                                            CcuLog.w(L.TAG_CCU_MESSAGING, "ACK Job FAILED for Messages: " + messageIds + " ERR: " + response.code());
                                        }
                                    },
                                    error -> CcuLog.e(L.TAG_CCU_MESSAGING, "ACK Job FAILED for Messages: " + messageIds, error)
                            );
            });
        }
    }
}
