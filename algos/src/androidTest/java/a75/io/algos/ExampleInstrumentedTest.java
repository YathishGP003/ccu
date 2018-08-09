package a75.io.algos;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest
{
    @Test
    public void testInfluxTS() {
    
        /*Configuration configuration = new Configuration("192.168.0.15", "8086", "root", "root", "renatusts");
        try
        {
            DataWriter writer = new DataWriter(configuration);
            writer.setMeasurement("VAV");
            writer.setTimeUnit(TimeUnit.SECONDS);
            writer.setTime(System.currentTimeMillis() / 1000);
    
            double roomTemp = 65;
            writer.addField("roomTemp1", ++roomTemp);		// Integer value
            writer.addField("roomTemp2", ++roomTemp);		// Double value
            writer.addField("roomTemp3", roomTemp);	// String value
    
            writer.addTag("hostname", "server001");
            writer.addTag("disk_type", "SSD");
            writer.writeData();
            
        } catch (Exception e) {
            e.printStackTrace();
        }*/
        /*InfluxDB influxDB = InfluxDBFactory.connect("http://10.0.2.2:8086", "root", "root");
        String dbName = "renatusts";
        //influxDB.createDatabase(dbName);
        influxDB.setDatabase(dbName);
        //String rpName = "aRetentionPolicy";
        //influxDB.createRetentionPolicy(rpName, dbName, "30d", "30m", 2, true);
        //influxDB.setRetentionPolicy(rpName);
        
        influxDB.enableBatch(BatchOptions.DEFAULTS);
        
        influxDB.write(Point.measurement("cpu")
                            .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                            .addField("idle", 150L)
                            .addField("user", 9L)
                            .addField("system", 1L)
                            .build());
        
        influxDB.write(Point.measurement("disk")
                            .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                            .addField("used", 80L)
                            .addField("free", 1L)
                            .build());
    
        *//*Query query = new Query("SELECT idle FROM cpu", dbName);
        influxDB.query(query);
        influxDB.dropRetentionPolicy(rpName, dbName);
        influxDB.deleteDatabase(dbName);*//*
        influxDB.close();*/
    }
    
    @Test
    public void testNewInfluxLib() {
        /*InfluxDB influxDB = InfluxDBFactory.connect("http://192.168.0.15:8086", "root", "root");
        String dbName = "renatusts";
        //influxDB.createDatabase(dbName);
        
        BatchPoints batchPoints = BatchPoints
                                          .database(dbName)
                                          .tag("async", "true")
                                          .retentionPolicy("autogen")
                                          .consistency(InfluxDB.ConsistencyLevel.ALL)
                                          .build();
        
        
        Point point1 = Point.measurement("Temp1")
                            .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                            .addField("room", 88.0)
                            .addField("supply", 80)
                            .addField("discharge", 85)
                            .build();
        Point point2 = Point.measurement("VAV1")
                            .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                            .addField("damper", 100)
                            .addField("valve", 50)
                            .build();
        batchPoints.point(point1);
        batchPoints.point(point2);
        influxDB.write(batchPoints);*/
    }
}
