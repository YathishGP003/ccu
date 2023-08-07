package a75f.io.api.haystack;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.reflect.TypeToken;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;

import org.projecthaystack.HDateTime;
import org.projecthaystack.HGrid;
import org.projecthaystack.HRow;
import org.projecthaystack.HVal;
import org.projecthaystack.io.HZincReader;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import a75f.io.data.RenatusDatabaseBuilder;
import a75f.io.data.WriteArray;
import a75f.io.data.entities.DatabaseHelper;
import a75f.io.data.entities.EntityDBUtilKt;
import a75f.io.data.entities.EntityDatabaseHelper;
import a75f.io.data.entities.HayStackEntity;
import a75f.io.data.writablearray.WritableArray;
import a75f.io.data.writablearray.WritableArrayDBUtilKt;
import a75f.io.data.writablearray.WritableArrayDatabaseHelper;
import a75f.io.logger.CcuLog;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class TagDbMigration {

    public static final String TAG = TagDbMigration.class.getSimpleName();

    public static void doTagStringToRoomDBMigration(Context context, RuntimeTypeAdapterFactory<HVal> hsTypeAdapter) {
        String tagsString = context.getSharedPreferences("ccu_tags", Context.MODE_PRIVATE).getString("tagsMap", null);
        String waPreference = context.getApplicationContext().getSharedPreferences("ccu_tags", Context.MODE_PRIVATE).getString("writeArrayMap", null);
        CcuLog.d(TAG, "tagsString-->" + tagsString);
        CcuLog.d(TAG, "waPreference-->" + waPreference);


        if(tagsString != null) {
            String test = new String(tagsString);
            HZincReader hZincReader = new HZincReader(tagsString);
            HGrid hGrid = hZincReader.readGrid();
            hGrid.dump();
            CcuLog.d(TAG, "hGrid.numRows():" + hGrid.numRows());


            for (int i = 0; i < hGrid.numRows(); i++) {
                HRow val = hGrid.row(i);
                CcuLog.d(TAG, "Migrate all entities");
                HashMap<String, Object> map = new HashMap<>();
                if(val != null && val.has("id")) {
                    String key = val.get("id").toString().replace("@", "");
                    map.put(key, val.toZinc());
                    HayStackEntity entity = new HayStackEntity(key, map);
                    EntityDBUtilKt.insert(entity, context);
                    CcuLog.d(TAG, "migrate this -->"+val);
                }else{
                    CcuLog.d(TAG, "can not migrate this it is missing id attribute-->"+val);
                }
            }
        }
        //read writable array.
        if(waPreference != null) {
            Gson gson = new GsonBuilder()
                    .registerTypeAdapterFactory(hsTypeAdapter).registerTypeAdapter(TimeZone.class, new TimeZoneInstanceCreator())
                    .setPrettyPrinting()
                    .disableHtmlEscaping()
                    .create();
            Type waType = new TypeToken<ConcurrentHashMap<String, CCUTagsDb.WriteArray>>() {
            }.getType();
            ConcurrentHashMap<String, CCUTagsDb.WriteArray> writeArrays = gson.fromJson(waPreference, waType);
            Log.d(TAG, "Migrate all writable");

            writeArrays.entrySet().forEach(entry -> {
                String id = entry.getKey();
                CCUTagsDb.WriteArray pointArray = (CCUTagsDb.WriteArray) entry.getValue();

                String[] pointWho = pointArray.who;
                HVal[] pointVal = pointArray.val;
                long[] pointDuration = pointArray.duration;
                HDateTime[] pointModifiedTime = pointArray.lastModifiedDateTime;

                WriteArray roomData = new WriteArray();

                for (int i = 0; i < pointWho.length; i++) {
                    roomData.setValue(pointVal[i] == null ? null : pointVal[i].toZinc(), i);
                    roomData.setWho(pointWho[i], i);
                    roomData.setDuration(pointDuration[i], i);
                    roomData.setModifiedTime(pointModifiedTime[i] == null ? null : pointModifiedTime[i].millisDefaultTZ(), i);
                }
                String data = new Gson().toJson(roomData);
                String key = id.replace("@", "");
                Log.d("SpooTag", "onPointWrite@@ id->" + key + "<-data->"+data);
                WritableArray writableArrayForRoomData = new WritableArray(key, data);

                Log.d("SpooTag", " ID " + key);
                WritableArrayDBUtilKt.insert(writableArrayForRoomData, context);
            });
        }
    }

    private static HVal[] convertToZinc(String[] value) {
        HVal[] val = new HVal[17];
        for (int i = 0; i < 17; i++) {
            if(value[i] == null){
                val[i] = null;
            }else{
                val[i] = new HZincReader(value[i]).readVal();
            }
        }
        return val;
    }

    private static class TimeZoneInstanceCreator implements InstanceCreator<TimeZone> {
        public TimeZone createInstance(Type type) {
            return TimeZone.getDefault();
        }
    }

}
