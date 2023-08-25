package a75f.io.messaging.handler;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.projecthaystack.HDateTime;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;
import org.projecthaystack.HVal;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Point;

public class ModbusHandler {

    public static void updatePoint(JsonObject msgObject, Point configPoint) {
        double duration = 0;
        HDateTime lastModifiedDateTime;

        JsonElement durHVal = msgObject.get("duration");
        Object lastModifiedTimeTag = msgObject.get("lastModifiedDateTime");
        if (lastModifiedTimeTag != null) {
            lastModifiedDateTime = (HDateTime) lastModifiedTimeTag;
        } else {
            lastModifiedDateTime = HDateTime.make(System.currentTimeMillis());
        }
        double durationRemote = durHVal == null ? 0d : Double.parseDouble(durHVal.toString());
        //If duration shows it has already expired, then just write 1ms to force-expire it locally.
        duration = (durationRemote == 0 ? 0 : (durationRemote - System.currentTimeMillis()) > 0 ? (durationRemote - System.currentTimeMillis()) : 1);


        CCUHsApi.getInstance().getHSClient().pointWrite(HRef.copy(configPoint.getId()), msgObject.get("level").getAsInt(),
                CCUHsApi.getInstance().getCCUUserName(), HNum.make(msgObject.get("val").getAsInt()),
                HNum.make(duration), lastModifiedDateTime );
    }
}
