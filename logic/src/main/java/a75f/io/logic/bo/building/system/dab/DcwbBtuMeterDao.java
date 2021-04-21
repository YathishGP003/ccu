package a75f.io.logic.bo.building.system.dab;

import android.content.Context;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logic.Globals;

/**
 * Class that encapsulates the access to btu meter specific data. There are no write methods since modbus fetch happens
 * internally using register address and there is no requirement at this point to write these to database.
 * Please note that this is not really an implementation of DAO pattern though the name says so.
 *
 */
public class DcwbBtuMeterDao {
    
    //Just use a reasonable non-zero value to avoid algos going crazy when btu meter is actually not paired.
    private static final int DEFAULT_INLET_TEMP = 44;
    private static final int DEFAULT_OUTLET_TEMP = 55;
    
    
    public static DcwbBtuMeterDao getInstance() {
        return new DcwbBtuMeterDao();
    }
    
    /**
     * Get inlet water temp measured using BTU meter.
     * @param hayStack
     * @return
     */
    public double getInletWaterTemperature(CCUHsApi hayStack) {
        if (Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                   .getBoolean("btu_proxy", false)) {
            return Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting",
                                                                                      Context.MODE_PRIVATE)
                          .getInt("inlet_waterTemp", 0);
        }
        HashMap<Object, Object> inletTempPoint = hayStack.read("point and zone and btu and inlet and temp");
        
        if (inletTempPoint.isEmpty()) {
            return DEFAULT_INLET_TEMP;
        }
        
        return hayStack.readHisValById(inletTempPoint.get("id").toString());
    }
    
    /**
     * Get outlet water temp measure using BTU meter.
     * @param hayStack
     * @return
     */
    public double getOutletWaterTemperature(CCUHsApi hayStack) {
        if (Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                   .getBoolean("btu_proxy", false)) {
            return Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting",
                                                                                      Context.MODE_PRIVATE)
                          .getInt("outlet_waterTemp", 0);
        }
    
        HashMap<Object, Object> outletTempPoint = hayStack.read("point and zone and btu and outlet and temp");
    
        if (outletTempPoint.isEmpty()) {
            return DEFAULT_OUTLET_TEMP;
        }
    
        return hayStack.readHisValById(outletTempPoint.get("id").toString());
    }
    
    /**
     * Get chilled water flow rate measured using btu meter.
     * @param hayStack
     * @return
     */
    public double getCWMaxFlowRate(CCUHsApi hayStack) {
        if (Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                   .getBoolean("btu_proxy", false)) {
            return Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting",
                                                                                      Context.MODE_PRIVATE)
                          .getInt("cw_FlowRate", 0);
        }
    
        HashMap<Object, Object> outletTempPoint = hayStack.read("point and zone and btu and actual and flow");
    
        if (outletTempPoint.isEmpty()) {
            return 0;
        }
    
        return hayStack.readHisValById(outletTempPoint.get("id").toString());
    }
}
