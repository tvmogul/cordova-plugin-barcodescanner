/**
 * PhoneGap is available under *either* the terms of the modified BSD license *or* the
 * MIT License (2008). See http://opensource.org/licenses/alphabetical for full text.
 *
 * Copyright (c) Matt Kane 2010
 * Copyright (c) 2011, IBM Corporation
 * Copyright (c) 2013, Maciej Nux Jaros
 */
package com.phonegap.plugins.barcodescanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import com.sergioapps.grab.R;


/**
 * This calls out to the ZXing barcode reader and returns the result.
 *
 * @sa https://github.com/apache/cordova-android/blob/master/framework/src/org/apache/cordova/CordovaPlugin.java
 */
public class BarcodeScanner extends CordovaPlugin {
    public static final int REQUEST_CODE = 0x0ba7c0de;

    private static final String SCAN = "scan";
    private static final String ENCODE = "encode";
    private static final String CANCELLED = "cancelled";
    private static final String FORMAT = "format";
    private static final String TEXT = "text";
    private static final String DATA = "data";
    private static final String TYPE = "type";
    private static final String SCAN_INTENT = "com.phonegap.plugins.barcodescanner.SCAN";
    private static final String ENCODE_DATA = "ENCODE_DATA";
    private static final String ENCODE_TYPE = "ENCODE_TYPE";
    private static final String ENCODE_INTENT = "com.phonegap.plugins.barcodescanner.ENCODE";
    private static final String TEXT_TYPE = "TEXT_TYPE";
    private static final String EMAIL_TYPE = "EMAIL_TYPE";
    private static final String PHONE_TYPE = "PHONE_TYPE";
    private static final String SMS_TYPE = "SMS_TYPE";

    private static final String LOG_TAG = "BarcodeScanner";

    private CallbackContext callbackContext;

    /**
     * Constructor.
     */
    public BarcodeScanner() {
    }

    /**
     * Executes the request.
     *
     * This method is called from the WebView thread. To do a non-trivial amount of work, use:
     *     cordova.getThreadPool().execute(runnable);
     *
     * To run on the UI thread, use:
     *     cordova.getActivity().runOnUiThread(runnable);
     *
     * @param action          The action to execute.
     * @param args            The exec() arguments.
     * @param callbackContext The callback context used when calling back into JavaScript.
     * @return                Whether the action was valid.
     *
     * @sa https://github.com/apache/cordova-android/blob/master/framework/src/org/apache/cordova/CordovaPlugin.java
     */
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        this.callbackContext = callbackContext;

