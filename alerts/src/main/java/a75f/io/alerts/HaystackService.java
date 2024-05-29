package a75f.io.alerts;

import static a75f.io.alerts.AlertProcessor.TAG_CCU_ALERTS;

import com.google.gson.Gson;

import org.graalvm.polyglot.Value;
import org.projecthaystack.HDateTime;
import org.projecthaystack.HDateTimeRange;
import org.projecthaystack.HGrid;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;
import org.projecthaystack.HTimeZone;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HisItem;
import a75f.io.logger.CcuLog;

public class HaystackService {

    private HaystackService(){

    }

    private static HaystackService haystackServiceInstance = null;

    public static synchronized HaystackService getInstance() {
        if (haystackServiceInstance == null)
            haystackServiceInstance = new HaystackService();

        return haystackServiceInstance;
    }

    // return array of values
    public String hisReadManyInterpolateWithDateRange(Object hisPointIdObj, String startDate, String endDate, Object  contextHelper) {
        String hisPointId = hisPointIdObj.toString();
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
        return new Gson().toJson(values);
    }

    public String hisReadManyInterpolateWithInterval(String hisPointId, String rangeValue, String rangeUnit, Object contextHelper) {
        CcuLog.d(TAG_CCU_ALERTS, "---hisReadManyInterpolateWithInterval##$--hisPointId-" + hisPointId + "<--rangeValue-->" + rangeValue + "<--rangeUnit-->" + rangeUnit);
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
        return "";
    }

    // return array of values
    public String hisReadManyInterpolateWithInterval(String hisPointId, long minutes, Object contextHelper) {
        CcuLog.d(TAG_CCU_ALERTS, "---hisReadManyInterpolateWithInterval##--hisPointId-"+hisPointId + "<--minutes-->"+minutes);
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
        return new Gson().toJson(values);
    }

    public void pointWrite(String id, int level, double value, boolean override, Object contextHelper) {
        CcuLog.d(TAG_CCU_ALERTS, "---pointWrite##--id-"+id + "<--level-->"+level+"<--override-->"+override);
        CCUHsApi.getInstance().pointWrite(HRef.copy(id), level, "Seq_Demo", HNum.make(value), HNum.make(0));
    }

    // TODO: 15-04-2024 - needed this for sequence runner
    public void pointWriteMany(String[] ids, int level, double value, boolean override, Object contextHelper) {
        CcuLog.d(TAG_CCU_ALERTS, "---pointWriteMany##--id-"+ids.length + "<--level-->"+level+"<--override-->"+override);

        // TODO: 07-05-2024 - call below method once ameresh changes for batch write is ready
//        for(int i=0;i<ids.length; i++){
//            CCUHsApi.getInstance().pointWrite(HRef.copy(ids[i]), level, "Seq_Demo", HNum.make(value), HNum.make(0));
//        }
    }

    public boolean hisWriteMany(Map<String, Double> mapOfPointIds, Object contextHelper) {
        CcuLog.d(TAG_CCU_ALERTS, "---hisWriteMany##--id-"+mapOfPointIds.size());
        if(!mapOfPointIds.isEmpty()){
            for (Map.Entry<String, Double> entry : mapOfPointIds.entrySet()) {
                String key = entry.getKey();
                Double value = entry.getValue();
                CcuLog.d(TAG_CCU_ALERTS, "---hisWriteMany##--id-"+key + "<--value-->"+value);
                CCUHsApi.getInstance().writeHisValById(key, value);
            }
            return true;
        }
        return false;
    }

    public boolean hisWriteMany(String[] ids, Double value, Object contextHelper) {
        Map<String, Double> idValueMap = Arrays.stream(ids).collect(Collectors.toMap(key -> key, key -> value, (a, b) -> b));
        return hisWriteMany(idValueMap, contextHelper);
    }

    // returns list of entities
    public String findByFilter(String filter, Object contextHelper) {
        CcuLog.d(TAG_CCU_ALERTS, "---findByFilter##--original filter-" + filter);
        List<HashMap> list = findByFilterCustom(filter, contextHelper);
        return new Gson().toJson(list);
    }

    // returns single entity
    public String findEntityByFilter(String filter, Object contextHelper) {
        CcuLog.d(TAG_CCU_ALERTS, "---findEntityByFilter##--original filter-" + filter);
        List<HashMap> list = findByFilterCustom(filter, contextHelper);
        if(!list.isEmpty()){
            return new Gson().toJson(list.get(0));
        }else{
            return "";
        }
    }

