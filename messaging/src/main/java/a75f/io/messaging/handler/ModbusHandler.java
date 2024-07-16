package a75f.io.messaging.handler;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.projecthaystack.HDateTime;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;
import a75f.io.logger.CcuLog;

public class ModbusHandler {

    public static void updatePoint(JsonObject msgObject, Point configPoint) {
        //When a level is deleted, it currently generates a message with empty value.
        //Handle it here.
        if (msgObject.get("val").toString().equals("\"\"") || msgObject.get("val").toString().isEmpty()) {
            CCUHsApi.getInstance().clearPointArrayLevel(configPoint.getId(), msgObject.get("level").getAsInt(), true);
            CCUHsApi.getInstance().writeHisValById(configPoint.getId(), HSUtil.getPriorityVal(configPoint.getId()));
            return;
        }
        double duration;
        HDateTime lastModifiedDateTime;

        JsonElement durHVal = msgObject.get("duration");
        Object lastModifiedTimeTag = msgObject.get("lastModifiedDateTime");
        if (lastModifiedTimeTag != null) {
            lastModifiedDateTime = HDateTime.make(msgObject.get("lastModifiedDateTime").getAsString());
        } else {
            lastModifiedDateTime = HDateTime.make(System.currentTimeMillis());
        }
        double durationRemote = durHVal == null ? 0d : Double.parseDouble(durHVal.toString());
        //If duration shows it has already expired, then just write 1ms to force-expire it locally.
        duration = (durationRemote == 0 ? 0 : (durationRemote - System.currentTimeMillis()) > 0 ? (durationRemote - System.currentTimeMillis()) : 1);

        if(msgObject.get("val").getAsDouble()!=msgObject.get("val").getAsInt()) {
            CCUHsApi.getInstance().getHSClient().pointWrite(HRef.copy(configPoint.getId()), msgObject.get("level").getAsInt(),
                    CCUHsApi.getInstance().getCCUUserName(), HNum.make(msgObject.get("val").getAsDouble()),
                    HNum.make(duration), lastModifiedDateTime );
        } else {
            CCUHsApi.getInstance().getHSClient().pointWrite(HRef.copy(configPoint.getId()), msgObject.get("level").getAsInt(),
                    CCUHsApi.getInstance().getCCUUserName(), HNum.make(msgObject.get("val").getAsInt()),
                    HNum.make(duration), lastModifiedDateTime );
        }
    }
}
