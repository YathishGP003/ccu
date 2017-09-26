package a75f.io.renatus;

import org.junit.After;
import org.junit.Before;

/**
 * Created by samjithsadasivan on 9/22/17.
 */

public class LightControlSimulationTest extends CcuSimulationTest
{
    CcuTestRunner mRunner = null;
    
    @Before
    public void setUp() {
        mRunner =  CcuTestRunner.getInstance();
    }
    
    @After
    public void tearDown() {
    }
    
    @Override
    public String getTestDescription() {
        return LightControlSimulationTest.class.getSimpleName();
    }
    
    @Override
    public String getCCUStateFileName() {
        return "lighttestdemo.json";
    }
    
    @Override
    public String getSimulationFileName() {
        return "lighttest.csv";
    }
    
    @Override
    public TestResult analyzeTestResults(TestLog testLog) {
        return TestResult.PASS;
    }
    
    @Override
    public long testDuration() {
        return 60000;
    }
    
    @Override
    public void reportTestResults(TestLog testLog, TestResult result) {
        
    }
    
    @Override
    public String[] graphColumns() {
        return null;
    }
    
    @Override
    public void runTest() {
        
        System.out.println("runTest.........");
        
        /*Log.e("test", L.ccu().toString());
        try
        {
            String stateJSON = JsonSerializer.toJson(L.ccu(), true);
            Log.e("test", stateJSON);
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    
       mRunner.run(this);
    }
    
    
}
