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
import com.nexenio.bleindoorpositioning.ble.beacon.IBeacon;
import com.nexenio.bleindoorpositioning.ble.beacon.filter.BeaconFilter;
import com.nexenio.bleindoorpositioning.ble.beacon.filter.IBeaconFilter;
import com.nexenio.bleindoorpositioning.location.Location;
import com.nexenio.bleindoorpositioning.location.LocationListener;
import com.nexenio.bleindoorpositioning.location.provider.LocationProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static com.nexenio.bleindoorpositioning.IndoorPositioning.getUsableBeacons;


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


    Timer timer;//타이머
    TimerTask timerTask;


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



        /**
         * 타이머시작
         */
        // 타이머 및  태스크 초기화
        if(timer != null){
            timer.cancel();
            timer = null;
        }

        if(messagehandler!=null) {
            //yskim
            messagehandler = null;
        }
        // 타이머후처리 핸들러 인스턴스
        messagehandler = new Messagehandler();

        // 타이머 task : 일종의 스레드...
        timerTask = new TimerTask() {
            @Override
            public void run() {
                // 스레드에서 바로 스케줄러를 실행하면 UI작업에서 오류가날수 있으므로, 콜백핸들러에서 스케줄러 실행..
                messagehandler.sendEmptyMessage(1);//스케줄타이머작업 타입
            }
        };

        // 타이머시작실행...
        timer = new Timer();
        try{
            timer.schedule( timerTask, 100, 5*1000);
            Log.d("BTS", "타이머시작...");
        }catch (Exception e){
            Log.d("BTS", "타이머시작 실패...");
        }
        



        /**
         * 웹뷰 초기화
         */

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



        //AndroidLocationProvider.requestLocationEnabling(MainActivity.this);

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

        // observe bluetooth
        if (!BluetoothClient.isBluetoothEnabled()) {
            BluetoothClient.requestBluetoothEnabling(MainActivity.this);
        }
        BluetoothClient.startScanning();




        // observe location
        if (!AndroidLocationProvider.hasLocationPermission(this)) {
            AndroidLocationProvider.requestLocationPermission(this);
        } else if (!AndroidLocationProvider.isLocationEnabled(this)) {
            AndroidLocationProvider.requestLocationEnabling(MainActivity.this);
        }
        else{
            AndroidLocationProvider.startRequestingLocationUpdates();
            AndroidLocationProvider.requestLastKnownLocation();
        }

    }





















    //테스트용데이터...
    String[] ary = {"[14157033.626301805,4498237.59855357]","[14157023.34074761,4498236.739933394]","[14157023.34074761,4498236.739933394]","[14157032.685890839,4498237.520049699]","[14157023.34074761,4498236.739933394]","[14157033.626301805,4498237.59855357]"};
    String[] sBcAry = { "[{minor:10,dist:15}, {minor:7,dist:0.5}, {minor:9,dist:1.2},  {minor:1604,dist:16}]",
                        "[{minor:10,dist:14}, {minor:7,dist:1.5}, {minor:9,dist:3.2},  {minor:1604,dist:13}]",
                        "[{minor:10,dist:13}, {minor:7,dist:2.5}, {minor:9,dist:4.2},  {minor:1604,dist:13}]",
                        "[{minor:10,dist:12}, {minor:7,dist:3.5}, {minor:9,dist:5.2},  {minor:1604,dist:11}]",
                        "[{minor:10,dist:11}, {minor:7,dist:4.5}, {minor:9,dist:6.2},  {minor:1604,dist:11}]",
                        "[{minor:10,dist:10}, {minor:7,dist:5.5}, {minor:9,dist:8.2},  {minor:1604,dist:10}]",
                        "[{minor:10,dist:9},  {minor:7,dist:6.5}, {minor:9,dist:8.2},  {minor:1604,dist:6} ]",
                        "[{minor:10,dist:8},  {minor:7,dist:8.5}, {minor:9,dist:10.2}, {minor:1604,dist:6}]",
    };
    int cnt = 0;

    /************************************************************
     * 일반
     */
    // 이벤트 핸들러
    public class Messagehandler extends Handler {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what){
                case 0 :
                    //비콘로케이션 이벤트
                    String pos = (String) msg.obj;
                    //mWebView.loadUrl("javascript:pointMarker(" + pos + ")");
                    break;

                case 1 :
                    //mWebView.loadUrl("javascript:pointMarker("+ary[(cnt++)%6]+")"); //테스트포인트
                    //테스트움직임
                    mWebView.loadUrl("javascript:pointMarker(fn_bcSet("+sBcAry[(cnt++)%8]+"))");//기준비콘동심원
                    mWebView.loadUrl("javascript:pointMarker(gfn_calLatis("+sBcAry[(cnt++)%8]+"))");//포지션격자표시
                    //mWebView.loadUrl("javascript:lineMarker(gfn_calLatis("+sBcAry[(cnt++)%8]+"))");//포지션라인으로표시

//                    //타이머간격으로 비콘의 현재거리 가져오기
//                    //List<Beacon> blist =  getBeacons();  //packet manager에 들어온것
//                    List<Beacon> blist = getUsableBeacons(BeaconManager.getInstance().getBeaconMap().values()); //location 구할때 사용되는것
//
//                    //3개이상인경우만 위치업데이트
//                    if(!blist.isEmpty() && blist.size()>2){
//                        String bcAry = "[";
//                        for (Beacon beacon : blist) {
//                            bcAry += "{'minor':'"+((IBeacon)beacon).getMinor()+"', 'dist': "+beacon.getDistance()+"},";
//                            System.out.print("yskim lo beacon Minor >>"+((IBeacon)beacon).getMinor()+"  Distance: "+beacon.getDistance()+"\n");
//                        }
//                        bcAry += "]";
//
//                        //자바스크립트 호출
//                        //mWebView.loadUrl("javascript:pointMarker(gfn_calLatis("+bcAry+"))");
//                    }
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


                    if(location.getAccuracy()<2.8)
                    {
                        System.out.print("yskim lo point >>"+location.getLongitude()+","+location.getLatitude()+"   "+location.getAccuracy()+"\n");
                        Message msg = messagehandler.obtainMessage();
                        msg.what = 0;
                        msg.obj = "[" + location.getLongitude() + "," + location.getLatitude() + "]";
                        messagehandler.sendMessage(msg);
                    }
                    else
                    {
                        System.out.print("yskim lo point <<"+location.getLongitude()+","+location.getLatitude()+"   "+location.getAccuracy()+"\n");
                        //정확성떨어지는것
                    }
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