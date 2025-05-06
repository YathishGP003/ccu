package a75f.io.alerts;


import static a75f.io.alerts.AlertProcessor.TAG_CCU_ALERTS;
import static a75f.io.util.query_parser.QueryParserKt.modifyKVPairFromFilter;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Function;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8Value;
import com.google.gson.Gson;

import org.projecthaystack.HDateTime;
import org.projecthaystack.HDateTimeRange;
import org.projecthaystack.HGrid;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;
import org.projecthaystack.HTimeZone;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import a75f.io.alerts.log.LogLevel;
import a75f.io.alerts.log.LogOperation;
import a75f.io.alerts.log.SequencerLogsCallback;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HisItem;
import a75f.io.logger.CcuLog;

public class HaystackService {

    private static final String TAG = TAG_CCU_ALERTS;

    private static final String MSG_NO_ENTITY = "No entity found for given filter";

    private static final String MSG_NO_VALUE = "No value Found for given filter, please check query";
    private static final String MSG_NO_ENTITY_FOR_ID = "No Entity found for given id, please check query";
    private static final String MSG_CALCULATING = "calculating";
    static final String MSG_SUCCESS = "success";
    private static final String MSG_FAILED = "failed";
    private static final String WHO = "CCU_SEQUENCER";

    private static final String MSG_ERROR_NO_ENTITIES = "Error in finding Entities, please check query";
    private static final String MSG_ERROR_NO_ENTITY = "Error in finding Entity, please check query";

    private V8Function fetchFunction;
    private V8Array v8Array;

    public static final int SYSTEM_DEFAULT_VAL_LEVEL = 17;
    public static final int UI_DEFAULT_VAL_LEVEL = 8;
    public static final int SYSTEM_BUILDING_VAL_LEVEL = 16;

    private SequencerLogsCallback sequenceLogsCallback;

    private String reason;

    HaystackService(SequencerLogsCallback sequenceLogsCallback){
        this.sequenceLogsCallback = sequenceLogsCallback;
    }

    // return array of values
    public String hisReadManyInterpolateWithDateRange(Object hisPointIdObj, String startDate, String endDate, Object  contextHelper) {
        String hisPointId = hisPointIdObj.toString();
        String message = String.format(Locale.ENGLISH, "read, his values for  point with id = %s, startDate = %s, endDate = %s", hisPointId, startDate, endDate);
        sequenceLogsCallback.logInfo(LogLevel.INFO, LogOperation.valueOf("HIS_READMANY_INTERPOLATE"), message, MSG_CALCULATING);
        long startDateInMillis = convertStringToMilliseconds(startDate, "yyyy-MM-dd");
        long endDateInMillis = convertStringToMilliseconds(endDate, "yyyy-MM-dd");

        HDateTimeRange dateTimeRange = HDateTimeRange.make(HDateTime.make(startDateInMillis).date,
                HDateTime.make(endDateInMillis).date, HTimeZone.DEFAULT);

        List<HisItem> hisItems = CCUHsApi.getInstance()
                .hisRead(hisPointId, dateTimeRange
                );

        Double[] values = hisItems.stream()
                .map(HisItem::getVal)
                .toArray(Double[]::new);


        HashMap resultMap = new HashMap();
        resultMap.put("values", Arrays.toString(values));

        String finalMessage = String.format(Locale.ENGLISH, "found values %s", Arrays.toString(values));
        sequenceLogsCallback.logInfo(LogLevel.INFO, LogOperation.valueOf("HIS_READMANY_INTERPOLATE"), finalMessage, MSG_SUCCESS, getResultJson(resultMap));
        return new Gson().toJson(values);
    }