        if (action.equals(ENCODE)) {
            JSONObject obj = args.optJSONObject(0);
            if (obj != null) {
                String type = obj.optString(TYPE);
                String data = obj.optString(DATA);

                // If the type is null then force the type to text
                if (type == null) {
                    type = TEXT_TYPE;
                }

                if (data == null) {
                    callbackContext.error("User did not specify data to encode");
                    return true;
                }

                encode(type, data);
            } else {
                callbackContext.error("User did not specify data to encode");
                return true;
            }
        } else if (action.equals(SCAN)) {
            scan();
        } else {
            return false;
        }
        return true;
    }

    /**
     * Starts an intent to scan and decode a barcode.
     */
    public void scan() {
        Intent intentScan = new Intent(SCAN_INTENT);
        intentScan.addCategory(Intent.CATEGORY_DEFAULT);
        // avoid calling other phonegap apps
        intentScan.setPackage(this.cordova.getActivity().getApplicationContext().getPackageName());

        this.cordova.startActivityForResult((CordovaPlugin) this, intentScan, REQUEST_CODE);
    }

    /**
     * Called when the barcode scanner intent completes.
     *
     * @param requestCode The request code originally supplied to startActivityForResult(),
     *                       allowing you to identify who this result came from.
     * @param resultCode  The integer result code returned by the child activity through its setResult().
     * @param intent      An Intent, which can return result data to the caller (various data can be attached to Intent "extras").
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                JSONObject obj = new JSONObject();
                try {
                	obj.put(TEXT, intent.getStringExtra("SCAN_RESULT"));
                    obj.put(FORMAT, intent.getStringExtra("SCAN_RESULT_FORMAT"));
                    obj.put(CANCELLED, false);
                    displayScanResults(intent.getStringExtra("SCAN_RESULT"), intent.getStringExtra("SCAN_RESULT_FORMAT"), callbackContext);
                } catch (JSONException e) {
                    Log.d(LOG_TAG, "This should never happen");
                }
                //this.success(new PluginResult(PluginResult.Status.OK, obj), this.callback);
                this.callbackContext.success(obj);
            } else if (resultCode == Activity.RESULT_CANCELED) {
                JSONObject obj = new JSONObject();
                try {
                    obj.put(TEXT, "");
                    obj.put(FORMAT, "");
                    obj.put(CANCELLED, true);
                } catch (JSONException e) {
                    Log.d(LOG_TAG, "This should never happen");
                }
                //this.success(new PluginResult(PluginResult.Status.OK, obj), this.callback);
                this.callbackContext.success(obj);
            } else {
                //this.error(new PluginResult(PluginResult.Status.ERROR), this.callback);
                this.callbackContext.error("Unexpected error");
            }
        }
    }

    /**
     * Initiates a barcode encode.
     *
     * @param type Endoiding type.
     * @param data The data to encode in the bar code.
     */
    public void encode(String type, String data) {
        Intent intentEncode = new Intent(ENCODE_INTENT);
        intentEncode.putExtra(ENCODE_TYPE, type);
        intentEncode.putExtra(ENCODE_DATA, data);
        // avoid calling other phonegap apps
        intentEncode.setPackage(this.cordova.getActivity().getApplicationContext().getPackageName());

        this.cordova.getActivity().startActivity(intentEncode);
    }
    
    @SuppressLint({ "InflateParams", "NewApi" })
	private synchronized void displayScanResults(
    		final String strScanResult, 
    		final String strScanResultFormat,
            final CallbackContext callbackContext) {

		try {
			final String message = strScanResult + "\n\nFormat: " + strScanResultFormat;

			/* THEME_TRADITIONAL 1
			THEME_HOLO_DARK 2
			THEME_HOLO_LIGHT 3
			THEME_DEVICE_DEFAULT_DARK 4
			THEME_DEVICE_DEFAULT_LIGHT 5 */		
			AlertDialog.Builder alertDialog = new AlertDialog.Builder(cordova.getActivity(), 4); 	
			
			// Bill SerGio - Use a scrollable view for large messages!
			LayoutInflater inflater= LayoutInflater.from(cordova.getActivity());
			View view=inflater.inflate(R.layout.barcode_scrollview, null);
			TextView textview=(TextView)view.findViewById(R.id.barcode_textmsg);		
			//final String strDisplayData = myGuid.getDisplayData();
			textview.setPadding(12, 12, 12, 12);
			textview.setText(message);	
			alertDialog
				.setTitle("Scan Result")
				//.setMessage(strDisplayData)
				.setView(view)
				.setCancelable(false)
				.setIcon(R.drawable.ic_launcher)
                .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    @TargetApi(11)
                    public void onClick(DialogInterface dialog, int id) {
                    	dialog.cancel();
                    }
                })
                /* .setNeutralButton("Copy", new DialogInterface.OnClickListener() {
                    @TargetApi(11)
                    public void onClick(DialogInterface dialog, int id) {
                    	ClipboardManager myClipboard;
                       	myClipboard = (ClipboardManager) cordova.getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                    	ClipData myClip;
                    	myClip = ClipData.newPlainText("text", message);
                    	myClipboard.setPrimaryClip(myClip);	
                    	//Toast.makeText(cordova.getActivity(), "Results Pasted",Toast.LENGTH_SHORT).show();
                        cordova.getThreadPool().execute(new Runnable() {
                            public void run() {
                                callbackContext.success("Data Copied!"); // Thread-safe.
                            }
                        });
                    }
                }) */
                .setNeutralButton("Lookup", new DialogInterface.OnClickListener() {
                    @TargetApi(11)
                    public void onClick(DialogInterface dialog, int id) {
                        cordova.getThreadPool().execute(new Runnable() {
                            public void run() {
                                //callbackContext.success("Data Copied!"); // Thread-safe.
                            	doScanSearch(strScanResult, strScanResultFormat, callbackContext);
                            }
                        });
                    }
                })
                .setPositiveButton("Share", new DialogInterface.OnClickListener() {
                    @TargetApi(11)
                    public void onClick(DialogInterface dialog, int id) {
                        doShare(message, callbackContext);
                    }
                }).show();		
		}
		catch (Exception e) {	
			//_appid = "00000000-0000-0000-0000-000000000000";
			e.printStackTrace(); 
		}    
	}

    private void doShare(String zdata, CallbackContext callbackContext) {
        //callbackContext.success(zdata);
    	Intent sendIntent = new Intent();
    	sendIntent.setAction(Intent.ACTION_SEND);
    	sendIntent.putExtra(Intent.EXTRA_TEXT, zdata);
    	sendIntent.setType("text/plain");
        //cordova.getActivity().startActivity(sendIntent);
        this.cordova.getActivity().startActivity(Intent.createChooser(sendIntent, "Share"));
    }    

    private void doScanSearch(String scanResult, String scanResultFormat, CallbackContext callbackContext) {
        if (scanResult != null && scanResult.length() > 0) { 
            callbackContext.success(scanResult);
            String url = "https:www.google.com/search?q=" + scanResult + "&gws_rd=ssl";
            if(scanResultFormat.equals("QR_CODE"))
            {
            	url = scanResult;
            }
    	    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url)); 
            this.cordova.getActivity().startActivity(i);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }
    

    
    
}


