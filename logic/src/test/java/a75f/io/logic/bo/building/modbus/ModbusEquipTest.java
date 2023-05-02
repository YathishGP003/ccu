package a75f.io.logic.bo.building.modbus;

import static a75f.io.logic.bo.building.modbus.ModbusEquip.isEquipTypeInUpperCase;
import org.junit.Assert;
import org.junit.Test;

public class ModbusEquipTest {

    @Test
    public void testAreAllWordsUpperCase() {
        Assert.assertTrue(isEquipTypeInUpperCase("PAC"));
        Assert.assertFalse(isEquipTypeInUpperCase("watercooled"));
        Assert.assertTrue(isEquipTypeInUpperCase(""));
        Assert.assertTrue(isEquipTypeInUpperCase("BTU"));
        Assert.assertFalse(isEquipTypeInUpperCase("pAc"));
    }

}