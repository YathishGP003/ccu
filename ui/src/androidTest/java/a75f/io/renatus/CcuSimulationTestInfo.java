package a75f.io.renatus;

import java.util.ArrayList;
import java.util.List;

import a75f.io.bo.building.CCUApplication;

/**
 * Created by samjithsadasivan on 9/26/17.
 */

public class CcuSimulationTestInfo
{
    public String name;
    
    public SimulationResult result;
    
    public List<String[]> simulationInput = null;
    
    public CCUApplication inputCcuState = null;
    
    public ArrayList<SmartNodeParams> nodeParams = new ArrayList<>();
}
