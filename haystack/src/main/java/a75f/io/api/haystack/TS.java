package a75f.io.api.haystack;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;

import java.util.concurrent.TimeUnit;

public class TS {

	
	private InfluxDB mInfluxDB;
	private static final String TIME_SERIES_DATABASE_NAME = "haystack";
	private static TS mTimeSeries; 
	
	private TS()
	{
		mInfluxDB = InfluxDBFactory.connect("http://renatus-influxiprvgkeeqfgys.centralus.cloudapp.azure.com:8086",
				"75f@75f.io", "7575");
		
		mInfluxDB.createDatabase(TIME_SERIES_DATABASE_NAME);
		//mInfluxDB.setDatabase(TIME_SERIES_DATABASE_NAME);
		//String rpName = "aRetentionPolicy";
		
		//mInfluxDB.createRetentionPolicy(rpName, TIME_SERIES_DATABASE_NAME, "30d", "30m", 2, true);
		//mInfluxDB.setRetentionPolicy(rpName);

		//mInfluxDB.enableBatch(BatchOptions.DEFAULTS);
		mInfluxDB.enableBatch(100, 60 , TimeUnit.SECONDS);
	}
	
	
	public static TS getInstance()
	{
		if(mTimeSeries == null)
		{
			mTimeSeries = new TS(); 
		}
		
		return mTimeSeries; 
	}
	
	public InfluxDB getTS()
	{
		return mInfluxDB; 
	}
	
	public String getTimeSeriesDBName()
	{
		return TIME_SERIES_DATABASE_NAME;
	}
	
}