    public String hisReadManyInterpolateWithInterval(String hisPointId, String rangeValue, String rangeUnit, Object contextHelper) {
        String message = String.format(Locale.ENGLISH, "read, his values for  point with id = %s, rangeValue = %s, rangeUnit = %s", hisPointId, rangeValue, rangeUnit);
        sequenceLogsCallback.logInfo(LogLevel.INFO, LogOperation.valueOf("HIS_READMANY_INTERPOLATE"), message, MSG_CALCULATING);
        CcuLog.d(TAG, "---hisReadManyInterpolateWithInterval##$--hisPointId-" + hisPointId + "<--rangeValue-->" + rangeValue + "<--rangeUnit-->" + rangeUnit);
        int totalMinutes = 0;
        if (rangeUnit.equalsIgnoreCase("min")) {
            totalMinutes = Integer.parseInt(rangeValue);
            return hisReadManyInterpolateWithInterval(hisPointId, Long.parseLong(String.valueOf(totalMinutes)), contextHelper);
        } else if (rangeUnit.equalsIgnoreCase("hour")) {
            totalMinutes = 60 * Integer.parseInt(rangeValue);
            return hisReadManyInterpolateWithInterval(hisPointId, Long.parseLong(String.valueOf(totalMinutes)), contextHelper);
        } else if (rangeUnit.equalsIgnoreCase("day")) {
            totalMinutes = 24 * 60 * Integer.parseInt(rangeValue);
            return hisReadManyInterpolateWithInterval(hisPointId, Long.parseLong(String.valueOf(totalMinutes)), contextHelper);
        }
        sequenceLogsCallback.logInfo(LogLevel.INFO, LogOperation.valueOf("HIS_READMANY_INTERPOLATE"), "failed to get values", MSG_FAILED);
        return "";
    }

    // return array of values
    public String hisReadManyInterpolateWithInterval(String hisPointId, long minutes, Object contextHelper) {
        String message = String.format(Locale.ENGLISH, "read his values for  point with id = %s, minutes = %d", hisPointId, minutes);
        sequenceLogsCallback.logInfo(LogLevel.INFO, LogOperation.valueOf("HIS_READMANY_INTERPOLATE"), message, MSG_CALCULATING);
        CcuLog.d(TAG, "---hisReadManyInterpolateWithInterval##--hisPointId-"+hisPointId + "<--minutes-->"+minutes);
        long endDateInMillis = System.currentTimeMillis();
        long startDateInMillis = endDateInMillis - (minutes * 60 * 1000);

        HDateTimeRange dateTimeRange = HDateTimeRange.make(HDateTime.make(startDateInMillis).date,
                HDateTime.make(endDateInMillis).date, HTimeZone.DEFAULT);

        List<HisItem> hisItems = CCUHsApi.getInstance()
                .hisRead(hisPointId, dateTimeRange
                );

        Double[] values = hisItems.stream()
                .map(HisItem::getVal)
                .toArray(Double[]::new);
        String finalMessage = String.format(Locale.ENGLISH, "found values %s", Arrays.toString(values));

        HashMap resultMap = new HashMap();
        resultMap.put("values", Arrays.toString(values));

        sequenceLogsCallback.logInfo(LogLevel.INFO, LogOperation.valueOf("HIS_READMANY_INTERPOLATE"), finalMessage, MSG_SUCCESS, getResultJson(resultMap));
        return new Gson().toJson(values);
    }

    public void pointWrite(String id, int level, double value, boolean override, Object contextHelper) {
        String message = String.format("writing point with id = %s, level = %d, value = %s", id, level, value);
        sequenceLogsCallback.logInfo(LogLevel.INFO, LogOperation.valueOf("POINT_WRITE"), message, MSG_CALCULATING);
        CcuLog.d(TAG, "---pointWrite##--id-"+id + "<--level-->"+level+"<--override-->"+override);
        CCUHsApi.getInstance().pointWrite(HRef.copy(id), level, "CCU_ALERTS_SEQUENCER", HNum.make(value), HNum.make(0));
    }

    public void pointWriteMany(V8Array ids, int level, double value, boolean override, Object contextHelper) {
        CcuLog.d(TAG, "---pointWriteMany##--id-"+ids.length() + "<--level-->"+level+"<--override-->"+override);
        for(int i=0;i<ids.length(); i++){
            String pointId = ids.getString(i);
            String message = String.format("writing point with, id = %s, level = %d, value = %s", HRef.copy(pointId), level, value);
            sequenceLogsCallback.logInfo(LogLevel.INFO, LogOperation.valueOf("POINT_WRITE"), message, MSG_CALCULATING);
            CCUHsApi.getInstance().pointWrite(HRef.copy(pointId), level, "Seq_Demo", HNum.make(value), HNum.make(0));
        }
        ids.close();
    }

