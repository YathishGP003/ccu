package a75f.io.device.mesh;

import android.util.Log;

import junit.framework.TestCase;

import org.junit.Assert;

public class DeviceUtilTestcases extends TestCase {

    public void testGetValidDesiredCoolingTemp() {

        double res = DeviceUtil.getValidDesiredCoolingTemp(66, 1, 78, 70);
        System.out.println("Result 66 = "+res);
         res = DeviceUtil.getValidDesiredCoolingTemp(66.5, 1, 78, 70);
        System.out.println("Result 66.5 =  "+res);
        res = DeviceUtil.getValidDesiredCoolingTemp(67, 1, 78, 70);
        System.out.println("Result 67 =  "+res);
        res = DeviceUtil.getValidDesiredCoolingTemp(67.5, 1, 78, 70);
        System.out.println("Result 67.5 = "+res);
        res = DeviceUtil.getValidDesiredCoolingTemp(68, 1, 78, 70);
        System.out.println("Result 68 = "+res);
        res = DeviceUtil.getValidDesiredCoolingTemp(68.5, 1, 78, 70);
        System.out.println("Result 68.5 = "+res);
        res = DeviceUtil.getValidDesiredCoolingTemp(69, 1, 78, 70);
        System.out.println("Result 69 = "+res);
        res = DeviceUtil.getValidDesiredCoolingTemp(69.5, 1, 78, 70);
        System.out.println("Result 69.5 = "+res);
        res = DeviceUtil.getValidDesiredCoolingTemp(70, 1, 78, 70);
        System.out.println("Result 70 = "+res);
        res = DeviceUtil.getValidDesiredCoolingTemp(70.5, 1, 78, 70);
        System.out.println("Result 70.5 = "+res);
        res = DeviceUtil.getValidDesiredCoolingTemp(71, 1, 78, 70);
        System.out.println("Result 71 = "+res);
        res = DeviceUtil.getValidDesiredCoolingTemp(71.5, 1, 78, 70);
        System.out.println("Result 71.5 = "+res);

        res = DeviceUtil.getValidDesiredCoolingTemp(72, 1, 78, 70);
        System.out.println("Result 72 = "+res);

        res = DeviceUtil.getValidDesiredCoolingTemp(72.5, 1, 78, 70);
        System.out.println("Result 72.5 = "+res);

        res = DeviceUtil.getValidDesiredCoolingTemp(73, 1, 78, 70);
        System.out.println("Result 73 = "+res);

        res = DeviceUtil.getValidDesiredCoolingTemp(73.5, 1, 78, 70);
        System.out.println("Result 73.5 = "+res);

        res = DeviceUtil.getValidDesiredCoolingTemp(74, 1, 78, 70);
        System.out.println("Result 74 = "+res);

        res = DeviceUtil.getValidDesiredCoolingTemp(74.5, 1, 78, 70);
        System.out.println("Result 74.5 = "+res);

        res = DeviceUtil.getValidDesiredCoolingTemp(75, 1, 78, 70);
        System.out.println("Result 75 = "+res);
        Assert.assertEquals(76.0 , res, 0.0);
        res = DeviceUtil.getValidDesiredCoolingTemp(75.5, 1, 78, 70);
        System.out.println("Result 75.5 = "+res);
        Assert.assertEquals(76.5 , res, 0.0);
        res = DeviceUtil.getValidDesiredCoolingTemp(76, 1, 78, 70);
        System.out.println("Result 76 = "+res);
        Assert.assertEquals(77.0 , res, 0.0);
        res = DeviceUtil.getValidDesiredCoolingTemp(76.5, 1, 78, 70);
        System.out.println("Result 76.5 = "+res);
        Assert.assertEquals(77.5 , res, 0.0);
        res = DeviceUtil.getValidDesiredCoolingTemp(77, 1, 78, 70);
        System.out.println("Result 77 = "+res);
        Assert.assertEquals(78.0 , res, 0.0);
        res = DeviceUtil.getValidDesiredCoolingTemp(77.5, 1, 78, 70);
        System.out.println("Result 77.5 = "+res);
        Assert.assertEquals(78.0 , res, 0.0);
        res = DeviceUtil.getValidDesiredCoolingTemp(78, 1, 78, 70);
        System.out.println("Result 78 = "+res);
        Assert.assertEquals(78.0 , res, 0.0);
        res = DeviceUtil.getValidDesiredCoolingTemp(78.5, 1, 78, 70);
        System.out.println("Result 78.5 = "+res);

        Assert.assertEquals(78 , res, 0.0);
        res = DeviceUtil.getValidDesiredCoolingTemp(79, 1, 78, 70);
        System.out.println("Result 79 = "+res);

        res = DeviceUtil.getValidDesiredCoolingTemp(80, 1, 78, 70);
        System.out.println("Result 80= "+res);
        res = DeviceUtil.getValidDesiredCoolingTemp(81, 1, 78, 70);
        System.out.println("Result 81= "+res);

        res = DeviceUtil.getValidDesiredCoolingTemp(82, 1, 78, 70);
        System.out.println("Result 82= "+res);



        Assert.assertEquals(78.0 , res, 0.0);

    }

