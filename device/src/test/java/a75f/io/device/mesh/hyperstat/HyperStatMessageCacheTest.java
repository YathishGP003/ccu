package a75f.io.device.mesh.hyperstat;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;

import a75f.io.device.HyperStat;

public class HyperStatMessageCacheTest extends TestCase {

    HyperStat.HyperStatControlsMessage_t.Builder hyperStatControlsMessage;
    HyperStatMessageCache dummyCache;

    @Before
    public void setUp() throws Exception {
        hyperStatControlsMessage = HyperStat.HyperStatControlsMessage_t.newBuilder();
        hyperStatControlsMessage.setReset(false);
        hyperStatControlsMessage.setAnalogOut1
                (HyperStat.HyperStatAnalogOutputControl_t.newBuilder().setPercent(10).build());
        hyperStatControlsMessage.setAnalogOut2
                (HyperStat.HyperStatAnalogOutputControl_t.newBuilder().setPercent(30).build());
        hyperStatControlsMessage.setAnalogOut3
                (HyperStat.HyperStatAnalogOutputControl_t.newBuilder().setPercent(10).build());
        hyperStatControlsMessage.setConditioningMode(HyperStat.HyperStatConditioningMode_e.HYPERSTAT_CONDITIONING_MODE_AUTO);
        hyperStatControlsMessage.setConditioningModeValue(2);
        hyperStatControlsMessage.setFanSpeed(HyperStat.HyperStatFanSpeed_e.HYPERSTAT_FAN_SPEED_AUTO);
        hyperStatControlsMessage.setFanSpeedValue(2);
        hyperStatControlsMessage.setOperatingMode(HyperStat.HyperStatOperatingMode_e.HYPERSTAT_OPERATING_MODE_COOLING);
        hyperStatControlsMessage.setOperatingModeValue(2);
        hyperStatControlsMessage.setSetTempCooling(69);
        hyperStatControlsMessage.setSetTempHeating(76);
        hyperStatControlsMessage.setRelay1(true);
        hyperStatControlsMessage.setRelay2(true);
        hyperStatControlsMessage.setRelay3(true);
        hyperStatControlsMessage.setRelay4(true);
        hyperStatControlsMessage.setRelay5(true);
        hyperStatControlsMessage.setRelay6(true);
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        hyperStatControlsMessage.clear();
    }

    public void testControlCheck(){
       boolean val =  HyperStatMessageCache.getInstance().checkControlMessage(1001,hyperStatControlsMessage.build());
        assertFalse(val);
    }
}