    public boolean hisWriteMany(Map<String, Double> mapOfPointIds, Object contextHelper) {
        CcuLog.d(TAG, "---hisWriteMany##--id-" + mapOfPointIds.size());
        sequenceLogsCallback.logInfo(LogLevel.INFO, LogOperation.valueOf("HIS_WRITE_MANY"), mapOfPointIds.toString(), MSG_CALCULATING);
        if (!mapOfPointIds.isEmpty()) {
            for (Map.Entry<String, Double> entry : mapOfPointIds.entrySet()) {
                String key = entry.getKey();
                Double value = entry.getValue();
                CcuLog.d(TAG, "---hisWriteMany##--id-" + key + "<--value-->" + value);
                CCUHsApi.getInstance().writeHisValById(key, value);
                sequenceLogsCallback.logInfo(LogLevel.INFO, LogOperation.valueOf("HIS_WRITE_MANY"), "writing --> " + value + "<--at-->" + key, MSG_SUCCESS);
            }
            return true;
        }
        sequenceLogsCallback.logInfo(LogLevel.INFO, LogOperation.valueOf("HIS_WRITE_MANY"), "failed during his write many", MSG_FAILED);
        return false;
    }

    public boolean hisWriteMany(String[] ids, Double value, Object contextHelper) {
        Map<String, Double> idValueMap = Arrays.stream(ids).collect(Collectors.toMap(key -> key, key -> value, (a, b) -> b));
        return hisWriteMany(idValueMap, contextHelper);
    }

    // returns list of entities
    public String findByFilter(String filter, Object contextHelper) {
        sequenceLogsCallback.logInfo(LogLevel.INFO, LogOperation.valueOf("FIND_BY_FILTER"), filter, MSG_CALCULATING);
        CcuLog.d(TAG, "---findByFilter##--original filter-" + filter);
        List<HashMap> list = findByFilterCustom(filter, contextHelper);
        String result = new Gson().toJson(list);
        if (list.size() > 0) {
            sequenceLogsCallback.logInfo(LogLevel.INFO, LogOperation.valueOf("FIND_BY_FILTER"), getCustomMessage(list.size()), MSG_SUCCESS, result);
        } else {
            sequenceLogsCallback.logError(LogLevel.ERROR, LogOperation.valueOf("FIND_BY_FILTER"), MSG_ERROR_NO_ENTITIES, MSG_FAILED);
        }
        CcuLog.d(TAG, "---findByFilter##--original filter result->" + result);
        return result;
    }


    // returns single entity
    public String findEntityByFilter(String filter, Object contextHelper) {
        CcuLog.d(TAG, "---findEntityByFilter##--original filter-" + filter);
        sequenceLogsCallback.logInfo(LogLevel.INFO, LogOperation.valueOf("FIND_ENTITY_BY_FILTER"), filter, MSG_CALCULATING);
        List<HashMap> list = findByFilterCustom(filter, contextHelper);
        if(!list.isEmpty()){
            String result = new Gson().toJson(list.get(0));
            sequenceLogsCallback.logInfo(LogLevel.INFO, LogOperation.valueOf("FIND_ENTITY_BY_FILTER"), getCustomMessageEntityId(result), MSG_SUCCESS, getResultJson(list.get(0)));
            CcuLog.d(TAG, "---findEntityByFilter##--original filter- result--" + result);
            return result;
        }else{
            sequenceLogsCallback.logInfo(LogLevel.INFO, LogOperation.valueOf("FIND_ENTITY_BY_FILTER"), MSG_ERROR_NO_ENTITY, MSG_FAILED);
            return "[]";
        }
    }

    private String getResultJson(HashMap hashMap){
        List<HashMap> list = new ArrayList<>();
        list.add(hashMap);
        String resultJson = new Gson().toJson(list);
        return resultJson;
    }

