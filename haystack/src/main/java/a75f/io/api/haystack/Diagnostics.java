package a75f.io.api.haystack;

import java.util.HashMap;

public class Diagnostics
{
    //The time in nano seconds
    public static HashMap<String, Long> methodTracing = new HashMap<String, Long>();
    
    
    public static void startMethodTracing(String key)
    {
        methodTracing.put(key, System.nanoTime());
    }
    
    
    
    
    public static void stopMethodTracing(String key)
    {
        Long duration = System.nanoTime() - methodTracing.remove(key);
        System.out.println("Key: " + key +  " duration nano: " + duration + " milli: " + duration / 1000000);
    }
}
