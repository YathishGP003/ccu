package a75f.io.dal;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.kinvey.android.model.User;
import com.kinvey.android.store.DataStore;
import com.kinvey.android.store.UserStore;
import com.kinvey.java.core.KinveyClientCallback;
import com.kinvey.java.store.StoreType;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class DALTest
{
	public static final String TAG = "ApplicationTest";
	
	//Context mMockContext;
	String   userId   = "userId";
	String   password = "password";
	String   zone     = "";
	String   floor    = "";
	CCUZones ccuZones = new CCUZones();
	private String entityId = "";
	private Context context;
	
	
	@Before
	public void setUp()
	{
		context = InstrumentationRegistry.getTargetContext();
	}
	
	
	@Test
	public void testKinveyContext()
	{
		Assert.assertNotNull(context);
	}
	
	
	@Test
	public void testDalContext()
	{
		DalContext.instantiate(context);
		Assert.assertNotNull(DalContext.getSharedClient());
	}
	
	
	@Test
	public void testDalUserParsing()
	{
		try
		{
			InputStream inputStream = context.getAssets().open("User.json");
			User user = JsonSerializer.fromJson(inputStream, User.class);
			Assert.assertNotNull(user);
			Assert.assertEquals(user.getUsername(), "userId");
		}
		catch (IOException e)
		{
			e.printStackTrace();
			Assert.fail();
		}
	}
	
	
	@Test
	public void testDalLCMParsing()
	{
		DalContext.instantiate(context);
		try
		{
			
			
			Handler h = new Handler(Looper.getMainLooper());
			h.post(new Runnable()
			{
				
				public void run()
				{
					try
					{
						if(!DalContext.getSharedClient().isUserLoggedIn())
						{
							UserStore.login(userId, password, DalContext.getSharedClient(), new KinveyClientCallback()
							{
								
								@Override
								public void onSuccess(Object o)
								{
									saveSchedule();
								}
								
								
								@Override
								public void onFailure(Throwable throwable)
								{
                                    Assert.fail(throwable.getMessage());
								}
							});
						}
						else
						{
							saveSchedule();
						}
					}
					catch (IOException e)
					{
						e.printStackTrace();
                        Assert.fail(e.getMessage());
                    }
				}
			});
		}
		catch (Exception e)
		{
			e.printStackTrace();
            Assert.fail(e.getMessage());
		}
		try
		{
			Thread.sleep(20000);
		}
		catch (InterruptedException e)
		{
            Assert.fail(e.getMessage());
            e.printStackTrace();
		}
	}
	
	
	private void saveSchedule()
	{
		InputStream inputStream = null;
		try
		{
			inputStream = context.getAssets().open("CCUSchedules.json");
			
			CCUSchedules schedule = JsonSerializer.fromJson(inputStream, CCUSchedules.class);
			final DataStore<CCUSchedules> schedulesDataStore = DataStore
					                                                   .collection(Constants.SCHEDULE_COLLECTION_NAME, CCUSchedules.class, StoreType.NETWORK, DalContext
							                                                                                                                                        .getSharedClient());


            schedulesDataStore.save(schedule, new KinveyClientCallback<CCUSchedules>() {
                @Override
                public void onSuccess(CCUSchedules ccuSchedules) {
                    Log.i(TAG, "Schedule SaVED");
                    Assert.assertNotNull(ccuSchedules);
                }

                @Override
                public void onFailure(Throwable throwable) {
                    Log.i(TAG, "Schedule fAILED");

                    Assert.fail(throwable.getMessage());
                }
            });

            Assert.assertNotNull(schedule);
			Assert.assertNotNull(schedule.getLcm_zone_schedule());
		}
		catch (IOException e)
		{
			e.printStackTrace();
            Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testCCUPreconfiguration()
	{
		//59c8ec3c8f57b7f96ed57ca5
		DalContext.instantiate(context);
		final DataStore<CCUPreconfiguration> preconfigurationDataStore = DataStore
				                                      .collection(Constants.PRECONFIGURATION_NAME, CCUPreconfiguration.class, StoreType.CACHE, DalContext
						                                                                                                                   .getSharedClient());
        
        final long currTime = System.nanoTime();
		zone = "zone";
		floor = "floor";
		entityId = "";
		ccuZones.setFloor_name(floor);
		ccuZones.set_zone(floor + "_" + zone);
		Handler h = new Handler(Looper.getMainLooper());
		h.post(new Runnable()
		{
			
			public void run()
			{
				try
				{
					UserStore.login(userId, password, DalContext
							                                  .getSharedClient(), new KinveyClientCallback()
					{
						
						@Override
						public void onSuccess(Object o)
						{
							Log.i(TAG, "USER LOGIN SUCCESS");
							preconfigurationDataStore.find("59c8ec3c8f57b7f96ed57ca5", new KinveyClientCallback<CCUPreconfiguration>()
							{
								@Override
								public void onSuccess(CCUPreconfiguration ccuPreconfiguration)
								{
                                    long lengthToPullAndParse = System.nanoTime() - currTime;
                                    Log.i(TAG, "Length to pull and parse in nanoseconds: " + lengthToPullAndParse);
                                    
									try
									{
										Log.i(TAG, "CCUPreconfiguration: "  + ccuPreconfiguration.toPrettyString());
									}
									catch (IOException e)
									{
										e.printStackTrace();
									}
								}
								@Override
								public void onFailure(Throwable throwable)
								{
									throwable.printStackTrace();
								}
							});
						}
						
						
						@Override
						public void onFailure(Throwable throwable)
						{
							throwable.printStackTrace();
						}
					});
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		});
		try
		{
			Thread.sleep(30000);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		
	}
	
	
	@Test
	public void testCCUZones()
	{
		DalContext.instantiate(context);
		final DataStore<CCUZones> zoneStore = DataStore
				                                      .collection(Constants.ZONE_COLLECTION_NAME, CCUZones.class, StoreType.CACHE, DalContext
						                                                                                                                   .getSharedClient());
		zone = "zone";
		floor = "floor";
		entityId = "";
		ccuZones.setFloor_name(floor);
		ccuZones.set_zone(floor + "_" + zone);
		Handler h = new Handler(Looper.getMainLooper());
		h.post(new Runnable()
		{
			
			public void run()
			{
				try
				{
					UserStore.login(userId, password, DalContext
							                                  .getSharedClient(), new KinveyClientCallback()
					{
						
						@Override
						public void onSuccess(Object o)
						{
							Log.i(TAG, "USER LOGIN SUCCESS");
							zoneStore.save(ccuZones, new KinveyClientCallback<CCUZones>()
							{
								@Override
								public void onSuccess(CCUZones result)
								{
									Log.i(TAG, "CCU SAVE ZONES SUCCESS");
									Assert.assertNotNull(result.getId());
									entityId = result.getId();
									zoneStore.find(entityId, new KinveyClientCallback<CCUZones>()
									{
										@Override
										public void onSuccess(CCUZones ccuZones)
										{
											Log.i(TAG, "CCU FIND ZONES SUCCESS");
											Assert.assertEquals(
													floor + "_" + zone, ccuZones.get_zone());
											Assert.assertEquals(floor, ccuZones.getFloor_name());
										}
										
										
										@Override
										public void onFailure(Throwable error)
										{
                                            Assert.fail(error.getMessage());
                                        }
									});
								}
								
								
								@Override
								public void onFailure(Throwable error)
								{

                                    Assert.fail(error.getMessage());
                                    error.printStackTrace();
								}
							});
						}
						
						
						@Override
						public void onFailure(Throwable throwable)
						{
						}
					});
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		});
		try
		{
			Thread.sleep(5000);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}
}