package a75f.io.logic.pubnub;

import org.junit.Assert;
import org.junit.Test;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class DataSyncHandlerTest {
    @Test
    public void testIsCloudScheduleHasLatestValue() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        /*Method method = DataSyncHandler.class.getDeclaredMethod("isCloudScheduleHasLatestValue", HDict.class, String.class);
        method.setAccessible(true);

        HDictBuilder scheduleDictCase1 = new HDictBuilder().add("lastModifiedDateTime","2023-04-02T09:53:00.000Z UTC");
        String lastModifiedDateTimeInCloudCase1 = "2023-04-02T15:22:00.000+05:30 Kolkata";
        HDictBuilder scheduleDictCase2 = new HDictBuilder().add("lastModifiedDateTime","2023-04-02T09:50:00.000Z UTC");
        String lastModifiedDateTimeInCloudCase2 = "2023-04-02T15:22:00.000+05:30 Kolkata";

        DataSyncHandler dataSyncHandler = new DataSyncHandler();
        boolean isLocalEntityHasLatestValue = (boolean) method.invoke(dataSyncHandler, scheduleDictCase1.toDict(), lastModifiedDateTimeInCloudCase1);
        boolean isLocalEntityHasLatestValueCase2 = (boolean) method.invoke(dataSyncHandler, scheduleDictCase2.toDict(), lastModifiedDateTimeInCloudCase2);
        Assert.assertFalse(isLocalEntityHasLatestValue);
        Assert.assertTrue(isLocalEntityHasLatestValueCase2);*/
    }
}