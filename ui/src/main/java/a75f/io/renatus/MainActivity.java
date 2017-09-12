package a75f.io.renatus;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import a75f.io.renatus.BLE.BLEHomeFragment;
import a75f.io.renatus.USB.USBHomeFragment;

public class MainActivity extends AppCompatActivity
{
	
	private BottomNavigationView.OnNavigationItemSelectedListener
			mOnNavigationItemSelectedListener =
			new BottomNavigationView.OnNavigationItemSelectedListener()
			{
				
				@Override
				public boolean onNavigationItemSelected(@NonNull MenuItem item)
				{
					switch (item.getItemId())
					{
						case R.id.navigation_home:
							changeContent(DefaultFragment.getInstance());
							return true;
						case R.id.navigation_ble:
							changeContent(BLEHomeFragment.getInstance());
							return true;
						case R.id.navigation_usb:
							changeContent(USBHomeFragment.getInstance());
							return true;
					}
					return false;
				}
			};
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		//Globals.getInstance().getSmartNode().setMeshAddress((short) 6000);
		//Globals.getInstance().getSmartNode().setName("RyansNode");
		BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
		navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
		changeContent(DefaultFragment.getInstance(false));
	}
	
	
	private void changeContent(Fragment fragment)
	{
		getSupportFragmentManager().beginTransaction().replace(R.id.content, fragment).commit();
	}
}
