package a75f.io.logic.pubnub;

import android.content.Context;
import android.util.Log;

import com.google.gson.JsonElement;
import com.pubnub.api.PNConfiguration;
import com.pubnub.api.PubNub;
import com.pubnub.api.callbacks.PNCallback;
import com.pubnub.api.callbacks.SubscribeCallback;
import com.pubnub.api.models.consumer.PNStatus;
import com.pubnub.api.models.consumer.history.PNHistoryResult;
import com.pubnub.api.models.consumer.objects_api.channel.PNChannelMetadataResult;
import com.pubnub.api.models.consumer.objects_api.membership.PNMembershipResult;
import com.pubnub.api.models.consumer.objects_api.uuid.PNUUIDMetadataResult;
import com.pubnub.api.models.consumer.pubsub.PNMessageResult;
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult;
import com.pubnub.api.models.consumer.pubsub.PNSignalResult;
import com.pubnub.api.models.consumer.pubsub.files.PNFileEventResult;
import com.pubnub.api.models.consumer.pubsub.message_actions.PNMessageActionResult;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

import a75f.io.api.haystack.BuildConfig;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;

import static com.pubnub.api.enums.PNReconnectionPolicy.LINEAR;
import static com.pubnub.api.enums.PNStatusCategory.PNConnectedCategory;
import static com.pubnub.api.enums.PNStatusCategory.PNReconnectedCategory;

public class PbSubscriptionHandler {
    
    private static final int PB_HISTORY_FETCH_COUNT = 6000;
    private static final int PB_MAX_RECONNECTION_TRIES = 10;
    
    
    final String pubnubSubscribeKey = BuildConfig.PUBNUB_SUBSCRIBE_KEY;
    final String pubnubPublishKey = BuildConfig.PUBNUB_PUBLISH_KEY;
    
    private PubNub  pbInstance;
    private boolean pbSubscriptionStatus = false;
    
    private PbSubscriptionHandler(){
    }
    
    private static PbSubscriptionHandler instance = null;
    
    public static PbSubscriptionHandler getInstance() {
        if (instance == null) {
            synchronized(PbSubscriptionHandler.class) {
                if (instance == null) {
                    instance = new PbSubscriptionHandler();
                }
            }
        }
        return instance;
    }
    
    public void registerSite(Context appContext, String siteId) {
        
        if (pbInstance != null) {
            pbInstance.destroy();
        }
        PNConfiguration pbConfig = getConfiguration(siteId);
        SubscribeCallback pbCallback = getSubscriberCallback(siteId, appContext);
        pbInstance = new PubNub(pbConfig);
        pbInstance.addListener(pbCallback);
        pbInstance.subscribe().channels(Arrays.asList(siteId.replace("@", ""))).execute();
    }
    
    private PNConfiguration getConfiguration(String siteId) {
        
        PNConfiguration pnConfiguration = new PNConfiguration();
        pnConfiguration.setSubscribeKey(pubnubSubscribeKey);
        pnConfiguration.setPublishKey(pubnubPublishKey);
        pnConfiguration.setUuid(siteId);
        pnConfiguration.setSecure(false);
        pnConfiguration.setReconnectionPolicy(LINEAR);
        return pnConfiguration;
    }
    
