package org.projecthaystack;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by samjithsadasivan on 9/17/18.
 */

public class MapImpl<K,V> extends HDict
{
    MapImpl(HashMap<K,V> map) { this.map = map; }
    
    public int size() { return map.size(); }
    
    public HVal get(String name, boolean checked)
    {
        HVal val = (HVal)map.get(name);
        if (val != null) return val;
        if (!checked) return null;
        throw new UnknownNameException(name);
    }
    
    public Iterator iterator() { return map.entrySet().iterator(); }
    
    private final HashMap<K,V> map;
}
