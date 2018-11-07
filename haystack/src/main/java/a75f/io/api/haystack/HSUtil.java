package a75f.io.api.haystack;

import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HVal;

import java.util.Map;

/**
 * Created by samjithsadasivan on 10/12/18.
 */

public class HSUtil
{
    public static HDict mapToHDict(Map<String,Object> m){
        HDictBuilder b = new HDictBuilder();
        for (Map.Entry<String,Object> entry : m.entrySet())
        {
            if (entry.getValue() instanceof  HVal)
            {
                b.add(entry.getKey(), (HVal) entry.getValue());
            } else {
                b.add(entry.getKey(), (String) entry.getValue());
            }
        }
        return b.toDict();
    }
}