    private SubscribeCallback getSubscriberCallback(String siteId, Context appContext) {
        
        return new SubscribeCallback() {
            @Override
            public void status(PubNub pubnub, PNStatus status) {
                
                /**
                 * PubNub reconnect does not seem to generate a status callback with Operation and Category
                 * during reconnect. Instead there is a status update with operation as null and category
                 * PNReconnectedCategory.
                 * This could be cleaned if a future version of pubnub lirary fixes this.
                 * */
                if (status.getOperation() == null && status.getCategory() == PNReconnectedCategory) {
                    CcuLog.i(L.TAG_CCU_PUBNUB, "Event PNReconnectedCategory "+status.toString());
                    pbSubscriptionStatus = true;
                    handleReconnect(siteId.replaceFirst("@",""),
                                    PbPreferences.getLastHandledTimeToken(appContext),
                                    appContext);
                    return;
                }
                
                switch (status.getOperation()) {
                    case PNSubscribeOperation:
                    case PNUnsubscribeOperation:
                        switch (status.getCategory()) {
                            case PNConnectedCategory:
                                // No error or issue whatsoever.
                                CcuLog.i(L.TAG_CCU_PUBNUB, "Event PNConnectedCategory "+status.toString());
                                pbSubscriptionStatus = true;
                                break;
                            case PNReconnectedCategory:
                                // Subscribe temporarily failed but reconnected.
                                // There is no longer any issue.
                                CcuLog.i(L.TAG_CCU_PUBNUB, "Event PNReconnectedCategory "+status.toString());
                                handleReconnect(siteId.replaceFirst("@",""),
                                                PbPreferences.getLastHandledTimeToken(appContext),
                                                appContext);
                                pbSubscriptionStatus = true;
                                break;
                            case PNDisconnectedCategory:
                                // No error in unsubscribing from everything.
                                CcuLog.i(L.TAG_CCU_PUBNUB, "Event PNDisconnectedCategory "+status.toString());
                                pbSubscriptionStatus = false;
                                break;
                            case PNUnexpectedDisconnectCategory:
                                // Usually an issue with the internet connection.
                                // This is an error: handle appropriately.
                                CcuLog.i(L.TAG_CCU_PUBNUB, "Event PNUnexpectedDisconnectCategory "+status.toString());
                                break;
                            case PNAccessDeniedCategory:
                                CcuLog.i(L.TAG_CCU_PUBNUB, "Event PNAccessDeniedCategory "+status.toString());
                                break;
    
                            default: {
                                CcuLog.i(L.TAG_CCU_PUBNUB, "Unhandled Status: "+status.toString());
                            }
                                
                        }
                    break;
                    default: {
                        CcuLog.i(L.TAG_CCU_PUBNUB, "Unknown Operation: "+status.toString());
                    }
                }
            }
    
            // Messages
            @Override
            public void message(PubNub pubnub, PNMessageResult message) {
        
                CcuLog.d(L.TAG_CCU_PUBNUB, "PubNub message: " + message.toString());
                PbMessageHandler.handlePunubMessage(message.getMessage(), appContext);
                PbPreferences.setLastHandledTimeToken(message.getTimetoken(), appContext);
            }
    
            // Presence
            @Override
            public void presence(@NotNull PubNub pubnub, @NotNull PNPresenceEventResult presence) {
                CcuLog.d(L.TAG_CCU_PUBNUB,"Presence Event: " + presence.getEvent());
            }
    
            // Signals
            @Override
            public void signal(PubNub pubnub, PNSignalResult signal) {
                CcuLog.d(L.TAG_CCU_PUBNUB,"Signal publisher: " + signal.getPublisher());
            }
    
            // Message actions
            @Override
            public void messageAction(PubNub pubnub, PNMessageActionResult pnActionResult) {
                CcuLog.d(L.TAG_CCU_PUBNUB,"Message action type: " + pnActionResult.getMessageAction().getType());
            }
    
            // files
            @Override
            public void file(PubNub pubnub, PNFileEventResult pnFileEventResult) {
                CcuLog.d(L.TAG_CCU_PUBNUB,"File channel: " + pnFileEventResult.getChannel());
            }
    
            @Override
            public void uuid(PubNub pubnub, PNUUIDMetadataResult pnFileEventResult) {
            }
    
            @Override
            public void channel(PubNub pubnub, PNChannelMetadataResult pnFileEventResult) {
            }
    
            @Override
            public void membership(PubNub pubnub, PNMembershipResult pnFileEventResult) {
            }
    
        };
    }
    
    
    private void handleReconnect(String channelId, Long pbLastTimeToken, Context appContext) {
        
        Log.d(L.TAG_CCU_PUBNUB, "handleReconnect: timeToken "+pbLastTimeToken);
    
        pbInstance.history()
                  .channel(channelId)
                  .reverse(false)
                  .includeTimetoken(true)
                  .includeMeta(false)
                  .end(pbLastTimeToken)
                  .count(PB_HISTORY_FETCH_COUNT)
                  .async((result, status) -> {
                      CcuLog.d(L.TAG_CCU_PUBNUB,
                               "handleReconnect: Pending Messages Count - "+result.getMessages().size());
    
                      Observable.fromIterable(result.getMessages())
                                .subscribeOn(Schedulers.io())
                                .filter(msg -> msg.getTimetoken() > pbLastTimeToken)
                                .subscribe(msg -> {
                                    PbMessageHandler.handlePunubMessage(msg.getEntry(), appContext);
                                    PbPreferences.setLastHandledTimeToken(msg.getTimetoken(), appContext);
                                }, throwable -> {
                                    CcuLog.d(L.TAG_CCU_PUBNUB, " Error! "+throwable.getMessage());
                                });
                  });
                  
    }
    
    public boolean isPubnubSubscribed() {
        return pbSubscriptionStatus;
    }
    
}
