package a75f.io.logic.bo.building.modbus;

import static a75f.io.logic.bo.building.modbus.ModbusEquip.areAllWordsUpperCase;
import org.junit.Assert;
import org.junit.Test;

public class ModbusEquipTest {

    @Test
    public void testAreAllWordsUpperCase() {
        Assert.assertTrue(areAllWordsUpperCase("PAC"));
        Assert.assertFalse(areAllWordsUpperCase("watercooled"));
        Assert.assertTrue(areAllWordsUpperCase(""));
        Assert.assertTrue(areAllWordsUpperCase("BTU"));
        Assert.assertFalse(areAllWordsUpperCase("pAc"));
    }

}