    public Double findValueByFilter(String filter, Object contextHelper) {
        CcuLog.d(TAG, "---findValueByFilter##--original filter-" + filter);
        sequenceLogsCallback.logInfo(LogLevel.INFO, LogOperation.valueOf("FETCH_VALUE_BY_ID"), filter, MSG_CALCULATING);
        List<HashMap> list = findByFilterCustom(filter, contextHelper);

        if(!list.isEmpty()){
            String entityId = list.get(0).get("id").toString();
            Double val = fetchValueByIdOnly(entityId, contextHelper);
            HashMap resultMap = new HashMap();
            resultMap.put("value", val);
            sequenceLogsCallback.logInfo(LogLevel.INFO, LogOperation.valueOf("FETCH_VALUE_BY_ID"), getCustomMessageValue(val), MSG_SUCCESS, getResultJson(resultMap));
            return val;
        }else{
            sequenceLogsCallback.logInfo(LogLevel.INFO, LogOperation.valueOf("FIND_BY_FILTER"), MSG_NO_VALUE, MSG_FAILED);
            return null;
        }
    }

    public String findById(String id, Object contextHelper) {
        sequenceLogsCallback.logInfo(LogLevel.INFO, LogOperation.valueOf("FIND_BY_ID"), "looking entity for id-->"+id, MSG_CALCULATING);
        CcuLog.d(TAG, "---findById##--id->" + id);
        HashMap result = CCUHsApi.getInstance().read(id);
        if(result != null){
            String entityResult = new Gson().toJson(result);
            sequenceLogsCallback.logInfo(LogLevel.INFO, LogOperation.valueOf("FETCH_VALUE_BY_ID"), getCustomMessageEntityId(entityResult), MSG_SUCCESS, getResultJson(result));
            return entityResult;
        }else{
            sequenceLogsCallback.logInfo(LogLevel.INFO, LogOperation.valueOf("FIND_BY_ID"), MSG_NO_ENTITY_FOR_ID, MSG_FAILED);
            return "";
        }
    }

    public Double fetchValueByHisReadMany(String id, Object contextHelper) {
        sequenceLogsCallback.logInfo(LogLevel.INFO, LogOperation.valueOf("FETCH_VALUE_BY_HIS_READ_MANY"), "looking value for id-->"+id, MSG_CALCULATING);
        return fetchValueByIdOnly(id, contextHelper);
    }

    public Double fetchValueByPointWriteMany(String id, Integer level, Object contextHelper) {
        sequenceLogsCallback.logInfo(LogLevel.INFO, LogOperation.valueOf("FETCH_VALUE_BY_POINT_WRITE_MANY"), "looking value for id-->"+id, MSG_CALCULATING);
        Double returnedValue = CCUHsApi.getInstance().readDefaultValByLevel(id, (int) (level.doubleValue()));
        HashMap resultMap = new HashMap();
        resultMap.put("value", returnedValue);
        sequenceLogsCallback.logInfo(LogLevel.INFO, LogOperation.valueOf("FETCH_VALUE_BY_POINT_WRITE_MANY"), "value is "+returnedValue, MSG_SUCCESS, getResultJson(resultMap));
        return returnedValue;
    }



    private static long convertStringToMilliseconds(String dateString, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        try {
            Date date = sdf.parse(dateString);
            return date.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
            return -1; // Handle the parsing error accordingly
        }
    }

    /**
     * This method will remove unwanted characters and clean the filter, then it can be used by ccu
     * there are some limitations why it is not generic
     * for -> domainName ==@buildingLimitMax | we need to remove @ before querying in CCU
     * same goes for port, dis and group
     * But
     * if filter contains id like shown below
     * id == @80dfaab5-9252-4c14-9bf0-1a6af67fc7bd
     * @ should not be removed from id or else query doesnt work
     *
     * @param filter
     * @param contextHelper
     * @return
     */
    private List<HashMap> findByFilterCustom(String filter, Object contextHelper) {
        CcuLog.d(TAG, "---findByFilterCustom##--original filter-"+filter);
        filter = modifyKVPairFromFilter(filter);

        CcuLog.d(TAG, "---findByFilter##--final filter for  readGrid->"+filter);
        HGrid hGrid = CCUHsApi.getInstance().readGrid(filter);
        List<HashMap> list = CCUHsApi.getInstance().HGridToListPlainString(hGrid);
        return list;
    }

