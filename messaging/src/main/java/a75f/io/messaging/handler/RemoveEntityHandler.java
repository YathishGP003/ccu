package a75f.io.messaging.handler;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.data.message.MessageDbUtilKt;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.messaging.MessageHandler;

public class RemoveEntityHandler implements MessageHandler
{
    public static final String CMD = "removeEntity";
    
    public void handleMessage(JsonObject msgObject)
    {
        try {
            //TODO - Revisit
            Gson gsonBuilder = new GsonBuilder().setPrettyPrinting()
                                                .disableHtmlEscaping()
                                                .create();
            HashMap m = gsonBuilder.fromJson(msgObject, HashMap.class);

            Type listType = new TypeToken<List<HashMap<String, String>>>() {
            }.getType();
            List<HashMap<String, String>> idList = gsonBuilder.fromJson(m.get("ids").toString(), listType);
            System.out.println(idList.toString());
            Log.i(L.TAG_CCU_MESSAGING, "Received message to delete: "+idList);

            for (HashMap id : idList) {
                String uuid = "@" + id.get("val").toString();
                if (CCUHsApi.getInstance().entitySynced(uuid)) {
                    HashMap<Object, Object> removedEntity = CCUHsApi.getInstance().readMapById(uuid);
                    if (!removedEntity.isEmpty() && removedEntity.containsKey("schedule")){
                        UpdateScheduleHandler.refreshSchedulesScreen();
                        UpdateScheduleHandler.refreshIntrinsicSchedulesScreen();
                    }
                    CCUHsApi.getInstance().removeEntity(uuid);
                } else if(CCUHsApi.getInstance().isEntityDeleted(uuid)) {
                    CCUHsApi.getInstance().removeId(uuid);
                }
            }
        } catch (Exception e) {
            CcuLog.d(L.TAG_CCU_PUBNUB, " Failed to parse removeEntity Json " + msgObject);
        }
    }

    @NonNull
    @Override
    public List<String> getCommand() {
        return Collections.singletonList(CMD);
    }

    @Override
    public void handleMessage(@NonNull JsonObject jsonObject, @NonNull Context context) {
        handleMessage(jsonObject);
    }
}
