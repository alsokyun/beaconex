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
import com.nexenio.bleindoorpositioning.ble.advertising.AdvertisingPacket;
import com.nexenio.bleindoorpositioning.ble.advertising.IBeaconAdvertisingPacket;
import com.nexenio.bleindoorpositioning.ble.advertising.IndoorPositioningAdvertisingPacket;
import com.nexenio.bleindoorpositioning.ble.beacon.Beacon;
import com.nexenio.bleindoorpositioning.ble.beacon.BeaconManager;
import com.nexenio.bleindoorpositioning.ble.beacon.BeaconUpdateListener;
import com.nexenio.bleindoorpositioning.ble.beacon.BeaconUtil;
import com.nexenio.bleindoorpositioning.ble.beacon.IBeacon;
import com.nexenio.bleindoorpositioning.ble.beacon.filter.BeaconFilter;
import com.nexenio.bleindoorpositioning.ble.beacon.filter.IBeaconFilter;
import com.nexenio.bleindoorpositioning.location.Location;
import com.nexenio.bleindoorpositioning.location.LocationListener;
import com.nexenio.bleindoorpositioning.location.multilateration.Multilateration;
import com.nexenio.bleindoorpositioning.location.provider.LocationProvider;

import org.apache.commons.math3.exception.TooManyEvaluationsException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.nexenio.bleindoorpositioning.IndoorPositioning.getUsableBeacons;


public class MainActivity extends AppCompatActivity  {


    /**
     * 비콘관련
     */
    protected BeaconManager beaconManager = BeaconManager.getInstance();
    protected LocationListener deviceLocationListener;
    protected BeaconUpdateListener beaconUpdateListener;
    protected List<BeaconFilter> beaconFilters = new ArrayList<>();

    // TODO: Remove legacy uuid once all beacons are updated
    // protected IBeaconFilter uuidFilter = new IBeaconFilter(IndoorPositioningAdvertisingPacket.INDOOR_POSITIONING_UUID);
    //protected IBeaconFilter uuidFilter = new IBeaconFilter(IndoorPositioningAdvertisingPacket.INDOOR_POSITIONING_UUID, UUID.fromString("acfd065e-c3c0-11e3-9bbe-1a514932ac01"));  //yskim
    //PNT 44790ba4-7eb3-4095-9e14-4b43ae67512b
    //HYCON CCE99BED-E080-04C4-1A91-1A1A29B64111
    //cce99bed-e080-04c4-1a91-1a1a29b64111
    protected IBeaconFilter uuidFilter = new IBeaconFilter(IndoorPositioningAdvertisingPacket.INDOOR_POSITIONING_UUID, UUID.fromString("cce99bed-e080-04c4-1a91-1a1a29b64111"));  //yskim


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