    public void hisWrite(String id, Double val, Object contextHelper) {
        sequenceLogsCallback.logInfo(LogLevel.INFO, LogOperation.valueOf("HIS_WRITE"), "id-->"+id+"<--val-->"+val, MSG_CALCULATING);
        CcuLog.d(TAG, "---hisWrite##--id-"+id+"<--val-->"+val);
        CCUHsApi.getInstance().writeHisValById(id, val);
    }

    // if writable tag is there, return value of highest level
    // if writable tag is not there, check for his item and return latest value
    public Double fetchValueByIdOnly(String filter, Object contextHelper) {
        sequenceLogsCallback.logInfo(LogLevel.INFO, LogOperation.valueOf("FETCH_VALUE_BY_ID"), "looking value for id-->"+filter, MSG_CALCULATING);
        Double output = getValueByEntityId(filter);

        HashMap resultMap = new HashMap();
        resultMap.put("value", output);

        sequenceLogsCallback.logInfo(LogLevel.INFO, LogOperation.valueOf("FETCH_VALUE_BY_ID"), "value -->"+output, MSG_SUCCESS, getResultJson(resultMap));
        if(contextHelper instanceof V8Object){
            ((V8Object) contextHelper).close();
        }
        return output;
    }

    private Double getValueByEntityId(String entityId){
        Double output = null;
        try {
            if(CCUHsApi.getInstance().readMapById(entityId).get("writable") != null) {
                output = CCUHsApi.getInstance().readPointPriorityVal(entityId);
            }else{
                output = CCUHsApi.getInstance().readHisValById(entityId);
            }
            CcuLog.d(TAG, "---getValueByEntityId###--id-"+entityId+"<---->"+output);
        }catch (Exception e){
            e.printStackTrace();
        }
        return output;
    }

    // fetch values for given level for all point ids
    public String fetchValueByIds(String[] ids, String level, Object contextHelper) {
        CcuLog.d(TAG, "---fetchValueById##--id-"+ids.length + "<--level-->"+level);
        Double[] values = new Double[ids.length];
        for(int i=0;i<ids.length; i++){
            CcuLog.d(TAG, "---fetchValueById##--id-"+ids[i] + "<--level-->"+level);
            values[i] = CCUHsApi.getInstance().readHisValById(ids[i]).doubleValue();
        }
        return new Gson().toJson(values);
    }
    public Double[] fetchValueByIdsList(ArrayList<String> ids, String level, Object contextHelper) {
        CcuLog.d(TAG, "---fetchValueByIdThree ids list size##--id-" + ids.size() + "<--level-->" + level);
        Double[] values = new Double[ids.size()]; // Initialize the array to hold values

        for (int i = 0; i < ids.size(); i++) {
            String value = ids.get(i);    // Retrieve the string value
            CcuLog.d(TAG, "---fetchValueById##--id-" + value + "<--level-->" + level);
            values[i] = fetchValueOfPoint(value, level);//CCUHsApi.getInstance().readHisValById(value).doubleValue();
        }
        if(contextHelper instanceof V8Object){
            ((V8Object) contextHelper).close();
        }
        return values;//new Gson().toJson(values);
    }

