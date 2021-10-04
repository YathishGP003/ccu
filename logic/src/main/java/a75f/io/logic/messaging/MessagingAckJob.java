package a75f.io.logic.messaging;

import java.util.Map;
import java.util.Set;

import a75f.io.logger.CcuLog;
import a75f.io.logic.L;

public class MessagingAckJob {
    private final String ccuId;
    private final MessagingService messagingService;

    public MessagingAckJob(String ccuId, String messagingUrl, String bearerToken) {
        this.ccuId = ccuId;

        this.messagingService = new ServiceGenerator().createService(messagingUrl, bearerToken);
    }

    public Runnable getJobRunnable()
    {
        return new Runnable()
        {
            @Override
            public void run()
            {
                doJob();
            }
        };
    }

    /**
     * Fetches all processed message Ids and sends a bulk acknowledgement request to the
     * Messaging API
     */
    public void doJob() {
        CcuLog.i(L.TAG_CCU_MESSAGING, "Doing Ack Job");

        Map<String, Set<String>> channelsToMessageIds = MessagingClient.getInstance().pollMessageIdsToAck();
        if (channelsToMessageIds.isEmpty()) {
            return;
        }

        channelsToMessageIds.forEach((channel, messageIds) -> {
                    messagingService.acknowledgeMessages(channel, ccuId, new AcknowledgeRequest(messageIds))
                            .subscribe(
                                    response -> {
                                        if (response.isSuccessful()) {
                                            CcuLog.i(L.TAG_CCU_MESSAGING, "ACK Succeeded - " + messageIds);
                                        } else {
                                            CcuLog.w(L.TAG_CCU_MESSAGING, "ACK FAILED - " + messageIds + " ERR: " + response.code());
                                        }
                                    }
                            );
                });
    }
}
