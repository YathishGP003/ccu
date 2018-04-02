package a75f.io.renatus.kinvey;

import android.content.Context;
import android.location.Address;
import android.os.Handler;
import android.os.Looper;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.google.api.client.json.GenericJson;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kinvey.android.store.DataStore;
import com.kinvey.android.store.UserStore;
import com.kinvey.java.core.KinveyClientCallback;
import com.kinvey.java.store.StoreType;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.concurrent.CountDownLatch;

import a75f.io.kinveybo.AlgoTuningParameters;
import a75f.io.kinveybo.AlgoTuningParameters2;
import a75f.io.kinveybo.CAddress;
import a75f.io.kinveybo.CCUPreconfiguration;
import a75f.io.kinveybo.CCUSchedules;
import a75f.io.kinveybo.CCUUser;
import a75f.io.kinveybo.CCUZones;
import a75f.io.kinveybo.Constants;
import a75f.io.kinveybo.DalContext;
import a75f.io.logic.JsonSerializer;
import a75f.io.renatus.ui.register.OnboardingWizard;

import static a75f.io.kinveybo.DalContext.getSharedClient;
import static a75f.io.logic.JsonSerializer.fromJson;


@RunWith(AndroidJUnit4.class)
@LargeTest
public class DALTest {
    public static final String TAG = "ApplicationTest";
    @Rule
    public ActivityTestRule<OnboardingWizard> mActivityRule =
            new ActivityTestRule<>(OnboardingWizard.class);

    String userId = "userId";
    String password = "password";
    String zone = "";
    String floor = "";
    CCUZones ccuZones = new CCUZones();
    CountDownLatch countDownLatch;
    private String entityId = "";
    private Context context;


    @Before
    public void setUp() {
        context = mActivityRule.getActivity();
    }


    @Test
    public void testKinveyContext() {
        Assert.assertNotNull(context);
    }
    //TODO: organize tuners this way?