    public V8Function fetchValueById(V8 v8) {
        if (fetchFunction == null) {
            fetchFunction = new V8Function(v8, (receiver, parameters) -> {
                V8Array resultArray = null;
                try {
                    int paramCount = parameters.length();
                    CcuLog.d(TAG, "---fetchValueById##--paramCount-" + paramCount + "<--parameters-->" + parameters);

                    if (paramCount == 2 && parameters.getType(0) == V8Value.STRING) {
                        String filter = parameters.getString(0);
                        Object contextHelper = parameters.get(1);
                        return fetchValueByIdOnly(filter, contextHelper);
                    }

                    if (paramCount == 3 && parameters.getType(0) == V8Value.V8_ARRAY) {
                        V8Array ids = parameters.getArray(0);
                        ArrayList<String> idList = convertV8ArrayToList(ids);
                        ids.close(); // Release the V8Array
                        if (ids.isReleased()) {
                            CcuLog.d(TAG, "-------------fetchValueById##--ids is released");
                        } else {
                            CcuLog.d(TAG, "-------------fetchValueById##--ids is not released");
                        }
                        String level = parameters.getString(1);
                        Object contextHelper = parameters.get(2);

                        Double[] resultArrayOfDoubles = fetchValueByIdsList(idList, level, contextHelper);
                        resultArray = convertDoubleArrayToV8Array(v8, resultArrayOfDoubles);
                        return resultArray;
                    }
                    sequenceLogsCallback.logInfo(LogLevel.INFO, LogOperation.valueOf("FETCH_VALUE_BY_ID"), "Invalid arguments for fetchValueById-->", MSG_FAILED);
                    CcuLog.d(TAG, "-------------fetchValueById##--Invalid arguments for fetchValueById"+ parameters);
                    return null;
                    //throw new IllegalArgumentException("Invalid arguments for fetchValueById");
                } finally {
                    CcuLog.d(TAG, "-------------fetchValueById##--release resources if something is not returned back to java script");
                }
            });
        }
        return fetchFunction;
    }

    private V8Array convertDoubleArrayToV8Array(V8 v8, Double[] doubleArray) {
        V8Array v8Array = new V8Array(v8);
        for (Double value : doubleArray) {
            v8Array.push(value);
        }
        return v8Array;
    }

    public ArrayList<String> convertV8ArrayToList(V8Array v8Array) {
        ArrayList<String> list = new ArrayList<>();

        for (int i = 0; i < v8Array.length(); i++) {
            // Assuming the V8Array contains strings
            if (v8Array.getType(i) == V8Array.STRING) {
                list.add(v8Array.getString(i));
            } else {
                throw new IllegalArgumentException("V8Array contains non-string element at index " + i);
            }
        }

        return list;
    }

    private Double fetchValueOfPoint(String pointId, String level){
        Double output = null;
        try {
            if(CCUHsApi.getInstance().readMapById(pointId).get("writable") != null) {
                output = fetchValueByGivenLevel(pointId, level);
            }else{
                output = (CCUHsApi.getInstance().readHisValById(pointId));
            }
            CcuLog.d(TAG, "---fetchValueOfPoint###--id-"+pointId+"<---->"+output);
        }catch (Exception e){
            e.printStackTrace();
        }
        return output;
    }

    private Double fetchValueByGivenLevel(String pointId, String level) {
        CcuLog.d(TAG, "---fetchValueByGivenLevel###--pointId-"+pointId+"<--level-->"+level);
        Double output;
        if (level.equalsIgnoreCase("highest")) {
            output = getValueByEntityId(pointId);
        } else if (level.equalsIgnoreCase("default")) {
            output = getValueByLevel(pointId, SYSTEM_DEFAULT_VAL_LEVEL-1);
            if (output == -1) {
                CcuLog.d(TAG, "---fetchValueByGivenLevel###--pointId-"+pointId+"<--level 17-->"+level + "<-output->"+output);
                output = getValueByLevel(pointId, SYSTEM_BUILDING_VAL_LEVEL-1);
                CcuLog.d(TAG, "---fetchValueByGivenLevel###--pointId-"+pointId+"<--level 16-->"+level + "<-output->"+output);
            }
            if (output == -1) {
                output = 0.0;
            }
        } else {
            output = getValueByLevel(pointId, Integer.parseInt(level) - 1);
            CcuLog.d(TAG, "---fetchValueByGivenLevel###--pointId-"+pointId+"<--level-->"+level + "<-output->"+output);
            if (output == -1) {
                output = 0.0;
            }
        }
        return output;
    }

