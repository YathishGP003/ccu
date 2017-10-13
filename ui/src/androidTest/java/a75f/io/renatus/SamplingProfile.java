package a75f.io.renatus;

/**
 * Created by samjithsadasivan on 10/14/2017.
 */

/**
 * Provides individual tests with finer control on frequency of pulling results
 */
public class SamplingProfile
{
	public SamplingProfile(int count, int period){
		resultCount = count;
		resultPeriodSecs = period;
	}
	int resultCount;
	int resultPeriodSecs;
	
}
