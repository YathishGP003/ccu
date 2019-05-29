package a75f.io.renatus.registartion;


import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import a75f.io.renatus.DABFullyAHUProfile;
import a75f.io.renatus.DABHybridAhuProfile;
import a75f.io.renatus.DABStagedProfile;
import a75f.io.renatus.DABStagedRtuWithVfdProfile;
import a75f.io.renatus.DefaultSystemProfile;
import a75f.io.renatus.FloorPlanFragment;
import a75f.io.renatus.SystemFragment;
import a75f.io.renatus.VavAnalogRtuProfile;
import a75f.io.renatus.VavHybridRtuProfile;
import a75f.io.renatus.VavStagedRtuProfile;
import a75f.io.renatus.VavStagedRtuWithVfdProfile;

public class RegistrationAdapter extends FragmentStatePagerAdapter /*implements InstallType */{
    int numOfTabs;
    Context mContext;
    StartCCUFragment startCCUFragment;
    InstallTypeFragment installTypeFragment;
    WifiFragment wifiFragment;
    CreateNewSite createNewSite;
    InstallerOptions installerOptions;
    Security securityFragment;
    AddtoExisting addtoExisting;
    PreConfigCCU preConfigCCU;
    ReplaceCCU replaceCCU;
    DefaultSystemProfile defaultSystemProfile;
    VavStagedRtuProfile vavStagedRTU;
    VavAnalogRtuProfile vavAnalogRTU;
    VavStagedRtuWithVfdProfile vavStagedRtuWithVfdProfile;
    VavHybridRtuProfile vavAdvancedHybridRtu;
    DABStagedProfile dabStagedProfile;
    DABFullyAHUProfile dabFullyAHUProfile;
    DABStagedRtuWithVfdProfile dabStagedRtuWithVfdProfile;
    DABHybridAhuProfile dabHybridAhuProfile;
    FloorPlanFragment floorPlanFragment;
    SystemFragment systemFragment;
    CongratsFragment congratsFragment;

    public RegistrationAdapter(Context context, FragmentManager fm, int numOfTabs) {
        super(fm);
        this.mContext = context;
        this.numOfTabs = numOfTabs;
       /* startCCUFragment = new StartCCUFragment();
        installTypeFragment = new InstallTypeFragment();
        wifiFragment = new WifiFragment();
        createNewSite = new CreateNewSite();
        installerOptions = new InstallerOptions();
        securityFragment = new Security();
        addtoExisting = new AddtoExisting();
        preConfigCCU = new PreConfigCCU();
        replaceCCU = new ReplaceCCU();
        vavStagedRTU = new VavStagedRtuProfile();
        defaultSystemProfile = new DefaultSystemProfile();*/
    }