        FindPoint();

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


/*
        String temp="[127.17473,37.42309]";  //10
        mWebView.loadUrl("javascript:pointMaker2(" + temp + ",,\"50\")");
        temp="[127.17492,37.42304]";  //9
        mWebView.loadUrl("javascript:pointMaker2(" + temp + ",\"50\")");
        temp="[127.17492,37.42311]";  //7
        mWebView.loadUrl("javascript:pointMaker2(" + temp + ",\"50\")");
        temp="[127.17474,37.42303]";  //1604
        mWebView.loadUrl("javascript:pointMaker2(" + temp + ",\"50\")");
    */
    }







    public class ExampleThread extends Thread {
        private static final String TAG = "ExampleThread";

        public ExampleThread() {
            // 초기화 작업 } public void run() { // 스레드에게 수행시킬 동작들 구현 }
        }

        public void run()
        {
            int second = 0;
            while (true) {


                try{
                    AdvertisingPacket latestAdvertisingPacket;
                    //List<String> inactiveBeaconKeys = new ArrayList<>();
                    //float beacon_arr[]=new float[BeaconManager.beaconMap.size()];
                    //int idx=0;
                    Map<Beacon, Float> testMap = new HashMap<Beacon,Float>();

                    for (Iterator<Map.Entry<String, Beacon>> beaconMapIterator = BeaconManager.beaconMap.entrySet().iterator(); beaconMapIterator.hasNext(); ) {

                        Map.Entry<String, Beacon> beaconEntry = beaconMapIterator.next();
                        latestAdvertisingPacket = beaconEntry.getValue().getLatestAdvertisingPacket();
                        Beacon bc=beaconEntry.getValue();

                        // 스레드에게 수행시킬 동작들 구현

                        if (((IBeacon) bc).getMinor() == 1) {

                            ArrayList<IBeaconAdvertisingPacket> advertisingPackets = ((IBeacon) bc).getYskimadvertisingPackets();

                            double tvalue=0,max=0, min=0;
                            int removecount=3;
                            int remove_persent=30;
                            int size=advertisingPackets.size();
                            float arr[]=new float[size];

                            int remove_cnt=(int) (((float)remove_persent/100)*advertisingPackets.size());


                            if(advertisingPackets.size()>10)
                            {
                                for (int i = 0; i < advertisingPackets.size(); i++) {
                                    IBeaconAdvertisingPacket b = advertisingPackets.get(i);
                                    arr[i]=b.getRssi();

                                    System.out.print("yskim processScanResult ok Minor 1 value : " + b.getRssi() + "\n");

                                    tvalue+=b.getRssi();
                                }

                                Arrays.sort(arr);


                                double caltvalue=0;

                                for(int i=(remove_cnt);i<(arr.length-remove_cnt);i++)
                                {
                                    caltvalue+=arr[i];
                                }
                                float revaverage=(float)(caltvalue/(arr.length-(remove_cnt*2)));
                                float revdist= (float) Math.pow(10, (-59 - revaverage ) / (10 * 1.7f));


                                System.out.print("yskim processScanResult ok Minor 1 max  : " + arr[0]+ "    min  : " + arr[size-1]+ "\n");
                                System.out.print("yskim processScanResult ok Minor 1 average : "+tvalue/advertisingPackets.size()+"    last dist : " +  bc.getDistance() + "\n");
                                System.out.print("yskim processScanResult ok Minor 1 rev average : " + revaverage +"   rev dist :"+revdist + "\n");
                                System.out.print("yskim processScanResult ok Minor 1 beacon  revdist: " + bc.getRevdistance()+"   revrssi :"+bc.getRevrssi() + "\n");
                                System.out.print("yskim processScanResult ok Minor 1 : >>>>>>>>>>>>>>>>>>>>>\n");
                            }
                        }  //minor filter

                        ((IBeacon) bc).trimAdvertisingPackets(); //시간이 지난것들은 지우고
                        ((IBeacon) bc).calculateRssiAverage();



                        if(bc.getRevdistance()>0)
                        {
                            testMap.put(bc,bc.getRevdistance());
                        }

                        //if (((IBeacon) bc).getMinor() == 7)
                        System.out.print("yskim processScanResult ok Minor 1 minor : "+((IBeacon) bc).getMinor()+"   size : "+((IBeacon) bc).getAdvertisingPackets().size()+"    beacon  revdist: " + bc.getRevdistance()+"    RSSI : "+bc.getRssi() +"   revrssi :"+bc.getRevrssi() +"   max :"+bc.getRevmax() +"   min :"+bc.getRevmin() + "\n");

                    }

                    //Object[] mapkey = testMap.keySet().toArray();
                    //Arrays.sort(mapkey);

                    List<Beacon> keySetList = new ArrayList<>(testMap.keySet());

                    // 오름차순
                    System.out.println("------value 오름차순------");

                    Collections.sort(keySetList, (o1, o2) -> (testMap.get(o1).compareTo(testMap.get(o2))));


                    for(Beacon key : keySetList) {
                        System.out.println("yskim processScanResult ok Minor 1 sort : key : " + ((IBeacon)key).getMinor() +"   "+ ((IBeacon)key).getLocation()+" / " + "value : " + testMap.get(key));
                    }



                    //내비콘인지 uuid
                    //가까운 3개를 추가한다.
                    //List<Beacon> usableBeacons=getUsableBeacons(BeaconManager.getInstance().getBeaconMap().values());


                    List<Beacon> usableBeacons = new ArrayList<>();
                    for(int i=0;i<keySetList.size();i++)
                    {
                        if(keySetList.get(i).getLocation().getLatitude()>0)
                        {
                            System.out.println("yskim processScanResult ok Minor 1 add minor : " + ((IBeacon)keySetList.get(i)).getMinor() +"   size : "+((IBeacon)keySetList.get(i)).getAdvertisingPackets().size()+"    dist "+ ((IBeacon)keySetList.get(i)).getRevdistance()+"    aver Rssi : " +((IBeacon)keySetList.get(i)).getRevrssi()+"    last Rssi : " +((IBeacon)keySetList.get(i)).getLatestAdvertisingPacket().getRssi()+"    max : " +((IBeacon)keySetList.get(i)).getRevmax()+"    min : " +((IBeacon)keySetList.get(i)).getRevmin());
                            usableBeacons.add(keySetList.get(i));
                        }
                    }

                    //System.out.print("yskim processScanResult ok Minor 1 add : ===========================\n");



                    if (usableBeacons.size() < 3) {
                        Thread.sleep(1000);
                        continue;
                    } else if (usableBeacons.size() > 3) {
                        Collections.sort(usableBeacons, BeaconUtil.revDescendingRssiComparator);
                        int maximumBeaconIndex = Math.min(10, usableBeacons.size());
                        int firstRemovableBeaconIndex = maximumBeaconIndex;
                        for (int beaconIndex = 3; beaconIndex < maximumBeaconIndex; beaconIndex++) {
                            if (usableBeacons.get(beaconIndex).getFilteredRssi() < -70) {
                                firstRemovableBeaconIndex = beaconIndex;
                                break;
                            }
                        }
                        usableBeacons.subList(firstRemovableBeaconIndex, usableBeacons.size()).clear();
                    }


                    Multilateration multilateration = new Multilateration(usableBeacons);

                    try {
                        Location location = multilateration.getLocation();

                        // The root mean square of multilateration is used to filter out inaccurate locations.
                        // Adjust value to allow location updates with higher deviation

                        System.out.print("yskim processScanResult ok Minor 1 add location : "+location.getLongitude()+","+location.getLatitude()+"    RMS : "+multilateration.getRMS() +"\n");
                        System.out.print("yskim processScanResult ok Minor 1 add : ===========================\n");

                        displaylocation(location);
                        displayPoint();
                        //locationListener.onLocationUpdated(this, location);

                        /*
                        if (multilateration.getRMS() < rootMeanSquareThreshold) {
                            locationPredictor.addLocation(location);
                            System.out.print("yskim onLocationUpdated after : "+multilateration.getRMS() +"\n");
                            //onLocationUpdated(location);
                        }
                        onLocationUpdated(location);
                        */
                    } catch (TooManyEvaluationsException e) {
                        // see https://github.com/neXenio/BLE-Indoor-Positioning/issues/73
                    }


                    Thread.sleep(1000);

                }catch (InterruptedException e) {
                    e.printStackTrace(); ;
                } Log.i("경과된 시간 : ", Integer.toString(second));

            }
        }
    }


    /************************************************************
     * 비콘관련 클래스, 메소드
     */

    public void InitAttacch()
    {
        beaconFilters.add(uuidFilter);
        IndoorPositioning.getInstance().setIndoorPositioningBeaconFilter(uuidFilter);
        IndoorPositioning.registerLocationListener(deviceLocationListener);
        AndroidLocationProvider.registerLocationListener(deviceLocationListener);
        AndroidLocationProvider.requestLastKnownLocation();
        BeaconManager.registerBeaconUpdateListener(beaconUpdateListener);

        ExampleThread thread = new ExampleThread();  //yskim
        thread.start();

        float revdistance1 = (float) Math.pow(10, (-59 - (-60) ) / (10 * 1.4f));
        float revdistance2 = (float) Math.pow(10, (-59 - (-65) ) / (10 * 1.4f));
        float revdistance3 = (float) Math.pow(10, (-59 - (-70) ) / (10 * 1.4f));

        float revdistance4 = (float) Math.pow(10, (-59 - (-75) ) / (10 * 1.4f));
        float revdistance5 = (float) Math.pow(10, (-59 - (-80) ) / (10 * 1.4f));

        System.out.print("1 : "+revdistance1+"  2 : "+revdistance2+"  3 : "+revdistance3+"   4 : "+revdistance4+"   5 : "+revdistance5);
    }


    public void initDetach()
    {
        IndoorPositioning.unregisterLocationListener(deviceLocationListener);
        AndroidLocationProvider.unregisterLocationListener(deviceLocationListener);
        BeaconManager.unregisterBeaconUpdateListener(beaconUpdateListener);
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
                    String b = (String) msg.obj;
                    mWebView.loadUrl("javascript:pointMaker2(" + b + ")");
                    //mWebView.loadUrl("javascript:addRandomFeature()");
                    break;

                default:
                    break;
            }

        }
    }


    public void displaylocation(Location location)
    {
        {
            System.out.print("yskim lo beacon Distance Minor point : "+location.getLongitude()+","+location.getLatitude()+"   "+location.getAccuracy()+"\n");
            Message msg = messagehandler.obtainMessage();
            msg.what = 0;
            msg.obj = "[" + location.getLongitude() + "," + location.getLatitude() + "]";
            messagehandler.sendMessage(msg);//스케줄타이머작업 타입
        }
    }

