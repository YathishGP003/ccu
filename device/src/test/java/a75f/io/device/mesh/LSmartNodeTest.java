package a75f.io.device.mesh;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.mock.MockCcuHsApi;
import a75f.io.device.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;
import a75f.io.device.serial.DamperActuator_t;
import a75f.io.device.serial.SmartNodeSettings_t;
import a75f.io.logic.bo.building.definitions.DamperType;
import a75f.io.logic.bo.building.definitions.ReheatType;

public class LSmartNodeTest extends TestCase {

    CCUHsApi hayStack;
    String NODE_ADDRESS = "1001";

    @Before
    public void setUp() {
        hayStack = new MockCcuHsApi();
    }

    @After
    public void tearDown() throws Exception {
        hayStack.tagsDb.getBoxStore().close();
    }

    @Test
    public void testSN(){
        assertTrue(LSmartNode.getControlMessageforEquip(NODE_ADDRESS, hayStack) != null);
    }
    @Test
    public void testSettingMsgDamperTypeConfig(){
        SmartNodeSettings_t seedMessage = new SmartNodeSettings_t();

        LSmartNode.setupDamperActuator(seedMessage, DamperType.ZeroToTenV.ordinal(), DamperType.ZeroToTenV.ordinal(), 0,"dab");
        Assert.assertSame(seedMessage.outsideAirOptimizationDamperActuatorType.get(), DamperActuator_t.DAMPER_ACTUATOR_0_10V);
        Assert.assertSame(seedMessage.returnAirDamperActuatorType.get(), DamperActuator_t.DAMPER_ACTUATOR_0_10V);

        LSmartNode.setupDamperActuator(seedMessage,DamperType.TwoToTenV.ordinal(), DamperType.TwoToTenV.ordinal(), 0,"dab");
        Assert.assertSame(seedMessage.outsideAirOptimizationDamperActuatorType.get(), DamperActuator_t.DAMPER_ACTUATOR_2_10V);
        Assert.assertSame(seedMessage.returnAirDamperActuatorType.get(), DamperActuator_t.DAMPER_ACTUATOR_2_10V);

        LSmartNode.setupDamperActuator(seedMessage,DamperType.TenToTwov.ordinal(), DamperType.TenToTwov.ordinal(), 0,"dab");
        Assert.assertSame(seedMessage.outsideAirOptimizationDamperActuatorType.get(), DamperActuator_t.DAMPER_ACTUATOR_10_2V);
        Assert.assertSame(seedMessage.returnAirDamperActuatorType.get(), DamperActuator_t.DAMPER_ACTUATOR_10_2V);

        LSmartNode.setupDamperActuator(seedMessage,DamperType.TenToZeroV.ordinal(), DamperType.TenToZeroV.ordinal(), 0,"dab");
        Assert.assertSame(seedMessage.outsideAirOptimizationDamperActuatorType.get(), DamperActuator_t.DAMPER_ACTUATOR_10_0V);
        Assert.assertSame(seedMessage.returnAirDamperActuatorType.get(), DamperActuator_t.DAMPER_ACTUATOR_10_0V);

        LSmartNode.setupDamperActuator(seedMessage,DamperType.MAT.ordinal(), DamperType.MAT.ordinal(), 0,"dab");
        Assert.assertSame(seedMessage.outsideAirOptimizationDamperActuatorType.get(), DamperActuator_t.DAMPER_ACTUATOR_MAT);
        Assert.assertSame(seedMessage.returnAirDamperActuatorType.get(), DamperActuator_t.DAMPER_ACTUATOR_MAT);

        LSmartNode.setupDamperActuator(seedMessage,DamperType.MAT.ordinal(), DamperType.MAT.ordinal(), ReheatType.TwoToTenV.ordinal(),"dab");
        Assert.assertSame(seedMessage.outsideAirOptimizationDamperActuatorType.get(), DamperActuator_t.DAMPER_ACTUATOR_MAT);
        Assert.assertSame(seedMessage.returnAirDamperActuatorType.get(), DamperActuator_t.DAMPER_ACTUATOR_2_10V);

        LSmartNode.setupDamperActuator(seedMessage,DamperType.MAT.ordinal(), DamperType.MAT.ordinal(), ReheatType.TenToTwov.ordinal(),"dab");
        Assert.assertSame(seedMessage.outsideAirOptimizationDamperActuatorType.get(), DamperActuator_t.DAMPER_ACTUATOR_MAT);
        Assert.assertSame(seedMessage.returnAirDamperActuatorType.get(), DamperActuator_t.DAMPER_ACTUATOR_10_2V);

        LSmartNode.setupDamperActuator(seedMessage,DamperType.MAT.ordinal(), DamperType.MAT.ordinal(), ReheatType.TenToZeroV.ordinal(),"dab");
        Assert.assertSame(seedMessage.outsideAirOptimizationDamperActuatorType.get(), DamperActuator_t.DAMPER_ACTUATOR_MAT);
        Assert.assertSame(seedMessage.returnAirDamperActuatorType.get(), DamperActuator_t.DAMPER_ACTUATOR_10_0V);

        LSmartNode.setupDamperActuator(seedMessage,DamperType.MAT.ordinal(), DamperType.MAT.ordinal(), ReheatType.Pulse.ordinal(),"dab");
        Assert.assertSame(seedMessage.outsideAirOptimizationDamperActuatorType.get(), DamperActuator_t.DAMPER_ACTUATOR_MAT);
        Assert.assertSame(seedMessage.returnAirDamperActuatorType.get(), DamperActuator_t.DAMPER_ACTUATOR_PULSED);

        LSmartNode.setupDamperActuator(seedMessage,DamperType.MAT.ordinal(), DamperType.MAT.ordinal(), ReheatType.OneStage.ordinal(),"dab");
        Assert.assertSame(seedMessage.outsideAirOptimizationDamperActuatorType.get(), DamperActuator_t.DAMPER_ACTUATOR_MAT);
        Assert.assertSame(seedMessage.returnAirDamperActuatorType.get(), DamperActuator_t.DAMPER_ACTUATOR_Staged);
        
        LSmartNode.setupDamperActuator(seedMessage,DamperType.MAT.ordinal(), DamperType.MAT.ordinal(), ReheatType.TwoStage.ordinal(),"dab");
        Assert.assertSame(seedMessage.outsideAirOptimizationDamperActuatorType.get(), DamperActuator_t.DAMPER_ACTUATOR_MAT);
        Assert.assertSame(seedMessage.returnAirDamperActuatorType.get(), DamperActuator_t.DAMPER_ACTUATOR_Staged);

    }
}