    @Override
    public Fragment getItem(int position) {

        Fragment fragment = null;
        switch (position) {
            case 0:
                //fragment = new StartCCUFragment();
                startCCUFragment = new StartCCUFragment();
                //return StartCCUFragment.newInstance("Start CCU","0");
                return startCCUFragment;
            case 1:
                //fragment = new InstallTypeFragment();
                installTypeFragment = new InstallTypeFragment();
                //return InstallTypeFragment.newInstance("Select Type of Install","1");
                //installTypeFragment = new InstallTypeFragment();
                return installTypeFragment;
            case 2:
                //fragment = new WifiFragment();
                wifiFragment = new WifiFragment();
                //return WifiFragment.newInstance("Connect to Wifi","2");
                //wifiFragment = new WifiFragment();
                return  wifiFragment;
            case 3:
                //fragment = new CreateNewSite();
                //return CreateNewSite.newInstance("Create New Site","3");
                createNewSite = new CreateNewSite();
                return createNewSite;
            case 4:
                //fragment = new InstallerOptions();
                //InstallerOptions installerOptions = new InstallerOptions();
                //return InstallerOptions.newInstance("Installer Options","4");
                installerOptions = new InstallerOptions();
                return installerOptions;
            case 5:
                //fragment = new Security();
                //Security securityFragment = new Security();
                //return Security.newInstance("Security","5");
                securityFragment = new Security();
                return securityFragment;
            case 6:
                //fragment = new AddtoExisting();
                //AddtoExisting addtoExisting = new AddtoExisting();
                //return AddtoExisting.newInstance("Add to Existing Site","6");
                addtoExisting = new AddtoExisting();
                return addtoExisting;
            case 7:
                //fragment = new PreConfigCCU();
                //PreConfigCCU preConfigCCU = new PreConfigCCU();
                //return PreConfigCCU.newInstance("Pre Configured CCU","7");
                preConfigCCU = new PreConfigCCU();
                return preConfigCCU;
            case 8:
                //fragment = new ReplaceCCU();
                replaceCCU = new ReplaceCCU();
                //return ReplaceCCU.newInstance("Replace CCU","8");
                return replaceCCU;
            case 9:
                //fragment = new DefaultSystemProfile();
                defaultSystemProfile = new DefaultSystemProfile();
                //return ReplaceCCU.newInstance("Replace CCU","8");
                return defaultSystemProfile;
            case 10:
                //fragment = new VavStagedRtuProfile();
                vavStagedRTU = new VavStagedRtuProfile();
                //return ReplaceCCU.newInstance("Replace CCU","8");
                return vavStagedRTU;
            case 11:
                //fragment = new VavStagedRtuProfile();
                vavAnalogRTU = new VavAnalogRtuProfile();
                //return ReplaceCCU.newInstance("Replace CCU","8");
                return vavAnalogRTU;
            case 12:
                //fragment = new VavStagedRtuProfile();
                vavStagedRtuWithVfdProfile = new VavStagedRtuWithVfdProfile();
                //return ReplaceCCU.newInstance("Replace CCU","8");
                return vavStagedRtuWithVfdProfile;
            case 13:
                //fragment = new VavStagedRtuProfile();
                vavAdvancedHybridRtu = new VavHybridRtuProfile();
                //return ReplaceCCU.newInstance("Replace CCU","8");
                return vavAdvancedHybridRtu;
            case 14:
                //fragment = new VavStagedRtuProfile();
                dabStagedProfile = new DABStagedProfile();
                //return ReplaceCCU.newInstance("Replace CCU","8");
                return dabStagedProfile;
            case 15:
                //fragment = new VavStagedRtuProfile();
                dabFullyAHUProfile = new DABFullyAHUProfile();
                //return ReplaceCCU.newInstance("Replace CCU","8");
                return dabFullyAHUProfile;
            case 16:
                //fragment = new VavStagedRtuProfile();
                dabStagedRtuWithVfdProfile = new DABStagedRtuWithVfdProfile();
                //return ReplaceCCU.newInstance("Replace CCU","8");
                return dabStagedRtuWithVfdProfile;
            case 17:
                //fragment = new VavStagedRtuProfile();
                dabHybridAhuProfile = new DABHybridAhuProfile();
                //return ReplaceCCU.newInstance("Replace CCU","8");
                return dabHybridAhuProfile;
            case 18:
                //fragment = new VavStagedRtuProfile();
                floorPlanFragment = new FloorPlanFragment();
                //return ReplaceCCU.newInstance("Replace CCU","8");
                return floorPlanFragment;
            case 19:
                //fragment = new VavStagedRtuProfile();
                systemFragment = new SystemFragment();
                //return ReplaceCCU.newInstance("Replace CCU","8");
                return systemFragment;
            case 20:
                //fragment = new VavStagedRtuProfile();
                congratsFragment = new CongratsFragment();
                //return ReplaceCCU.newInstance("Replace CCU","8");
                return congratsFragment;
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return numOfTabs;
    }

    /*@Override
    public void GoTo(int from, int to) {
        //WifiFragment wifiFragment = new WifiFragment();
        wifiFragment.GoTo(from,to);
    }*/
}