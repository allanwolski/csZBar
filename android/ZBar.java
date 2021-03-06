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
    private JSONObject params;

    // Permissions
    private String[] permissions = {Manifest.permission.CAMERA};

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
                    createScanActivity();
                }
            } else if (action.equals("close")) {
                ZBarScannerActivity.scan.finish();
                return false;
            } else {
                return false;
            }
        } else {
            PermissionHelper.requestPermissions(this, 0, permissions);
        }

        return true;
    }

    private void createScanActivity() {
         Context appCtx = cordova.getActivity().getApplicationContext();
         Intent scanIntent = new Intent(appCtx, ZBarScannerActivity.class);
         scanIntent.putExtra(ZBarScannerActivity.EXTRA_PARAMS, params.toString());
         cordova.startActivityForResult(this, scanIntent, SCAN_CODE);
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

    // Processes the result of permission request
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        if (grantResults.length > 0) {
            Boolean hasAllPermissions = true;

            for (int r : grantResults) {
                if (r == PackageManager.PERMISSION_DENIED) {
                    hasAllPermissions = false;
                }
            }

            if (hasAllPermissions) {
                createScanActivity();
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
                    scanCallbackContext.error("Falha ao tentar digitalizar o Código de Barras");
                    break;
                default:
                    scanCallbackContext.error("Erro não tratado");
            }
            isInProgress = false;
            scanCallbackContext = null;
        }
    }
}
