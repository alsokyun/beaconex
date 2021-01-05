package com.example.bcon;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bcon.bluetooth.BluetoothClient;
import com.example.bcon.location.AndroidLocationProvider;
import com.nexenio.bleindoorpositioning.IndoorPositioning;
import com.nexenio.bleindoorpositioning.ble.advertising.IndoorPositioningAdvertisingPacket;
import com.nexenio.bleindoorpositioning.ble.beacon.Beacon;
import com.nexenio.bleindoorpositioning.ble.beacon.BeaconManager;
import com.nexenio.bleindoorpositioning.ble.beacon.BeaconUpdateListener;
import com.nexenio.bleindoorpositioning.ble.beacon.filter.BeaconFilter;
import com.nexenio.bleindoorpositioning.ble.beacon.filter.IBeaconFilter;
import com.nexenio.bleindoorpositioning.location.Location;
import com.nexenio.bleindoorpositioning.location.LocationListener;
import com.nexenio.bleindoorpositioning.location.provider.LocationProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class MainActivity extends AppCompatActivity  {


    /**
     * 비콘관련
     */
    protected LocationListener deviceLocationListener;
    protected BeaconUpdateListener beaconUpdateListener;
    protected List<BeaconFilter> beaconFilters = new ArrayList<>();

    // TODO: Remove legacy uuid once all beacons are updated
    // protected IBeaconFilter uuidFilter = new IBeaconFilter(IndoorPositioningAdvertisingPacket.INDOOR_POSITIONING_UUID);
    //protected IBeaconFilter uuidFilter = new IBeaconFilter(IndoorPositioningAdvertisingPacket.INDOOR_POSITIONING_UUID, UUID.fromString("acfd065e-c3c0-11e3-9bbe-1a514932ac01"));  //yskim
    protected IBeaconFilter uuidFilter = new IBeaconFilter(IndoorPositioningAdvertisingPacket.INDOOR_POSITIONING_UUID, UUID.fromString("44790ba4-7eb3-4095-9e14-4b43ae67512b"));


    /**
     * 웹뷰관련
     */
    private WebView mWebView;
    Messagehandler messagehandler;//메세지핸들러



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        /**
         * 비콘장비관련 초기화
         */
        // setup location
        AndroidLocationProvider.initialize(this);

        // setup bluetooth
        BluetoothClient.initialize(this);

        deviceLocationListener = createDeviceLocationListener();
        beaconUpdateListener = createBeaconUpdateListener();

        InitAttacch();



//aaaaaaaaaaaaaaaaaaaaaaaaaaa

        /**
         * 웹뷰 초기화
         */
        // 타이머후처리 핸들러 인스턴스
        if(messagehandler!=null) {
            messagehandler = null;
        }
        messagehandler = new Messagehandler();

        mWebView = findViewById(R.id.webView);

        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setSupportZoom(true);
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.getSettings().setDatabaseEnabled(true);
        mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

        //CORS 허용
        mWebView.getSettings().setAllowFileAccess(true);
        mWebView.getSettings().setAllowContentAccess(true);
        mWebView.getSettings().setAllowFileAccessFromFileURLs(true);
        mWebView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        //동영상플레이 FullScreen 관련설정
        mWebView.getSettings().setMediaPlaybackRequiresUserGesture(false);

        WebView.setWebContentsDebuggingEnabled(true); //크롬디버깅허용


        /**
         * 1.WebViewClient 정의
         *  로딩후처리 선언
         */
        mWebView.setWebViewClient(new WebViewClient(){

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                //세션전달
                //view.loadUrl("javascript:_setPath('"+ file_path + "')");
                super.onPageFinished(view, url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return super.shouldOverrideUrlLoading(view, request);
            }


        });



        //페이지로딩
        mWebView.loadUrl("file:///android_asset/WWW/index.html");


        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        AndroidLocationProvider.requestLocationEnabling(MainActivity.this);

    }



    @Override
    protected void onDestroy(){
        super.onDestroy();
        initDetach();
        finish();
    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case BluetoothClient.REQUEST_CODE_ENABLE_BLUETOOTH: {
                if (resultCode == RESULT_OK) {
                    Log.d("main", "Bluetooth enabled, starting to scan");
                    BluetoothClient.startScanning();
                } else {
                    Log.d("main", "Bluetooth not enabled, invoking new request");
                    BluetoothClient.requestBluetoothEnabling(this);
                }
                break;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // observe location
        if (!AndroidLocationProvider.hasLocationPermission(this)) {
            AndroidLocationProvider.requestLocationPermission(this);
        } else if (!AndroidLocationProvider.isLocationEnabled(this)) {
            //requestLocationServices();
        }
        AndroidLocationProvider.startRequestingLocationUpdates();
        AndroidLocationProvider.requestLastKnownLocation();

        // observe bluetooth
        if (!BluetoothClient.isBluetoothEnabled()) {
            //requestBluetooth();
        }
        BluetoothClient.startScanning();

        String temp="[127.17473,37.42309]";  //10
        mWebView.loadUrl("javascript:pointMaker2(" + temp + ")");
        temp="[127.17492,37.42304]";  //9
        mWebView.loadUrl("javascript:pointMaker2(" + temp + ")");
        temp="[127.17492,37.42311]";  //7
        mWebView.loadUrl("javascript:pointMaker2(" + temp + ")");
        temp="[127.17474,37.42303]";  //1604
        mWebView.loadUrl("javascript:pointMaker2(" + temp + ")");

    }

























    /************************************************************
     * 일반
     */
    // 타이머후처리 핸들러
    public class Messagehandler extends Handler {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what){
                case 0 :
                    String pos = (String) msg.obj;
                    mWebView.loadUrl("javascript:pointMaker(" + pos + ")");
                    break;

                case 1 :
                    //mWebView.loadUrl("javascript:addRandomFeature()");
                    break;

                default:
                    break;
            }

        }
    }




    /************************************************************
     * 비콘관련 클래스, 메소드
     */



    public void InitAttacch()
    {
        IndoorPositioning.getInstance().setIndoorPositioningBeaconFilter(uuidFilter);
        IndoorPositioning.registerLocationListener(deviceLocationListener);
        AndroidLocationProvider.registerLocationListener(deviceLocationListener);
        AndroidLocationProvider.requestLastKnownLocation();
        BeaconManager.registerBeaconUpdateListener(beaconUpdateListener);
    }


    public void initDetach()
    {
        IndoorPositioning.unregisterLocationListener(deviceLocationListener);
        AndroidLocationProvider.unregisterLocationListener(deviceLocationListener);
        BeaconManager.unregisterBeaconUpdateListener(beaconUpdateListener);
    }

    protected LocationListener createDeviceLocationListener() {
        return new LocationListener() {
            @Override
            public void onLocationUpdated(LocationProvider locationProvider, Location location) {
                if (locationProvider == IndoorPositioning.getInstance()) {
                    System.out.print("yskim lo >>"+location.getLongitude()+"\n");

                    Message msg = messagehandler.obtainMessage();
                    msg.what = 0;
                    msg.obj = "[" + location.getLongitude() + "," + location.getLatitude() + "]";
                    messagehandler.sendMessage(msg);//스케줄타이머작업 타입

                }
            }
        };
    }


    protected BeaconUpdateListener createBeaconUpdateListener() {
        return new BeaconUpdateListener() {
            @Override
            public void onBeaconUpdated(Beacon beacon) {

                //beaconRadar.setBeacons(getBeacons());
            }
        };
    }

}