    public Integer findValueByFilter(String filter, Object contextHelper) {
        CcuLog.d(TAG_CCU_ALERTS, "---findValueByFilter##--original filter-" + filter);
        List<HashMap> list = findByFilterCustom(filter, contextHelper);

        if(!list.isEmpty()){
            String entityId = list.get(0).get("id").toString();
            return fetchValueById(entityId, contextHelper);
        }else{
            return null;
        }
    }

    public String findById(String id, Object contextHelper) {
        return new Gson().toJson(CCUHsApi.getInstance().read(id));
    }

    public int fetchValueByHisReadMany(String id, Object contextHelper) {
        return fetchValueById(id, contextHelper);
    }

    public int fetchValueByPointWriteMany(String id, Double level, Object contextHelper) {
        return (int) (CCUHsApi.getInstance().readDefaultValByLevel(id, (int) (level.doubleValue()))).doubleValue();
    }



    public static long convertStringToMilliseconds(String dateString, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        try {
            Date date = sdf.parse(dateString);
            return date.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
            return -1; // Handle the parsing error accordingly
        }
    }

    private List<HashMap> findByFilterCustom(String filter, Object contextHelper) {
        CcuLog.d(TAG_CCU_ALERTS, "---findByFilterCustom##--original filter-"+filter);
        filter = removeFirstAndLastParentheses(filter);

          // below code need to be fixed
        if(filter.contains("port")){
            filter = filter.replaceAll("port==@", "port==");
        }
        if(filter.contains("dis")){
            filter = filter.replaceAll("dis==@", "dis==");
        }
        if(filter.contains("group")){
            filter = filter.replaceAll("group==@", "group==");
        }
        filter = fixInvertedCommas(filter);


        CcuLog.d(TAG_CCU_ALERTS, "---findByFilter##--final filter for  readGrid->"+filter);
        HGrid hGrid = CCUHsApi.getInstance().readGrid(filter);
        List<HashMap> list = CCUHsApi.getInstance().HGridToListPlainString(hGrid);
        return list;
    }

    public static String removeFirstAndLastParentheses(String input) {
        if (input.startsWith("(") && input.endsWith(")")) {
            return input.substring(1, input.length() - 1);
        } else {
            return input;
        }
    }

    public void hisWrite(String id, Double val, Object contextHelper) {
        CcuLog.d(TAG_CCU_ALERTS, "---hisWrite##--id-"+id+"<--val-->"+val);
        CCUHsApi.getInstance().writeHisValById(id, val);
    }


    // if writable tag is there, return value of highest level
    // if writable tag is not there, check for his item and return latest value
    public Integer fetchValueById(String filter, Object contextHelper) {
        Integer output = null;
        try {
            output = (int)(CCUHsApi.getInstance().readHisValById(filter).doubleValue());
            CcuLog.d(TAG_CCU_ALERTS, "---fetchValueById###--id-"+filter+"<---->"+output);
        }catch (Exception e){
            e.printStackTrace();
        }
        return output;
    }

    // fetch values for given level for all point ids
    // todo -- check level is missing
    public Value[] fetchValueById(String[] ids, String level, Object contextHelper) {
        CcuLog.d(TAG_CCU_ALERTS, "---fetchValueById##--id-"+ids.length + "<--level-->"+level);
        Value[] values = new Value[ids.length];
        for(int i=0;i<ids.length; i++){
            CcuLog.d(TAG_CCU_ALERTS, "---fetchValueById##--id-"+ids[i] + "<--level-->"+level);
            values[i] = Value.asValue((int)(CCUHsApi.getInstance().readHisValById(ids[i]).doubleValue()));
        }
        return values;
    }

    public void printThis(String obj){
        CcuLog.d(TAG_CCU_ALERTS, "printThis---obj--"+obj);
    }


    public static String checkSiteRef(String inputString) {
        CcuLog.d(TAG_CCU_ALERTS, "checkSiteRef--"+inputString);
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
        CcuLog.d(TAG_CCU_ALERTS, "checkCcuRef--"+inputString);
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
        CcuLog.d(TAG_CCU_ALERTS, "checkCcuPort--"+inputString);
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

    public static String fixInvertedCommas(String input) {
        // Define the pattern
        Pattern pattern = Pattern.compile("==([^\\s]+)");

        // Match the pattern against the input
        Matcher matcher = pattern.matcher(input);

        // StringBuffer to build the modified string
        StringBuffer result = new StringBuffer();

        // Find and replace the pattern
        while (matcher.find()) {
            // Extract the value after "=="
            String extractedValue = matcher.group(1);

            // Add inverted commas around the extracted value
            String replacement = "==\"" + extractedValue + "\"";

            // Replace the matched part with the modified value
            matcher.appendReplacement(result, replacement);
        }

        // Append the remaining part of the input
        matcher.appendTail(result);
        String finalResult = result.toString();
        // Print the modified string
        return finalResult;
    }
}