/////////////////////////////////////////////////////////////////////////////

    //Point arrp[]=
    public int  FindPoint()
    {
        int idx=-1;
        double dist=100;
        for(int i=0;i<10;i++) {
            double temp=distance(127.17486939281615, 37.423071906756235, 127.1748097, 37.42306698);
            if(dist>temp) {
                dist=temp;
                idx=i;
            }
        }

        return idx;
    }
    private  double distance(double lat1, double lon1, double lat2, double lon2) {

        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));

        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;


        dist = dist * 1609.344;


        return (dist);
    }

   // This function converts decimal degrees to radians
    private  double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    // This function converts radians to decimal degrees
    private  double rad2deg(double rad) {
        return (rad * 180 / Math.PI);
    }
    /////////////////////////////////////////////////////////////////////////////
    public void displayPoint()
    {
        List<Beacon> blist =  getBeacons();  //packet manager에 들어온것
        //List<Beacon> blist = getUsableBeacons(BeaconManager.getInstance().getBeaconMap().values()); //location 구할때 사용되는것

        float max_dist=0;
        float dist1=0;
        float dist2=0;
        float dist3=0;
        float dist4=0;
        float dist5=0;
        float dist6=0;


        for (Beacon beacon : blist) {

            System.out.print("yskim lo beacon Distance Minor >>"+((IBeacon)beacon).getMinor()+"  Distance: "+beacon.getRevdistance()+"   RSSI : "+beacon.getRssi()+"\n");
            if(max_dist<beacon.getRevdistance())
                max_dist=beacon.getRevdistance();


            if(((IBeacon)beacon).getMinor()==1)
            {
                dist1=beacon.getRevdistance();
                //String temp="1604,[127.17474,37.42303],"+beacon.getDistance();  //1604
                //mWebView.loadUrl("javascript:pointMaker2(" + temp + ")");
            }
            else if(((IBeacon)beacon).getMinor()==2)
            {
                dist2=beacon.getRevdistance();
                //String temp="7,[127.17492,37.42311],"+beacon.getDistance();  //7
                //mWebView.loadUrl("javascript:pointMaker2(" + temp + ")");
            }
            else if(((IBeacon)beacon).getMinor()==5)
            {
                dist5=beacon.getRevdistance();
                //String temp="7,[127.17492,37.42311],"+beacon.getDistance();  //7
                //mWebView.loadUrl("javascript:pointMaker2(" + temp + ")");
            }

            else if(((IBeacon)beacon).getMinor()==3)
            {
                dist3=beacon.getRevdistance();
                //String temp="9,[127.17492,37.42304],"+beacon.getDistance();  //9
                //mWebView.loadUrl("javascript:pointMaker2(" + temp + ")");
            }
            else if(((IBeacon)beacon).getMinor()==4)
            {
                dist4=beacon.getRevdistance();
                //String temp="9,[127.17492,37.42304],"+beacon.getDistance();  //9
                //mWebView.loadUrl("javascript:pointMaker2(" + temp + ")");
            }
            else if(((IBeacon)beacon).getMinor()==6)
            {
                dist6=beacon.getRevdistance();
                //String temp="7,[127.17492,37.42311],"+beacon.getDistance();  //7
                //mWebView.loadUrl("javascript:pointMaker2(" + temp + ")");
            }


        }

        if(max_dist>20)
            max_dist=20;

        //System.out.print("yskim lo beacon Distance max_dist >>"+max_dist+"\n");




        Message msg = messagehandler.obtainMessage();
        msg.what = 1;
        String temp="1,[127.17487241030128, 37.4230324993432364],"+max_dist+","+dist1;  //9
        msg.obj = temp;
        messagehandler.sendMessage(msg);//스케줄타이머작업 타입

        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Message msg2 = messagehandler.obtainMessage();
        msg2.what = 1;
        temp="2,[127.1748136, 37.42302797],"+max_dist+","+dist2;  //7
        msg2.obj = temp;
        messagehandler.sendMessage(msg2);//스케줄타이머작업 타입
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Message msg3 = messagehandler.obtainMessage();
        msg3.what = 1;
        temp="3,[127.17475472838073, 37.42302344628594],"+max_dist+","+dist3;  //1604
        msg3.obj = temp;
        messagehandler.sendMessage(msg3);//스케줄타이머작업 타입
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Message msg4 = messagehandler.obtainMessage();
        msg4.what = 1;
        temp="4,[127.17475003451499, 37.42306205490493],"+max_dist+","+dist4;  //1604
        msg4.obj = temp;
        messagehandler.sendMessage(msg4);//스케줄타이머작업 타입
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Message msg5 = messagehandler.obtainMessage();
        msg5.what = 1;
        temp="5,[127.1748097, 37.42306698],"+max_dist+","+dist5;  //1604
        msg5.obj = temp;
        messagehandler.sendMessage(msg5);//스케줄타이머작업 타입
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Message msg6 = messagehandler.obtainMessage();
        msg6.what = 1;
        temp="6,[127.17486939281615, 37.423071906756235],"+max_dist+","+dist6;  //7
        msg6.obj = temp;
        messagehandler.sendMessage(msg6);//스케줄타이머작업 타입
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        /*
        String temp="1,[127.17492,37.42304],"+max_dist+","+dist9;  //9
        mWebView.loadUrl("javascript:pointMaker2(" + temp + ")");
        temp="2,[127.17492,37.42311],"+max_dist+","+dist7;  //7
        mWebView.loadUrl("javascript:pointMaker2(" + temp + ")");
        temp="3,[127.17474,37.42303],"+max_dist+","+dist1604;  //1604
        mWebView.loadUrl("javascript:pointMaker2(" + temp + ")");

        temp="4,[127.17473,37.42309],"+max_dist+","+dist4;  //1604
        mWebView.loadUrl("javascript:pointMaker2(" + temp + ")");
        */


        /*
        //if(location.getAccuracy()<100)
        {
            System.out.print("yskim lo beacon Distance Minor point : "+location.getLongitude()+","+location.getLatitude()+"   "+location.getAccuracy()+"\n");
            Message msg = messagehandler.obtainMessage();
            msg.what = 0;
            msg.obj = "[" + location.getLongitude() + "," + location.getLatitude() + "]";
            messagehandler.sendMessage(msg);//스케줄타이머작업 타입
        }
        System.out.print("yskim lo beacon Distance Minor =======================================================================\n");

         */
    }

    protected LocationListener createDeviceLocationListener() {
        return new LocationListener() {
            @Override
            public void onLocationUpdated(LocationProvider locationProvider, Location location) {
                if (locationProvider == IndoorPositioning.getInstance()) {
                    //displayPoint();
                }
            }
        };
    }


    protected List<Beacon> getBeacons() {
        if (beaconFilters.isEmpty()) {
            return new ArrayList<>(beaconManager.getBeaconMap().values());
        }
        List<Beacon> beacons = new ArrayList<>();
        for (Beacon beacon : beaconManager.getBeaconMap().values()) {
            for (BeaconFilter beaconFilter : beaconFilters) {
                if (beaconFilter.matches(beacon)) {
                    beacons.add(beacon);
                    break;
                }
            }
        }
        return beacons;
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