package com.example.bcon.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.bcon.MainActivity;
import com.nexenio.bleindoorpositioning.ble.advertising.AdvertisingPacket;
import com.nexenio.bleindoorpositioning.ble.advertising.IBeaconAdvertisingPacket;
import com.nexenio.bleindoorpositioning.ble.beacon.Beacon;
import com.nexenio.bleindoorpositioning.ble.beacon.BeaconManager;
import com.nexenio.bleindoorpositioning.ble.beacon.BeaconUtil;
import com.nexenio.bleindoorpositioning.ble.beacon.IBeacon;
import com.nexenio.bleindoorpositioning.ble.beacon.filter.BeaconFilter;
import com.nexenio.bleindoorpositioning.location.Location;
import com.nexenio.bleindoorpositioning.location.multilateration.Multilateration;
import com.nexenio.bleindoorpositioning.location.provider.IBeaconLocationProvider;

import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.scan.ScanResult;
import com.polidea.rxandroidble.scan.ScanSettings;

import androidx.annotation.NonNull;

import org.apache.commons.math3.exception.TooManyEvaluationsException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import rx.Observer;
import rx.Subscription;

import static com.nexenio.bleindoorpositioning.IndoorPositioning.getUsableBeacons;

/**
 * Created by steppschuh on 24.11.17.
 */

public class BluetoothClient {

    private static final String TAG = BluetoothClient.class.getSimpleName();
    public static final int REQUEST_CODE_ENABLE_BLUETOOTH = 10;

    private static BluetoothClient instance;

    private Context context;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BeaconManager beaconManager = BeaconManager.getInstance();

    private RxBleClient rxBleClient;
    private Subscription scanningSubscription;

    private BluetoothClient() {

    }

    public static BluetoothClient getInstance() {
        if (instance == null) {
            instance = new BluetoothClient();
        }
        return instance;
    }
    ExampleThread thread;
    public static void initialize(@NonNull Context context) {
        Log.v(TAG, "Initializing with context: " + context);
        BluetoothClient instance = getInstance();
        instance.rxBleClient = RxBleClient.create(context);
        instance.bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        instance.bluetoothAdapter = instance.bluetoothManager.getAdapter();
        if (instance.bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth adapter is not available");
        }

       // ExampleThread thread = new ExampleThread();  //yskim
      //  thread.start();
    }

    public static class ExampleThread extends Thread {
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

                        if (((IBeacon) bc).getMinor() == 5) {

                            ArrayList<IBeaconAdvertisingPacket> advertisingPackets = ((IBeacon) bc).getAdvertisingPackets();

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
                        System.out.print("yskim processScanResult ok Minor 1 minor : "+((IBeacon) bc).getMinor()+"   size : "+((IBeacon) bc).getAdvertisingPackets().size()+"    beacon  revdist: " + bc.getRevdistance()+"    actual : "+bc.getActualdistance()+"   revrssi :"+bc.getRevrssi() +"   max :"+bc.getRevmax() +"   min :"+bc.getRevmin() + "\n");

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
                            System.out.println("yskim processScanResult ok Minor 1 add minor : " + ((IBeacon)keySetList.get(i)).getMinor() +"   dist "+ ((IBeacon)keySetList.get(i)).getRevdistance()+"    max : " +((IBeacon)keySetList.get(i)).getRevmax()+"    min : " +((IBeacon)keySetList.get(i)).getRevmin());
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
                        //MainActivity.displaylocation(location);
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







    public static void startScanning() {
        if (isScanning()) {
            return;
        }

        final BluetoothClient instance = getInstance();
        Log.d(TAG, "Starting to scan for beacons");

        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build();

        instance.scanningSubscription = instance.rxBleClient.scanBleDevices(scanSettings)
                .subscribe(new Observer<ScanResult>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        System.out.print("1>>>>>>>8 Bluetooth scanning error"+"\n");
                        Log.e(TAG, "Bluetooth scanning error", e);
                    }

                    @Override
                    public void onNext(ScanResult scanResult) {
                        instance.processScanResult(scanResult);
                    }
                });
    }

    public static void stopScanning() {
        if (!isScanning()) {
            return;
        }

        BluetoothClient instance = getInstance();
        Log.d(TAG, "Stopping to scan for beacons");
        instance.scanningSubscription.unsubscribe();
    }

    public static boolean isScanning() {
        Subscription subscription = getInstance().scanningSubscription;
        return subscription != null && !subscription.isUnsubscribed();
    }

    public static boolean isBluetoothEnabled() {
        BluetoothClient instance = getInstance();
        return instance.bluetoothAdapter != null && instance.bluetoothAdapter.isEnabled();
    }

    public static void requestBluetoothEnabling(@NonNull Activity activity) {
        Log.d(TAG, "Requesting bluetooth enabling");
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activity.startActivityForResult(enableBtIntent, REQUEST_CODE_ENABLE_BLUETOOTH);
    }

