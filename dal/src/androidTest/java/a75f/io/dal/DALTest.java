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
					}
				}
			});
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		try
		{
			Thread.sleep(5000);
		}
		catch (InterruptedException e)
		{
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
					                                                   .collection(Constants.SCHEDULE_COLLECTION_NAME, CCUSchedules.class, StoreType.CACHE, DalContext
							                                                                                                                                        .getSharedClient());
			schedulesDataStore.save(schedule);
			Assert.assertNotNull(schedule);
			Assert.assertNotNull(schedule.getLcm_zone_schedule());
		}
		catch (IOException e)
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
										public void onFailure(Throwable throwable)
										{
										}
									});
								}
								
								
								@Override
								public void onFailure(Throwable error)
								{
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