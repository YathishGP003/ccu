package a75f.io.logic.pubnub;

import android.content.Context;
import android.util.Log;

import com.pubnub.api.PubNub;

import java.util.concurrent.TimeUnit;

import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

class PbReconnectionHandler {
    
    private static final int PB_HISTORY_FETCH_COUNT = 6000;
    private static final int PB_RECONNECT_DELAY_SECONDS = 30;
    
    public static void handleReconnect(String channelId, Long pbLastTimeToken, PubNub pbInstance, Context appContext) {
        
        Log.d(L.TAG_CCU_PUBNUB, "handleReconnect: timeToken " + pbLastTimeToken);
        
        if (pbLastTimeToken == 0) {
            //Token handling information is invalid. Skip fetching history.
            return;
        }
        
        pbInstance.history()
                  .channel(channelId)
                  .reverse(false)
                  .includeTimetoken(true)
                  .includeMeta(false)
                  .end(pbLastTimeToken)
                  .count(PB_HISTORY_FETCH_COUNT)
                  .async((result, status) -> {
            
                      if (result == null || result.getMessages() == null) {
                          CcuLog.d(L.TAG_CCU_PUBNUB, "handleReconnect: Empty messages");
                          return;
                      }
            
                      CcuLog.i(L.TAG_CCU_PUBNUB,
                               "handleReconnect: Pending Messages  - "+result.getMessages().size());
            
                      Observable.fromIterable(result.getMessages())
                                .subscribeOn(Schedulers.io())
                                .filter(msg -> msg.getTimetoken() > pbLastTimeToken)
                                .subscribe(msg -> {
                                    PbMessageHandler.getInstance().handlePunubMessage(msg.getEntry(),
                                                                                      msg.getTimetoken(),
                                                                         appContext);
                                }, throwable -> {
                                    CcuLog.e(L.TAG_CCU_PUBNUB, "Pubnub handling Error! "+throwable.getMessage());
                                });
                  });
        
    }
    
    
    public static void handleDisconnect(PubNub pbIntance) {
        
        Observable.timer(PB_RECONNECT_DELAY_SECONDS, TimeUnit.SECONDS)
                    .subscribeOn(Schedulers.io())
                    .subscribe ( i -> {
                        CcuLog.i(L.TAG_CCU_PUBNUB, "Reconnect Pubnub");
                        pbIntance.reconnect();
                        
                    });
                    
    }
}
