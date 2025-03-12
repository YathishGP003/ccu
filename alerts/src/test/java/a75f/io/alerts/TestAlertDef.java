package a75f.io.alerts;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by samjithsadasivan on 4/24/18.
 */

/**
 * domainName == \"relay3\" and system
 * zone and (current or space) and temp
 * zone and pid and equip
 * system and occupied and not oao
 * ((zone and vav and equip) or (oao and equip))
 * ((domainName==@analog1Out) or (port==@ANALOG_OUT_ONE))
 * id==@8f4d814f-6712-4783-afcd-4119138d5d0c
 * condensate
 */

public class TestAlertDef
{
    @Test
    public void test_fixInvertedComma() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String ip1 = "domainName==@buildingLimitMax";
        String ip2 = "domainName==\"buildingLimitMax\"";

        // Use reflection to access the private method
        Method method = HaystackService.class.getDeclaredMethod("fixInvertedCommas", String.class);
        method.setAccessible(true);

        // Invoke the method and get the result
        String result = (String) method.invoke(null, ip1);
        System.out.println(result);
        // Verify the result
        assertEquals(ip2, result);
    }
}
