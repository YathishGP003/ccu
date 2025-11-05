package a75f.io.device.mesh;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.mock.MockCcuHsApi;
import a75f.io.device.serial.DamperActuator_t;
import a75f.io.device.serial.SmartNodeSettings_t;
import a75f.io.logic.bo.building.definitions.DamperType;

public class LSmartNodeTest {

    CCUHsApi hayStack;
    String NODE_ADDRESS = "1001";

    @Before
    public void setUp() {
        hayStack = new MockCcuHsApi();
    }

    @After
    public void tearDown() {
        hayStack.tagsDb.getBoxStore().close();
    }

    @Test
    public void testSN(){
        //Assert.assertNotNull(LSmartNode.getControlMessageforEquip(NODE_ADDRESS, hayStack));
    }
    @Test
    public void testSettingMsgDamperTypeConfig(){
        SmartNodeSettings_t seedMessage = new SmartNodeSettings_t();

        LSmartNode.setupDamperActuator(seedMessage, DamperType.ZeroToTenV.ordinal(), DamperType.ZeroToTenV.ordinal(), -1,"dab");
        Assert.assertSame(DamperActuator_t.DAMPER_ACTUATOR_0_10V, seedMessage.outsideAirOptimizationDamperActuatorType.get());

        LSmartNode.setupDamperActuator(seedMessage, DamperType.TwoToTenV.ordinal(), DamperType.ZeroToTenV.ordinal(), -1,"dab");
        Assert.assertSame(DamperActuator_t.DAMPER_ACTUATOR_2_10V, seedMessage.outsideAirOptimizationDamperActuatorType.get());

        LSmartNode.setupDamperActuator(seedMessage, DamperType.TenToTwov.ordinal(), DamperType.ZeroToTenV.ordinal(), -1,"dab");
        Assert.assertSame(DamperActuator_t.DAMPER_ACTUATOR_10_2V, seedMessage.outsideAirOptimizationDamperActuatorType.get());

        LSmartNode.setupDamperActuator(seedMessage, DamperType.TenToZeroV.ordinal(), DamperType.ZeroToTenV.ordinal(), -1,"dab");
        Assert.assertSame(DamperActuator_t.DAMPER_ACTUATOR_10_0V, seedMessage.outsideAirOptimizationDamperActuatorType.get());

        LSmartNode.setupDamperActuator(seedMessage, DamperType.MAT.ordinal(), DamperType.ZeroToTenV.ordinal(), -1,"dab");
        Assert.assertSame(DamperActuator_t.DAMPER_ACTUATOR_MAT, seedMessage.outsideAirOptimizationDamperActuatorType.get());

        LSmartNode.setupDamperActuator(seedMessage, DamperType.ZeroToTenV.ordinal(), DamperType.ZeroToTenV.ordinal(), -1,"dab");
        Assert.assertSame(DamperActuator_t.DAMPER_ACTUATOR_0_10V, seedMessage.returnAirDamperActuatorType.get());

        LSmartNode.setupDamperActuator(seedMessage, DamperType.TwoToTenV.ordinal(), DamperType.TwoToTenV.ordinal(), -1,"dab");
        Assert.assertSame(DamperActuator_t.DAMPER_ACTUATOR_2_10V, seedMessage.returnAirDamperActuatorType.get());

        LSmartNode.setupDamperActuator(seedMessage, DamperType.TenToTwov.ordinal(), DamperType.TenToTwov.ordinal(), -1,"dab");
        Assert.assertSame(DamperActuator_t.DAMPER_ACTUATOR_10_2V, seedMessage.returnAirDamperActuatorType.get());

        LSmartNode.setupDamperActuator(seedMessage, DamperType.TenToZeroV.ordinal(), DamperType.TenToZeroV.ordinal(), -1,"dab");
        Assert.assertSame(DamperActuator_t.DAMPER_ACTUATOR_10_0V, seedMessage.returnAirDamperActuatorType.get());

        LSmartNode.setupDamperActuator(seedMessage, DamperType.MAT.ordinal(), DamperType.MAT.ordinal(), -1,"dab");
        Assert.assertSame(DamperActuator_t.DAMPER_ACTUATOR_MAT, seedMessage.returnAirDamperActuatorType.get());

        LSmartNode.setupDamperActuator(seedMessage, DamperType.ZeroToTenV.ordinal(), DamperType.MAT.ordinal(), 0,"dab");
        Assert.assertSame(DamperActuator_t.DAMPER_ACTUATOR_MAT_REHEAT_0_10V, seedMessage.returnAirDamperActuatorType.get());

        LSmartNode.setupDamperActuator(seedMessage, DamperType.ZeroToTenV.ordinal(), DamperType.MAT.ordinal(), 1,"dab");
        Assert.assertSame(DamperActuator_t.DAMPER_ACTUATOR_MAT_REHEAT_2_10V, seedMessage.returnAirDamperActuatorType.get());

        LSmartNode.setupDamperActuator(seedMessage, DamperType.ZeroToTenV.ordinal(), DamperType.MAT.ordinal(), 2,"dab");
        Assert.assertSame(DamperActuator_t.DAMPER_ACTUATOR_MAT_REHEAT_10_2V, seedMessage.returnAirDamperActuatorType.get());

        LSmartNode.setupDamperActuator(seedMessage, DamperType.ZeroToTenV.ordinal(), DamperType.MAT.ordinal(), 3,"dab");
        Assert.assertSame(DamperActuator_t.DAMPER_ACTUATOR_MAT_REHEAT_10_0V, seedMessage.returnAirDamperActuatorType.get());

        LSmartNode.setupDamperActuator(seedMessage, DamperType.ZeroToTenV.ordinal(), DamperType.MAT.ordinal(), 4,"dab");
        Assert.assertSame(DamperActuator_t.DAMPER_ACTUATOR_MAT_REHEAT_PULSED, seedMessage.returnAirDamperActuatorType.get());

        LSmartNode.setupDamperActuator(seedMessage, DamperType.ZeroToTenV.ordinal(), DamperType.MAT.ordinal(), 5,"dab");
        Assert.assertSame(DamperActuator_t.DAMPER_ACTUATOR_MAT_REHEAT_ONE_STAGE, seedMessage.returnAirDamperActuatorType.get());

        LSmartNode.setupDamperActuator(seedMessage, DamperType.ZeroToTenV.ordinal(), DamperType.MAT.ordinal(), 6,"dab");
        Assert.assertSame(DamperActuator_t.DAMPER_ACTUATOR_MAT_REHEAT_TWO_STAGE, seedMessage.returnAirDamperActuatorType.get());

        LSmartNode.setupDamperActuator(seedMessage, DamperType.ZeroToTenV.ordinal(), DamperType.ZeroToTenV.ordinal(), 1,"dab");
        Assert.assertSame(DamperActuator_t.DAMPER_ACTUATOR_NOT_PRESENT, seedMessage.returnAirDamperActuatorType.get());

        LSmartNode.setupDamperActuator(seedMessage, DamperType.ZeroToTenV.ordinal(), DamperType.ZeroToTenV.ordinal(), 2,"dab");
        Assert.assertSame(DamperActuator_t.DAMPER_ACTUATOR_NOT_PRESENT, seedMessage.returnAirDamperActuatorType.get());

        LSmartNode.setupDamperActuator(seedMessage, DamperType.ZeroToTenV.ordinal(), DamperType.ZeroToTenV.ordinal(), 3,"dab");
        Assert.assertSame(DamperActuator_t.DAMPER_ACTUATOR_NOT_PRESENT, seedMessage.returnAirDamperActuatorType.get());

        LSmartNode.setupDamperActuator(seedMessage, DamperType.ZeroToTenV.ordinal(), DamperType.ZeroToTenV.ordinal(), 5,"dab");
        Assert.assertSame(DamperActuator_t.DAMPER_ACTUATOR_0_10V_REHEAT_ONE_STAGE, seedMessage.returnAirDamperActuatorType.get());

        LSmartNode.setupDamperActuator(seedMessage, DamperType.ZeroToTenV.ordinal(), DamperType.ZeroToTenV.ordinal(), 6,"dab");
        Assert.assertSame(DamperActuator_t.DAMPER_ACTUATOR_0_10V_REHEAT_TWO_STAGE, seedMessage.returnAirDamperActuatorType.get());

        LSmartNode.setupDamperActuator(seedMessage, DamperType.ZeroToTenV.ordinal(), DamperType.TwoToTenV.ordinal(), 5,"dab");
        Assert.assertSame(DamperActuator_t.DAMPER_ACTUATOR_2_10V_REHEAT_ONE_STAGE, seedMessage.returnAirDamperActuatorType.get());

        LSmartNode.setupDamperActuator(seedMessage, DamperType.ZeroToTenV.ordinal(), DamperType.TwoToTenV.ordinal(), 6,"dab");
        Assert.assertSame(DamperActuator_t.DAMPER_ACTUATOR_2_10V_REHEAT_TWO_STAGE, seedMessage.returnAirDamperActuatorType.get());

        LSmartNode.setupDamperActuator(seedMessage, DamperType.ZeroToTenV.ordinal(), DamperType.TenToTwov.ordinal(), 5,"dab");
        Assert.assertSame(DamperActuator_t.DAMPER_ACTUATOR_10_2V_REHEAT_ONE_STAGE, seedMessage.returnAirDamperActuatorType.get());

        LSmartNode.setupDamperActuator(seedMessage, DamperType.ZeroToTenV.ordinal(), DamperType.TenToTwov.ordinal(), 6,"dab");
        Assert.assertSame(DamperActuator_t.DAMPER_ACTUATOR_10_2V_REHEAT_TWO_STAGE, seedMessage.returnAirDamperActuatorType.get());

        LSmartNode.setupDamperActuator(seedMessage, DamperType.ZeroToTenV.ordinal(), DamperType.TenToZeroV.ordinal(), 5,"dab");
        Assert.assertSame(DamperActuator_t.DAMPER_ACTUATOR_10_0V_REHEAT_ONE_STAGE, seedMessage.returnAirDamperActuatorType.get());

        LSmartNode.setupDamperActuator(seedMessage, DamperType.ZeroToTenV.ordinal(), DamperType.TenToZeroV.ordinal(), 6,"dab");
        Assert.assertSame(DamperActuator_t.DAMPER_ACTUATOR_10_0V_REHEAT_TWO_STAGE, seedMessage.returnAirDamperActuatorType.get());
    }
}