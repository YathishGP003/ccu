package a75f.io.renatus;

import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.kinvey.android.store.UserStore;
import com.kinvey.java.core.KinveyClientCallback;

import java.io.IOException;

import a75f.io.bo.kinvey.CCUUser;
import a75f.io.bo.kinvey.DalContext;
import a75f.io.logic.L;

import static a75f.io.bo.kinvey.DalContext.getSharedClient;
import static a75f.io.logic.L.ccu;
import static a75f.io.logic.L.isDeveloperTesting;
import static a75f.io.logic.L.isSimulation;

/**
 * Created by Yinten on 10/15/2017.
 */

public class SplashActivity extends AppCompatActivity
{
    
    private static final String TAG = SplashActivity.class.getSimpleName();
    
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState,
                         @Nullable PersistableBundle persistentState)
    {
        super.onCreate(savedInstanceState, persistentState);
        setContentView(R.layout.splash);
        L.clearCaches();
        L.setupTestUserIfNeeded();
        login();

    }
    
    
    private void login()
    {
        if (ccu().getUser() != null)
        {
            if (!DalContext.getSharedClient().isUserLoggedIn())
            {
                try
                {
                    UserStore.login(ccu().getUser().getUsername(), ccu().getUser()
                                                                        .getPassword(), getSharedClient(), new KinveyClientCallback()
                    {
                        
                        @Override
                        public void onSuccess(Object o)
                        {
                            loginComplete();
                        }
                        
                        
                        @Override
                        public void onFailure(Throwable throwable)
                        {
                            if (isSimulation() || isDeveloperTesting())
                            {
                                UserStore.exists(ccu().getUser().getUsername(), DalContext
                                                                                        .getSharedClient(), new KinveyClientCallback<Boolean>()
                                {
                                    
                                    @Override
                                    public void onSuccess(Boolean exists)
                                    {
                                        if (!exists)
                                        {
                                            registerUser();
                                        }
                                    }
                                    
                                    
                                    @Override
                                    public void onFailure(Throwable throwable)
                                    {
                                    }
                                });
                            }
                            else
                            {
                                showLoginFailure();
                            }
                        }
                    });
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                    Toast.makeText(SplashActivity.this, "Login Failed", Toast.LENGTH_LONG);
                }
            }
            else
            {
                loginComplete();
            }
        }
        else
        {
            //Go to register screen
            goToRegisterationScreen();
        }
    }
    
    
    private void loginComplete()
    {
        Intent intent = new Intent(this, RenatusLandingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
        //GO to main screen and start loading data from kinvey.
    }
    
    
    private void registerUser()
    {
        if (isSimulation() || isDeveloperTesting())
        {
            UserStore.signUp(ccu().getUser().getUsername(), ccu().getUser()
                                                                 .getPassword(), ccu().getUser(), getSharedClient(), new KinveyClientCallback<CCUUser>()
            {
                @Override
                public void onSuccess(CCUUser ccuUser)
                {
                    try
                    {
                        Log.i(TAG, "Logged in: " + ccuUser.toPrettyString());
                        loginComplete();
                    }
                    catch (IOException e)
                    {
                        Toast.makeText(SplashActivity.this, "Signup Failed", Toast.LENGTH_LONG);
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
    }
    
    
    private void showLoginFailure()
    {
        Toast.makeText(this, "Login Failure", Toast.LENGTH_LONG).show();
    }
    
    
    private void goToRegisterationScreen()
    {
        Toast.makeText(SplashActivity.this, "Go to register screens", Toast.LENGTH_LONG).show();
    }
}
