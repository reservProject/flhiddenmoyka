package wash.rocket.xor.rocketwash.util;

import android.app.Application;
import android.content.Context;

import com.crashlytics.android.Crashlytics;

import java.util.ArrayList;

import io.fabric.sdk.android.Fabric;
import wash.rocket.xor.rocketwash.model.CarsAttributes;
import wash.rocket.xor.rocketwash.model.Profile;

public class App extends Application {

    private ArrayList<CarsAttributes> carsAttributes;
    private Profile profile;

    public ArrayList<CarsAttributes> getCarsAttributes() {
        return carsAttributes;
    }

    public void setCarsAttributes(ArrayList<CarsAttributes> carsAttributes) {
        this.carsAttributes = carsAttributes;
    }

    public Profile getProfile() {

        if (profile == null) {
            Preferences pref = new Preferences(getBaseContext());
            profile = pref.getProfile();
        }

        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }


    @Override
    public void onCreate() {
        //MultiDex.install(this);
        super.onCreate();
        Fabric.with(this, new Crashlytics());
        //JodaTimeAndroid.init(this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        //MultiDex.install(this);
        super.attachBaseContext(base);
    }
}
