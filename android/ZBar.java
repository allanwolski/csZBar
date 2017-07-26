package org.cloudsky.cordovaPlugins;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.Manifest;

import org.apache.cordova.PermissionHelper;
import org.cloudsky.cordovaPlugins.ZBarScannerActivity;

public class ZBar extends CordovaPlugin {
    // Configuration
    private static int SCAN_CODE = 1;

    // State
    private boolean isInProgress = false;
    private CallbackContext scanCallbackContext;
    
    //permissions
    private String[] permissions = {Manifest.permission.CAMERA};
    private JSONObject params;    

    // Plugin API
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        scanCallbackContext = callbackContext;
        params = args.optJSONObject(0);

        if (hasPermission()) {
            if (action.equals("scan")) {
                if (isInProgress) {
                    callbackContext.error("A scan is already in progress!");
                } else {
                    isInProgress = true;

                    Context appCtx = cordova.getActivity().getApplicationContext();
                    Intent scanIntent = new Intent(appCtx, ZBarScannerActivity.class);
                    scanIntent.putExtra(ZBarScannerActivity.EXTRA_PARAMS, params.toString());
                    cordova.startActivityForResult(this, scanIntent, SCAN_CODE);
                }
                return true;
            } else if (action.equals("close")) {
                ZBarScannerActivity.scan.finish();
                return false;
            } else {
                return false;
            }
        } else {
            PermissionHelper.requestPermissions(this, 0, permissions);
            return true;
        }
    }

    // check application's permissions
    private boolean hasPermission() {
        for(String p : permissions) {
            if(!PermissionHelper.hasPermission(this, p)) {
                return false;
            }
        }
        return true;
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case CAMERA_PERMISSION_REQUEST: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    isInProgress = true;

                    Context appCtx = cordova.getActivity().getApplicationContext();
                    Intent scanIntent = new Intent(appCtx, ZBarScannerActivity.class);
                    scanIntent.putExtra(ZBarScannerActivity.EXTRA_PARAMS, params.toString());
                    cordova.startActivityForResult(this, scanIntent, SCAN_CODE);
                }
                return;
            }
        }
    }

    // External results handler
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (requestCode == SCAN_CODE) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    String barcodeValue = result.getStringExtra(ZBarScannerActivity.EXTRA_QRVALUE);
                    scanCallbackContext.success(barcodeValue);
                    break;
                case Activity.RESULT_CANCELED:
                    scanCallbackContext.error("cancelled");
                    break;
                case ZBarScannerActivity.RESULT_ERROR:
                    scanCallbackContext.error("Scan failed due to an error");
                    break;
                default:
                    scanCallbackContext.error("Unknown error");
            }
            isInProgress = false;
            scanCallbackContext = null;
        }
    }
}