    private void processScanResult(@NonNull ScanResult scanResult) {
        String macAddress = scanResult.getBleDevice().getMacAddress();
        byte[] data = scanResult.getScanRecord().getBytes();

        AdvertisingPacket advertisingPacket = BeaconManager.processAdvertisingData(macAddress, data, scanResult.getRssi());
        if (advertisingPacket != null) {

            Beacon beacon = BeaconManager.getBeacon(macAddress, advertisingPacket);

            Log.d(TAG, "yskim processScanResult : "+((IBeacon) beacon).getMinor()+"  "+scanResult.getBleDevice().getBluetoothDevice().getName()+"   "+ scanResult.getRssi());

            if (beacon instanceof IBeacon && !beacon.hasLocation()) {
                //Log.d(TAG, "yskim processScanResult : "+((IBeacon) beacon).getMinor()+"  "+scanResult.getBleDevice().getBluetoothDevice().getName()+"   "+ scanResult.getRssi());
                beacon.setLocationProvider(createDebuggingLocationProvider((IBeacon) beacon));
            }
            else{
               // System.out.print("yskim lo point >>"+location.getLongitude()+","+location.getLatitude()+"   "+location.getAccuracy()+"\n");

                /*
                if(((IBeacon) beacon).getMinor()==1)
                {
                    ArrayList<IBeaconAdvertisingPacket> advertisingPackets=((IBeacon) beacon).getAdvertisingPackets();
                    for(int i=0;i<advertisingPackets.size();i++)
                    {
                        IBeaconAdvertisingPacket b=advertisingPackets.get(i);
                        System.out.print("yskim processScanResult ok Minor 1 : "+b.getRssi()+"\n");

                    }
                    System.out.print("yskim processScanResult ok Minor 1 : >>>>>>>>>>>>>>>>>>>>>\n");
                }
                */


                //System.out.print("yskim processScanResult ok Minor 22: "+((IBeacon) beacon).getMinor()+"   rssi : "+ scanResult.getRssi()+"    dist : "+((IBeacon) beacon).getDistance()+"\n");
            }
        }
        else {
            //System.out.print("1>>>>>>>7 fail "+macAddress+"\n");
        }

    }

    private static IBeaconLocationProvider<IBeacon> createDebuggingLocationProvider(IBeacon iBeacon) {
        final Location beaconLocation = new Location();
        Log.v(TAG, "IBeaconLocationProvider: " +iBeacon.getMacAddress()+ iBeacon.getMinor());
/*
        moveMaker([127.17473, 37.42309]); //좌상 10 양전무
        moveMaker([127.17474, 37.42303]); //좌하 1604 연희

        moveMaker([127.17492, 37.42311]); //우상 7 내
        moveMaker([127.17492, 37.42304]); //우하 9 회의컴
*/

        switch (iBeacon.getMinor()) {

            //case 10: {
            case 1: {
                //yskim //기둥1
                //37.42028047335332985, 127.17682909002513725
                //beaconLocation.setLatitude(52.512423);
                //beaconLocation.setLongitude(13.390693);
                //beaconLocation.setAltitude(36);
                //37.42029044723202702,127.17699209169984442
                beaconLocation.setLatitude(37.423032499343236);
                beaconLocation.setLongitude(127.17487241030128);
                beaconLocation.setElevation(1);
                beaconLocation.setAltitude(0);
                break;
            }
            case 6: {  //2번대신
                //yskim //기둥2

                //37.42020695161892974 127.17699608125133182
                //beaconLocation.setLatitude(52.512393);
                //beaconLocation.setLongitude(13.390692);
                //beaconLocation.setAltitude(36);
                beaconLocation.setLatitude(37.423071906756235);
                beaconLocation.setLongitude(127.17486939281615);
                beaconLocation.setElevation(1);
                beaconLocation.setAltitude(0);
                break;
            }

            case 5: {
                //가운데것
                beaconLocation.setLatitude(37.42306698);
                beaconLocation.setLongitude(127.1748097);
                beaconLocation.setElevation(1);
                beaconLocation.setAltitude(0);
                break;
            }

            case 2: {
                //가운데것
                beaconLocation.setLatitude(37.42302797);
                beaconLocation.setLongitude(127.1748136);
                beaconLocation.setElevation(1);
                beaconLocation.setAltitude(0);
                break;
            }

            case 3: {
                //yskim  //기둥3
                //37.42019754767616035,127.17683336454457788
                //beaconLocation.setLatitude(52.512419);
                //beaconLocation.setLongitude(13.390825);
                //beaconLocation.setAltitude(36);
                //37.42028047335332985 127.17682909002513725
                beaconLocation.setLatitude(37.42302344628594);
                beaconLocation.setLongitude(127.17475472838073);
                beaconLocation.setElevation(1);
                beaconLocation.setAltitude(0);
                break;
            }


            case 4: {
                //yskim  //기둥4
                //beaconLocation.setLatitude(52.51241);
                //beaconLocation.setLongitude(13.390822);
                //beaconLocation.setAltitude(36);
                //yskim  //기둥4
                //37.42019754767616035 127.17683336454457788
                beaconLocation.setLatitude(37.42306205490493);
                beaconLocation.setLongitude(127.17475003451499);
                beaconLocation.setElevation(1);
                beaconLocation.setAltitude(0);
                break;
            }

//////////////////////////////////////////////////////////////////////////


        }
        return new IBeaconLocationProvider<IBeacon>(iBeacon) {
            @Override
            protected void updateLocation() {
                this.location = beaconLocation;
            }

            @Override
            protected boolean canUpdateLocation() {
                return true;
            }
        };
    }

}
