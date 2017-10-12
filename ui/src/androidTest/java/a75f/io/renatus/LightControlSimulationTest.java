package a75f.io.renatus;

import android.os.Environment;
import android.text.format.DateFormat;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import a75f.io.bo.building.definitions.MockTime;

/**
 * Created by samjithsadasivan on 9/22/17.
 */

public class LightControlSimulationTest extends BaseSimulationTest
{
    SimulationRunner mRunner = null;
    
    @Before
    public void setUp() {
        mRunner =  new SimulationRunner(this);
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
         return "lighttest.json";
    }
    
    @Override
    public String getSimulationFileName() {
        return "lighttest.csv";
    }
    
    @Override
    public TestResult analyzeTestResults(SimulationTestInfo testLog) {
    
        for (SmartNodeParams param : testLog.nodeParams) {
            if (param.lighting_control_enabled == 0) {
                return TestResult.FAIL;
            }
        }
        return TestResult.PASS;
    }
    
    @Override
    public long testDuration() {
        return mRunner.duration();
    }
    
    @Override
    public void reportTestResults(SimulationTestInfo testLog, TestResult result) {
        
    }
    
    @Override
    public String[] graphColumns() {
        return null;
    }
    
    @Override
    public void runTest() {
    
        String templateHtml = SimulationTestInfo.readFileFromAssets("templates/testdetails2.html");
        Document doc = Jsoup.parse(templateHtml);
        Element script = doc.getElementById("relay1chart");
    
        for (DataNode node : script.dataNodes()) {
            Attributes a = node.attributes();
            String x = node.attr("data");
            String s = node.getWholeData();
            String b = "";
        }
        
        Element scrip1 = doc.getElementById("analog1chart");
        
        Elements scripts = doc.getElementsByTag("script");
        for (Element scrit : scripts) {
            for (DataNode node : script.dataNodes()) {
                String a = node.attr("data");
                String s = node.getWholeData();
                String b = "";
            }
        }
        
        mRunner.runSimulation();
        //MockTime.getInstance().setMockTime(true, System.currentTimeMillis()+ (8 * 3600000)); // Force the mocktime to out of schedule interval
        //mRunner.resetRunner();
        //mRunner.runSimulation();
   }
    
    
}