    public void testGetValidDesiredHeatingTemp() {

        double res = DeviceUtil.getValidDesiredHeatingTemp(66, 1, 67,65);
        System.out.println("Result 66 = "+res);
        Assert.assertEquals(65,res,0.0);
        res = DeviceUtil.getValidDesiredHeatingTemp(66.5, 1, 67,65);
        System.out.println("Result 66.5 =  "+res);
        Assert.assertEquals(65.5,res,0.0);

        res = DeviceUtil.getValidDesiredHeatingTemp(67, 1, 67,65);
        System.out.println("Result 67 =  "+res);
        Assert.assertEquals(66,res,0.0);

        res = DeviceUtil.getValidDesiredHeatingTemp(67.5, 1, 67,65);
        System.out.println("Result 67.5 = "+res);
        Assert.assertEquals(66.5,res,0.0);

        res = DeviceUtil.getValidDesiredHeatingTemp(68, 1, 67,65);
        System.out.println("Result 67 = "+res);
        Assert.assertEquals(67,res,0.0);

        res = DeviceUtil.getValidDesiredHeatingTemp(68.5, 1, 67,65);
        System.out.println("Result 68.5 = "+res);
        Assert.assertEquals(67,res,0.0);

        res = DeviceUtil.getValidDesiredHeatingTemp(60, 1, 67, 65);
        System.out.println("Result 60 = "+res);
        Assert.assertEquals(65,res,0.0);

        res = DeviceUtil.getValidDesiredHeatingTemp(60.5, 1, 67, 65);
        System.out.println("Result 60.5 = "+res);
        Assert.assertEquals(65,res,0.0);

        res = DeviceUtil.getValidDesiredHeatingTemp(61, 1, 67, 65);
        System.out.println("Result 61 = "+res);
        Assert.assertEquals(65,res,0.0);

        res = DeviceUtil.getValidDesiredHeatingTemp(61.5, 1, 67, 65);
        System.out.println("Result 61.5 = "+res);
        Assert.assertEquals(65,res,0.0);

        res = DeviceUtil.getValidDesiredHeatingTemp(63, 1, 67, 65);
        System.out.println("Result 63 = "+res);
        Assert.assertEquals(65,res,0.0);

        res = DeviceUtil.getValidDesiredHeatingTemp(63.5, 1, 67, 65);
        System.out.println("Result 63.5 = "+res);

        res = DeviceUtil.getValidDesiredHeatingTemp(64, 1, 67, 65);
        System.out.println("Result 64 = "+res);

        res = DeviceUtil.getValidDesiredHeatingTemp(64.5, 1, 67, 65);
        System.out.println("Result 64.5 = "+res);

        res = DeviceUtil.getValidDesiredHeatingTemp(65, 1, 67, 65);
        System.out.println("Result 65 = "+res);
        Assert.assertEquals(65,res,0.0);

        res = DeviceUtil.getValidDesiredHeatingTemp(65.5, 1, 67, 65);
        System.out.println("Result 65.5 = "+res);
        Assert.assertEquals(65,res,0.0);


        res = DeviceUtil.getValidDesiredHeatingTemp(66, 1, 67, 65);
        System.out.println("Result 66 = "+res);

        Assert.assertEquals(65,res,0.0);
        res = DeviceUtil.getValidDesiredHeatingTemp(66.5, 1, 67, 65);
        System.out.println("Result 66.5 = "+res);
        Assert.assertEquals(65.5,res,0.0);

        res = DeviceUtil.getValidDesiredHeatingTemp(67, 1, 67, 65);
        System.out.println("Result 67 = "+res);
        Assert.assertEquals(66,res,0.0);

        res = DeviceUtil.getValidDesiredHeatingTemp(67.5, 1, 67, 65);
        System.out.println("Result 67.5 = "+res);
        Assert.assertEquals(66.5,res,0.0);

        res = DeviceUtil.getValidDesiredHeatingTemp(68, 1, 67, 65);
        System.out.println("Result 68 = "+res);
        Assert.assertEquals(67,res,0.0);

        res = DeviceUtil.getValidDesiredHeatingTemp(68.5, 1, 67, 65);
        System.out.println("Result 68.5 = "+res);
        Assert.assertEquals(67,res,0.0);
    }
}