    @Test
    public void testRegisteringNewUser() {
        countDownLatch = new CountDownLatch(1);
        Handler h = new Handler(Looper.getMainLooper());
        h.post(new Runnable() {

            public void run() {
                DalContext.instantiate(context);
                if (getSharedClient().isUserLoggedIn()) {
                    UserStore.logout(getSharedClient(), new KinveyClientCallback<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            registerUserTestExtension();
                        }


                        @Override
                        public void onFailure(Throwable throwable) {
                            Assert.fail(throwable.getMessage());
                            countDownLatch.countDown();
                        }
                    });
                } else {
                    registerUserTestExtension();
                }

            }
        });
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }


    private void registerUserTestExtension() {

        try {
            DalContext.instantiate(context);
            //InputStream inputStream = context.getAssets().open("User.json");
            //CCUUser user = fromJson(inputStream, CCUUser.class);

            CCUUser ccuUser = new CCUUser();
            ccuUser.setUsername("YintenStole22");
            ccuUser.setPassword("Password");
            CAddress address = new CAddress();
            address.setAddress("address");
            ccuUser.setFirstname("Ryan");
            ccuUser.setAddressInformation(address);

            Assert.assertNotNull(ccuUser);
            Assert.assertNotNull(ccuUser.getAddressInformation());
            Assert.assertSame(ccuUser.getFirstname(), "Ryan");
            Assert.assertSame(ccuUser.getAddressInformation().getAddress(), "address");
            UserStore
                    .signUp(ccuUser.getUsername(), ccuUser.getPassword(), ccuUser, getSharedClient(), new KinveyClientCallback<CCUUser>() {
                        @Override
                        public void onSuccess(CCUUser ccuUser) {
                            try {
                                Log.i(TAG,
                                        "Logged in: " + ccuUser.toPrettyString());
                                countDownLatch.countDown();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }


                        @Override
                        public void onFailure(Throwable throwable) {
                            Assert.fail(throwable.getMessage());
                            countDownLatch.countDown();
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
            countDownLatch.countDown();
        }
    }


    @Test
    public void testDalContext() {
        DalContext.instantiate(context);
        Assert.assertNotNull(getSharedClient());
        Assert.fail("Message");
    }

    @Test
    public void testAssertFail()
    {
        Assert.fail("Message Failure");
    }

    @Test
    public void testAssertSuccess()
    {
        String s = "";


    }

    @Test
    public void testSavingTuningParameters() {
        countDownLatch = new CountDownLatch(1);
        DalContext.instantiate(context);
        Handler h = new Handler(Looper.getMainLooper());
        h.post(new Runnable() {

            public void run() {
                testSavingTuningParametersUserLogin();
            }
        });
        try {
            countDownLatch.await();
            Log.i(TAG, "LATCH: :This happens after");
        } catch (InterruptedException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }


    private void testSavingTuningParametersUserLogin() {
        try {
            if (!getSharedClient().isUserLoggedIn()) {
                UserStore.login(userId, password, getSharedClient(), new KinveyClientCallback() {

                    @Override
                    public void onSuccess(Object o) {
                        testSavingTuningParametersActual();
                    }


                    @Override
                    public void onFailure(Throwable throwable) {
                        Assert.fail(throwable.getMessage());
                        throwable.printStackTrace();
                        countDownLatch.countDown();
                    }
                });
            } else {
                testSavingTuningParametersActual();
            }
        } catch (IOException e) {
            Assert.fail(e.getMessage());
            e.printStackTrace();
        }
    }


    private void testSavingTuningParametersActual() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    InputStream inputStream =
                            context.getAssets().open("DefaultTuningParameters_v100.json");
                    AlgoTuningParameters algoTuningParameters =
                            fromJson(inputStream, AlgoTuningParameters.class);
                    int buildingNoHotter = (int) algoTuningParameters
                            .get(AlgoTuningParameters.SSETuners.SSE_BUILDING_MAX_TEMP);
                    Assert.assertTrue(buildingNoHotter == 85);
                    final DataStore<AlgoTuningParameters> schedulesDataStore = DataStore
                            .collection(Constants.TUNERS_COLLECTION_NAME, AlgoTuningParameters.class, StoreType.NETWORK, getSharedClient());
                    schedulesDataStore.save(algoTuningParameters);
                    Log.i(TAG, "LATCH: This happens before");
                    countDownLatch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                    Assert.fail(e.getMessage());
                }
            }
        }.start();
    }


    @Test
    public void testDalUserParsing() {
        try {
            InputStream inputStream = context.getAssets().open("User.json");
            Reader reader = new InputStreamReader(inputStream, "UTF-8");
            GsonBuilder gsonBuilder = new GsonBuilder();

            CCUUser user = gsonBuilder.create().fromJson(reader, CCUUser.class);
            //fromJson(inputStream, CCUUser.class);
            Assert.assertNotNull(user);
            Assert.assertEquals(user.getUsername(), "ryan10:27PM");
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }


    @Test
    public void getDALHashMap() {
        DalContext.instantiate(context);
        AlgoTuningParameters2 algoTuningParameters2 = new AlgoTuningParameters2();
        GenericJson json = algoTuningParameters2.algoTunerBackup();
        try {
            JacksonFactory jacksonFactory = new JacksonFactory();
            Log.i(TAG, "Jackson Pretty string: " + jacksonFactory.toString(algoTuningParameters2));
            json.setFactory(jacksonFactory);
            Log.i(TAG, "GenericJSON: " + json.toPrettyString());
            Assert.fail(json.toString());
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }


    @Test
    public void testDalLCMParsing() {
        DalContext.instantiate(context);
        try {
            Handler h = new Handler(Looper.getMainLooper());
            h.post(new Runnable() {

                public void run() {
                    try {
                        if (!getSharedClient().isUserLoggedIn()) {
                            UserStore
                                    .login(userId, password, getSharedClient(), new KinveyClientCallback() {

                                        @Override
                                        public void onSuccess(Object o) {
                                            saveSchedule();
                                        }


                                        @Override
                                        public void onFailure(Throwable throwable) {
                                            Assert.fail(throwable.getMessage());
                                        }
                                    });
                        } else {
                            saveSchedule();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Assert.fail(e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
            e.printStackTrace();
        }
    }


    private void saveSchedule() {
        InputStream inputStream = null;
        try {
            inputStream = context.getAssets().open("CCUSchedules.json");
            CCUSchedules schedule = fromJson(inputStream, CCUSchedules.class);
            final DataStore<CCUSchedules> schedulesDataStore = DataStore
                    .collection(Constants.SCHEDULE_COLLECTION_NAME, CCUSchedules.class, StoreType.NETWORK, getSharedClient());
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
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }


    @Test
    public void testCCUPreconfiguration() {
        //59c8ec3c8f57b7f96ed57ca5
        DalContext.instantiate(context);
        final DataStore<CCUPreconfiguration> preconfigurationDataStore = DataStore
                .collection(Constants.PRECONFIGURATION_NAME, CCUPreconfiguration.class, StoreType.CACHE, getSharedClient());
        final long currTime = System.nanoTime();
        zone = "zone";
        floor = "floor";
        entityId = "";
        ccuZones.setFloor_name(floor);
        ccuZones.set_zone(floor + "_" + zone);
        Handler h = new Handler(Looper.getMainLooper());
        h.post(new Runnable() {

            public void run() {
                try {
                    UserStore.login(userId, password, getSharedClient(), new KinveyClientCallback() {

                        @Override
                        public void onSuccess(Object o) {
                            Log.i(TAG, "USER LOGIN SUCCESS");
                            preconfigurationDataStore
                                    .find("59c8ec3c8f57b7f96ed57ca5", new KinveyClientCallback<CCUPreconfiguration>() {
                                        @Override
                                        public void onSuccess(CCUPreconfiguration ccuPreconfiguration) {
                                            long lengthToPullAndParse =
                                                    System.nanoTime() - currTime;
                                            Log.i(TAG, "Length to pull and parse in nanoseconds: " +
                                                    lengthToPullAndParse);
                                            try {
                                                Log.i(TAG, "CCUPreconfiguration: " +
                                                        ccuPreconfiguration.toPrettyString());
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }


                                        @Override
                                        public void onFailure(Throwable throwable) {
                                            throwable.printStackTrace();
                                            Assert.fail(throwable.getMessage());
                                        }
                                    });
                        }


                        @Override
                        public void onFailure(Throwable throwable) {
                            throwable.printStackTrace();
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void testCCUZones() {
        DalContext.instantiate(context);
        final DataStore<CCUZones> zoneStore = DataStore
                .collection(Constants.ZONE_COLLECTION_NAME, CCUZones.class, StoreType.CACHE, getSharedClient());
        zone = "zone";
        floor = "floor";
        entityId = "";
        ccuZones.setFloor_name(floor);
        ccuZones.set_zone(floor + "_" + zone);
        Handler h = new Handler(Looper.getMainLooper());
        h.post(new Runnable() {

            public void run() {
                try {
                    UserStore.login(userId, password, getSharedClient(), new KinveyClientCallback() {

                        @Override
                        public void onSuccess(Object o) {
                            Log.i(TAG, "USER LOGIN SUCCESS");
                            zoneStore.save(ccuZones, new KinveyClientCallback<CCUZones>() {
                                @Override
                                public void onSuccess(CCUZones result) {
                                    Log.i(TAG, "CCU SAVE ZONES SUCCESS");
                                    Assert.assertNotNull(result.getId());
                                    entityId = result.getId();
                                    zoneStore.find(entityId, new KinveyClientCallback<CCUZones>() {
                                        @Override
                                        public void onSuccess(CCUZones ccuZones) {
                                            Log.i(TAG, "CCU FIND ZONES SUCCESS");
                                            Assert.assertEquals(
                                                    floor + "_" + zone, ccuZones.get_zone());
                                            Assert.assertEquals(floor, ccuZones.getFloor_name());
                                        }


                                        @Override
                                        public void onFailure(Throwable error) {
                                            Assert.fail(error.getMessage());
                                        }
                                    });
                                }


                                @Override
                                public void onFailure(Throwable error) {
                                    Assert.fail(error.getMessage());
                                    error.printStackTrace();
                                }
                            });
                        }


                        @Override
                        public void onFailure(Throwable throwable) {
                            Assert.fail(throwable.getMessage());
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    Assert.fail(e.getMessage());
                }
            }
        });
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}