package a75f.io.logic;


import android.location.Address;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import junit.framework.Assert;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.projecthaystack.HDict;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.io.HZincReader;
import org.projecthaystack.io.HZincWriter;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Site;

@RunWith(AndroidJUnit4.class)
public class GPSTest {

    private static final String TAG = "HaystackFrameworkTest";

    @Test
    public void testGPSLocation() throws IOException, JSONException {

        String address = "54016, United States";
        address = address.replaceAll(" ", "%20");

        HttpPost httppost = new HttpPost( "https://maps.google.com/maps/api/geocode/json?address=" + address + "&sensor=false&key=AIzaSyD3mUArjl1fvA7EBy6M8x8FJSKpKS3RmOg");
        HttpClient client = new DefaultHttpClient();

        org.apache.http.HttpResponse response;
        StringBuilder stringBuilder = new StringBuilder();

        response = client.execute(httppost);
        HttpEntity entity = response.getEntity();
        InputStream stream = entity.getContent();
        int b;
        while ((b = stream.read()) != -1) {
            stringBuilder.append((char) b);
        }
        System.out.println("Results: " + stringBuilder.toString());
        JSONObject jsonObject = new JSONObject(stringBuilder.toString());
        JSONArray array = (JSONArray) jsonObject.get("results");
        if(array.length() > 0) {
            for (int i = 0; i < 1; i++) {
                double lon = 0;
                double lat = 0;
                String name = "";
                try {
                    lon = array.getJSONObject(i).getJSONObject("geometry")
                            .getJSONObject("location").optDouble("lng");

                    lat = array.getJSONObject(i).getJSONObject("geometry")
                            .getJSONObject("location").optDouble("lat");
                    name = array.getJSONObject(i)
                            .optString("formatted_address");
                    Address addr = new Address(Locale.getDefault());
                    addr.setLatitude(lat);
                    addr.setLongitude(lon);
                    addr.setAddressLine(0, name != null ? name : "");

                } catch (JSONException e) {
                    e.printStackTrace();

                }
            }
        }
    }


}