    private Double getValueByLevel(String pointId, int levelInArray){
        ArrayList<HashMap> values = CCUHsApi.getInstance().readPoint(pointId);
        if (values != null && values.size() > 0) {
            HashMap valMap = values.get(levelInArray);
            return valMap.get("val") == null ? -1 : Double.parseDouble(valMap.get("val").toString());
        } else {
            return 0.0;
        }
    }

    public void printThis(String obj){
        CcuLog.d(TAG, "printThis---obj--"+obj);
    }


    public static String checkSiteRef(String inputString) {
        CcuLog.d(TAG, "checkSiteRef--"+inputString);
        //String inputString = "(heating and stage1 and system and cmd) and siteRef==@b74fac48-d267-43fd-9bc6-7df1ed244661 and ccuRef==@b74fac48-d267-43fd-9bc6-7df1ed244662";

        // Define the regular expression pattern to match the siteRef value
        Pattern pattern = Pattern.compile("siteRef==@([a-f0-9-]+)");

        // Create a Matcher object
        Matcher matcher = pattern.matcher(inputString);

        // Check if the pattern is found
        if (matcher.find()) {
            // Extract the siteRef value
            String siteRefValue = matcher.group(1);

            // Format the siteRef value and replace it in the original string
            return inputString.replace(matcher.group(), "siteRef==\"@" + siteRefValue + "\"");
        }
        return inputString;
    }

    public static String checkCcuRef(String inputString) {
        CcuLog.d(TAG, "checkCcuRef--"+inputString);
        //String inputString = "(heating and stage1 and system and cmd) and siteRef==@b74fac48-d267-43fd-9bc6-7df1ed244661 and ccuRef==@b74fac48-d267-43fd-9bc6-7df1ed244662";

        // Define the regular expression pattern to match the siteRef value
        Pattern pattern = Pattern.compile("ccuRef==@([a-f0-9-]+)");

        // Create a Matcher object
        Matcher matcher = pattern.matcher(inputString);

        // Check if the pattern is found
        if (matcher.find()) {
            // Extract the siteRef value
            String siteRefValue = matcher.group(1);

            // Format the siteRef value and replace it in the original string
            return inputString.replace(matcher.group(), "ccuRef==\"@" + siteRefValue + "\"");
        }
        return inputString;
    }

    public static String checkCcuPort(String inputString) {
        CcuLog.d(TAG, "checkCcuPort--"+inputString);
        inputString = inputString.replaceAll("@","");

        // Define the regular expression pattern to match the siteRef value
        Pattern pattern = Pattern.compile("port==([^\\\\s]+)");

        // Create a Matcher object
        Matcher matcher = pattern.matcher(inputString);

        // Check if the pattern is found
        if (matcher.find()) {
            // Extract the siteRef value
            String siteRefValue = matcher.group(1);

            // Format the siteRef value and replace it in the original string
            return inputString.replace(matcher.group(), "port==\"" + siteRefValue + "\"");
        }
        return inputString;
    }

    public static String checkCcuDis(String inputString) {
        inputString = inputString.replaceAll("@","");

        // Define the regular expression pattern to match the siteRef value
        Pattern pattern = Pattern.compile("dis==([^\\\\s]+)");

        // Create a Matcher object
        Matcher matcher = pattern.matcher(inputString);

        // Check if the pattern is found
        if (matcher.find()) {
            // Extract the siteRef value
            String siteRefValue = matcher.group(1);

            // Format the siteRef value and replace it in the original string
            String formattedString = inputString.replace(matcher.group(), "dis==\"" + siteRefValue + "\"");
            // Print the modified string
            return formattedString;
        }
        return inputString;
    }

    private String getCustomMessage(int size){
        return "list size is " + size;
    }

    private String getCustomMessageValue(Double value){
        return "value is " + value;
    }

    private String getCustomMessageEntityId(String entity){
        return "entity found with id ->" + entity;
    }

    private String getMsgNoEntity(){
        return "No entity found for given filter";
    }

    public void release() {
        if (fetchFunction != null) {
            fetchFunction.close(); // Release the V8Function
            fetchFunction = null; // Clear reference
        }

        if (v8Array != null) {
            v8Array.close(); // Release the V8Array
            v8Array = null; // Clear reference
        }
    }
}
