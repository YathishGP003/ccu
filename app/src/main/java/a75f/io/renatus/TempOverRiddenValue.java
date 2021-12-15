/*Created by Aniket on 20/10/2021
This class is used to to store and retreive Hashmap values in Temporary Overrides specifications. */

package a75f.io.renatus;

import org.apache.commons.collections.iterators.EntrySetMapIterator;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class TempOverRiddenValue {
    private static TempOverRiddenValue tempOverRiddenValue;
    private Map<String, String> originalValues;
    private Map<String, String> overriddenValues;

    private TempOverRiddenValue(){
        originalValues = new HashMap<>();
        overriddenValues = new HashMap<>();
    }

    public static TempOverRiddenValue getInstance(){
        if(tempOverRiddenValue == null){
            tempOverRiddenValue = new TempOverRiddenValue();
        }
        return tempOverRiddenValue;
    }

    public void addOriginalValues(String col, String Val){
        originalValues.put(col, Val);
    }

    public Map<String, String> getOriginalValues() {
        return originalValues;
    }

    public void clear(){
        originalValues.clear();
        overriddenValues.clear();
    }
    public Set<Map.Entry<String,String>> getAllItems() {
        return originalValues.entrySet();
    }

    public void addOverRiddenValues(String valueToOverride, String selectedSpinnerItem) {
        overriddenValues.put(valueToOverride, selectedSpinnerItem);
    }

    public Map<String, String> getOverriddenValues() {
        return overriddenValues;
    }
}
