package a75f.io.logic.pubnub;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;

public class RemoveEntityHandler
{
    public static final String CMD = "removeEntity";
    
    public static void handleMessage(JsonObject msgObject)
    {
        if (!CCUHsApi.getInstance().isCCURegistered()) {
            CcuLog.d(L.TAG_CCU_PUBNUB,"CCU does not have active registration, Ignore pubnub");
            return;
        }
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
    
            for (HashMap id : idList) {
                String guid = id.get("val").toString();
                String luid = "@" + guid;
                if (CCUHsApi.getInstance().entitySynced(luid))
                {
                    CCUHsApi.getInstance().removeEntity(luid);
        
                } else if(CCUHsApi.getInstance().getRemoveMapLUID(luid) != null) {
                    CCUHsApi.getInstance().removeId(CCUHsApi.getInstance().getRemoveMapLUID("@" + guid));
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
            CcuLog.d(L.TAG_CCU_PUBNUB, " Failed to parse removeEntity Json " + msgObject);
        }
    }
}
