package a75f.io.renatus.framework;

/**
 * Created by samjithsadasivan on 10/14/2017.
 */

/**
 * Provides individual tests with finer control on frequency of pulling results
 */
public class SamplingProfile
{
	int resultCount;
	int resultPeriodSecs;
	
	public SamplingProfile(int count, int period){
		resultCount = count;
		resultPeriodSecs = period;
	}
	
	public int getResultCount() {
		return resultCount;
	}
	
	public int getResultInterval() {
		return resultPeriodSecs;
	}
	
	
}
