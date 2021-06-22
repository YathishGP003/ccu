package a75f.io.alerts;

import org.projecthaystack.ParseException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Point;
import a75f.io.logger.CcuLog;

/***
 *  Alert messages shall have variables with prefix # to be formatted with runtime values while
 *  generating the message.
 *  Expected format is
 *  #entity-(name/val)-conditional-number
 *
 *  Example variable definitions
 *  #equipname1  => Use equip's name which satisfies the set conditionals here
 *  #pointval3  => Evaluate 3rd conditional use points value here.
 *  #pointname1  => Use first conditional point name here.
 *  #condval2   => Use check value given in 2nd conditional.
 *  #floorname
 *  #zonename2
 *
 */
public class AlertFormatter
{
    
    public static HashSet<String> entitySet = new HashSet<>();
    static
    {
        entitySet.add("point");
        entitySet.add("equip");
        entitySet.add("zone");
        entitySet.add("floor");
        entitySet.add("site");
        entitySet.add("system");
        entitySet.add("cond");
        entitySet.add("ccu");
        entitySet.add("old");
        entitySet.add("offset");
        entitySet.add("present");
    }
    
    public static String getFormattedMessage(AlertDefinition def) {
        String message = def.alert.mMessage;
        StringTokenizer t = new StringTokenizer(def.alert.mMessage);
        while(t.hasMoreTokens())
        {
            String token = t.nextToken();
            if (token.startsWith("#")) {
                try {
                    message = message.replace(token, parseToken(def, token, null));
                }
                catch (NullPointerException ex) {
                    // while parsing message.
                    // last ditch solution.  Just use token as is.
                    message = token;
                }
            }
        }
        CcuLog.d("CCU_ALERTS","  Alert Formatted Message "+message);
        return message;
    }
    
    public static String getFormattedMessage(AlertDefinition def, String pointId) {
        String message = def.alert.mMessage;
        StringTokenizer t = new StringTokenizer(def.alert.mMessage);
        while(t.hasMoreTokens())
        {
            String token = t.nextToken();
            if (token.startsWith("#")) {
                message = message.replace(token, parseToken(def, token, pointId));
            }
        }
        CcuLog.d("CCU_ALERTS","  Alert Formatted Message "+message);
        return message;
    }
    
    /*
     * This method is problematic in that 'point' can sometimes be null, but other times null
     * will cause a crash.  'point' being nonnull is neither fully required nor allowed.
     *
     * The strategy here is to require the client to be careful, and suggest a NPE catch block
     * if 'point' might be null.
     */
    private static String parseToken(AlertDefinition def, String token, String point) throws NullPointerException {
        token.replace("#","");
        int n = getConditionalIndex(token);
        Conditional c = n > 0 ? def.conditionals.get(n-1) : null;
        CCUHsApi hs = CCUHsApi.getInstance();
        Point p = null;
        if ( c != null || point != null) {
            p = new Point.Builder().setHashMap(c != null ? hs.read(c.key) : hs.readMapById(point)).build();
            if (p.getId() != null && point != null && !p.getId().equals(point)) {
                p = new Point.Builder().setHashMap(hs.readMapById(point)).build();
            }
        }
        switch (getEntity(token)) {
            case "point":
                if (c == null) {
                    return token.contains("name") ? p.getDisplayName() : hs.readHisValById(p.getId()).toString();
                } else
                {
                    return token.contains("name") ? p.getDisplayName() : ((c.grpOperation.contains("min") || c.grpOperation.contains("max") || c.grpOperation.contains("average")) ? String.valueOf(c.resVal) : hs.readHisValById(p.getId()).toString());
                }
            case "present":
                for (Conditional d : def.conditionals) {
                    for (PointVal v : d.presentValue) {
                        if (v.id.equals(point)) {
                            return String.valueOf(v.val);
                        }
                    }
                }
            case "old":
                for (Conditional d : def.conditionals) {
                    for (PointVal v : d.lastValue) {
                        if (v.id.equals(point)) {
                            return String.valueOf(v.val);
                        }
                    }
                }
            case "offset":
                return def.offset;
            case "equip":
                HashMap q = hs.readMapById(p.getEquipRef());
                return q.get("dis").toString();
            case "zone":
                HashMap z = hs.readMapById(p.getRoomRef());
                return z.get("dis").toString();
            case "floor":
                HashMap f = hs.readMapById(p.getFloorRef());
                return f.get("dis").toString();
            case "site":
                return hs.getSiteName();
            case "system":
                HashMap system = hs.read("system and equip");
                return system.get("dis").toString();
            case "ccu":
                HashMap ccu = hs.read("device and ccu");
                return ccu.get("dis").toString();
            case "cond":
                CcuLog.d("CCU_ALERTS"," Read cond val point "+point);
                //Cond vals can be of three forms
                // 1. value fetched for a unique query or absolute value -> use c.val
                // 2. point id for key - > run query to fetch cond val
                // 3. point id to get value -> run through pointValList to find
                // the value for a point that triggered the alert.
                if (c == null) {
                    for (Conditional d : def.conditionals) {
                        for (PointVal v : d.pointValList) {
                            if (v.id.equals(point)) {
                                return String.valueOf(v.val);
                            }
                        }
                    }
                }
                return point != null && !c.isValueNumeric() ? String.valueOf(hs.readHisValByQuery(c.value)) : c.val;
        }
        return "";
    }
    
    public static int getConditionalIndex(String token) {
        int num = 0;
        for (char ch : token.toCharArray()) {
            if (ch >= '0' && ch <= '9') {
                num = (num > 0 ? num * 10 + Character.getNumericValue(ch): Character.getNumericValue(ch));
            }
        }
        return num;
    }
    
    public static String getEntity(String token) {
        Iterator<String> iterator = entitySet.iterator();
        while(iterator.hasNext())
        {
            String entity = iterator.next();
            if (token.contains(entity)) {
                return entity;
            }
        }
        return "";
    }
    
    
}
