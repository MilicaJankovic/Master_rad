package com.example.petra.healthylifeapp.Helpers;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.example.petra.healthylifeapp.MainActivity;

/**
 * Created by Petra on 2/26/2018.
 */

public class LocationPermissionUtility extends ContextWrapper {

    private static final int REQUEST_OAUTH_REQUEST_CODE = 0x1001;
    private final static int REQUEST_PERMISSION_RESULT_CODE = 42;


    public LocationPermissionUtility(Context context) {
        super(context);
        //this.context = context;
    }

    public boolean checkLocationPermission() {
        if (!hasLocationPermission()) {
            Log.e("Tuts+", "Does not have location permission granted");
            requestLocationPermission();
            return false;
        }

        return true;
    }


    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(new LocationPermissionUtility(this), android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(
                MainActivity.instance,
                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_PERMISSION_RESULT_CODE);
    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
//                                           @NonNull int[] grantResults) {
//        switch (requestCode) {
//            case REQUEST_PERMISSION_RESULT_CODE: {
//                // If request is cancelled, the result arrays are empty.
//                if (grantResults.length > 0
//                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    //granted
//                } else {
//                    Log.e("Tuts+", "Location permission denied.");
//                }
//            }
//        }
//    }

}
