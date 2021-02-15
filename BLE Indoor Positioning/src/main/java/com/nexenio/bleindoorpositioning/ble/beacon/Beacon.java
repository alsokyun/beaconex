package com.nexenio.bleindoorpositioning.ble.beacon;

import com.nexenio.bleindoorpositioning.ble.advertising.AdvertisingPacket;
import com.nexenio.bleindoorpositioning.ble.advertising.AdvertisingPacketUtil;
import com.nexenio.bleindoorpositioning.ble.advertising.EddystoneAdvertisingPacket;
import com.nexenio.bleindoorpositioning.ble.advertising.IBeaconAdvertisingPacket;
import com.nexenio.bleindoorpositioning.ble.beacon.signal.KalmanFilter;
import com.nexenio.bleindoorpositioning.ble.beacon.signal.RssiFilter;
import com.nexenio.bleindoorpositioning.ble.beacon.signal.WindowFilter;
import com.nexenio.bleindoorpositioning.location.Location;
import com.nexenio.bleindoorpositioning.location.distance.BeaconDistanceCalculator;
import com.nexenio.bleindoorpositioning.location.provider.BeaconLocationProvider;
import com.nexenio.bleindoorpositioning.location.provider.LocationProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by steppschuh on 15.11.17.
 */

public abstract class Beacon<P extends AdvertisingPacket> {

    //public static final long MAXIMUM_PACKET_AGE = TimeUnit.SECONDS.toMillis(60);
    public static final long MAXIMUM_PACKET_AGE = TimeUnit.SECONDS.toMillis(10);  //yskim
    public static final int MIN_PACKET_BUFFER_SIZE=6; //yskim
    public static final int MIN_PACKET_READ_BUFFER_SIZE=4; //yskim
    public static final int MAX_RSSI=-80; //yskim
    public static final int MIN_RSSI=-45; //yskim

    protected String macAddress;
    protected int rssi; // in dBm
    protected int calibratedRssi; // in dBm
    protected int calibratedDistance; // in m
    protected int transmissionPower; // in dBm
    protected float distance; // in m
    protected boolean shouldUpdateDistance = true;
    protected final ArrayList<P> advertisingPackets = new ArrayList<>();
    protected final ArrayList<P> yskimadvertisingPackets = new ArrayList<>();

    protected BeaconLocationProvider<? extends Beacon> locationProvider;

    //yskim
    protected float revdistance;//yskim
    protected int revrssi;//yskim
    protected float revmax; //yskim
    protected float revmin; //yskim
    protected int remove_persent=10;//(%)
    protected double remove_actualdistance;


    public Beacon() {
        this.locationProvider = createLocationProvider();
    }

    /**
     * @deprecated use a {@link BeaconFactory} instead (e.g. in {@link BeaconManager#beaconFactory}).
     */
    @Deprecated
    public static Beacon from(AdvertisingPacket advertisingPacket) {
        Beacon beacon = null;
        if (advertisingPacket instanceof IBeaconAdvertisingPacket) {
            beacon = new IBeacon();
        } else if (advertisingPacket instanceof EddystoneAdvertisingPacket) {
            beacon = new Eddystone();
        }


        return beacon;
    }

    public boolean hasLocation() {
        return locationProvider != null && locationProvider.hasLocation();
    }

    public Location getLocation() {
        return locationProvider == null ? null : locationProvider.getLocation();
    }

    public static List<Location> getLocations(List<? extends Beacon> beacons) {
        List<Location> locations = new ArrayList<>();
        for (Beacon beacon : beacons) {
            if (beacon.hasLocation()) {
                locations.add(beacon.getLocation());
            }
        }
        return locations;
    }

    public abstract BeaconLocationProvider<? extends Beacon> createLocationProvider();

    public boolean hasAnyAdvertisingPacket() {
        return !advertisingPackets.isEmpty();
    }

    public P getOldestAdvertisingPacket() {
        synchronized (advertisingPackets) {
            if (!hasAnyAdvertisingPacket()) {
                return null;
            }
            return advertisingPackets.get(0);
        }
    }

    public P getLatestAdvertisingPacket() {
        synchronized (advertisingPackets) {
            if (!hasAnyAdvertisingPacket()) {
                return null;
            }
            return advertisingPackets.get(advertisingPackets.size() - 1);
        }
    }

    /**
     * Returns an ArrayList of AdvertisingPackets that have been received in the specified time
     *      * range. If no packets match, an empty list will be returned.
     *
     * @param startTimestamp minimum timestamp, inclusive
     * @param endTimestamp   maximum timestamp, exclusive
     */
    public ArrayList<P> getAdvertisingPacketsBetween(long startTimestamp, long endTimestamp) {
        return AdvertisingPacketUtil.getAdvertisingPacketsBetween(new ArrayList<>(advertisingPackets), startTimestamp, endTimestamp);
    }

    public ArrayList<P> getAdvertisingPacketsFromLast(long amount, TimeUnit timeUnit) {
        return getAdvertisingPacketsBetween(System.currentTimeMillis() - timeUnit.toMillis(amount), System.currentTimeMillis());
    }

    public ArrayList<P> getAdvertisingPacketsSince(long timestamp) {
        return getAdvertisingPacketsBetween(timestamp, System.currentTimeMillis());
    }

    public ArrayList<P> getAdvertisingPacketsBefore(long timestamp) {
        return getAdvertisingPacketsBetween(0, timestamp);
    }

    public void addAdvertisingPacket(P advertisingPacket) {
        synchronized (advertisingPackets) {
            rssi = advertisingPacket.getRssi();

            P latestAdvertisingPacket = getLatestAdvertisingPacket();
            if (latestAdvertisingPacket == null || !advertisingPacket.dataEquals(latestAdvertisingPacket)) {
                applyPropertiesFromAdvertisingPacket(advertisingPacket);
            }

            if (latestAdvertisingPacket != null && latestAdvertisingPacket.getTimestamp() > advertisingPacket.getTimestamp()) {
                return;
            }


            if(advertisingPacket.getRssi()<=MIN_RSSI && advertisingPacket.getRssi()>=MAX_RSSI) {
                try{
                    advertisingPackets.add(advertisingPacket);

                    P yskimpacket=advertisingPacket;

                    System.out.print("kalman before : " + advertisingPacket.getRssi() + "\n");

                    float filteredRssi = createSuggestedWindowFilter().filter(this);

                    yskimpacket.setRssi((int) filteredRssi);

                    System.out.print("kalman before 2: " + advertisingPacket.getRssi() + "\n");

                    yskimadvertisingPackets.add(yskimpacket);

                    System.out.print("kalman before >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n");
                }catch (Exception e){}
            }

            if(advertisingPackets.size()>(MIN_PACKET_BUFFER_SIZE+1))
            {
                //remove max
                advertisingPackets.remove(0);
                yskimadvertisingPackets.remove(0);
            }
            trimAdvertisingPackets();
            invalidateDistance();
            //calculateRssiAverage(); //yskim  신호가 안들어오면 스톱되어 버리네
        }
    }


    public int getRevrssi()
    {
        return revrssi;
    }

    public float getRevdistance()
    {
        return revdistance;
    }

    public float getRevmin()
    {
        return revmin;
    }


    public float getRevmax()
    {
        return revmax;
    }

    public double getActualdistance()
    {
        return remove_actualdistance;
    }




    public void calculateRssiAverage()
    {
       // ArrayList<IBeaconAdvertisingPacket> advertisingPackets = ((IBeacon) this).getAdvertisingPackets();


        if(yskimadvertisingPackets.size()>=MIN_PACKET_READ_BUFFER_SIZE) {

            int remove_cnt=(int) (((float)remove_persent/100)*yskimadvertisingPackets.size());
            float tvalue=0,max=0, min=0;
            //int removecount=3;
            int size=yskimadvertisingPackets.size();
            float arr[]=new float[size];


            for (int i = 0; i < yskimadvertisingPackets.size(); i++) {
                arr[i]=yskimadvertisingPackets.get(i).getRssi();
            }

            Arrays.sort(arr);

            double caltvalue=0;
            //protected int remove_top=3;
            //protected int remove_low=5;
            //for(int i=(remove_top);i<(arr.length-remove_low);i++)
            for(int i=(remove_cnt);i<(arr.length-remove_cnt);i++)
            {
                caltvalue+=arr[i];
            }
            //System.out.print("yskim processScanResult ok Minor 1 average : " + tvalue/advertisingPackets.size() + "\n");
            //System.out.print("yskim processScanResult ok Minor 1 max  : " + arr[0]+ "    min  : " + arr[size-1]+ "\n");
            revrssi = (int) (caltvalue/(arr.length-(remove_cnt*2)));
            //System.out.print("yskim processScanResult ok Minor 1 rt average : " + revrssi + "\n");
            //revdistance = (float) Math.pow(10, (-59 - revrssi ) / (10 * 1.4f));
            revdistance = revcalculateDistance(-54,revrssi);
            revmax = arr[0];
            revmin = arr[(yskimadvertisingPackets.size()-1)];

            //float filteredRssi = createSuggestedWindowFilter().filter(this);

            //remove_actualdistance=BeaconDistanceCalculator.calculateDistanceTo(this, filteredRssi);
        }
        else
        {
            //비콘신호가 없어졌다...  9개가 되면 그럼
            revrssi = 0;
            revdistance = 0.0f;
            revmax = 0;
            revmin = 0;
        }
    }


    public void yskimKanmal()
    {
        float filteredRssi = createSuggestedWindowFilter().filter(this);
    }

    float yskim_plus=-0.3f;
    float yskim_minus=0.0f;
    public  float revcalculateDistance(int txPower, double rssi) {
        //return (float) Math.pow(10, (txPower - rssi ) / (10 * 1.4f));

        if (rssi == 0) {
            return -1.0f; // if we cannot determine distance, return -1.
        }
        double ratio = rssi*1.0/txPower;
        if (ratio < 1.0) {
            return (float)Math.abs(Math.pow(ratio,10)+yskim_plus);
        }
        else {
            //-59 이하로 -40 이면 -
            //float accuracy = (float) Math.abs(((0.89976)*Math.pow(ratio,7.7095) + 0.111)+yskim_minus);
            float accuracy = (float) Math.abs(((1.1)*Math.pow(ratio,7.7095) + 0.111));
            return accuracy;
        }

    }


    public void applyPropertiesFromAdvertisingPacket(P advertisingPacket) {
        //setTransmissionPower(lastAdvertisingPacket.get);

    }

    public void trimAdvertisingPackets() {
        synchronized (advertisingPackets) {
            if (!hasAnyAdvertisingPacket()) {
                return;
            }

            List<P> removableAdvertisingPackets = new ArrayList<>();
            List<P> removyskimableAdvertisingPackets = new ArrayList<>();
            AdvertisingPacket latestAdvertisingPacket = getLatestAdvertisingPacket();
            long minimumPacketTimestamp = System.currentTimeMillis() - MAXIMUM_PACKET_AGE;
            for (P advertisingPacket : advertisingPackets) {
                if (advertisingPacket == latestAdvertisingPacket) {
                    // don't remove the latest packet

                    continue;
                }

                if (advertisingPacket.getTimestamp() < minimumPacketTimestamp) {
                    // mark old packets as removable
                    removableAdvertisingPackets.add(advertisingPacket);
                }
            }

            //yskim yskimadvertisingPackets
            for (P advertisingPacket : yskimadvertisingPackets) {
                if (advertisingPacket.getTimestamp() < minimumPacketTimestamp) {
                    // mark old packets as removable
                    removyskimableAdvertisingPackets.add(advertisingPacket);
                }
            }

            advertisingPackets.removeAll(removableAdvertisingPackets);
            yskimadvertisingPackets.removeAll(removyskimableAdvertisingPackets);
        }
    }

    public boolean equalsLastAdvertisingPackage(P advertisingPacket) {
        return hasAnyAdvertisingPacket() && getLatestAdvertisingPacket().equals(advertisingPacket);
    }

    public boolean hasBeenSeenSince(long timestamp) {
        if (!hasAnyAdvertisingPacket()) {
            return false;
        }
        return getLatestAdvertisingPacket().getTimestamp() > timestamp;
    }

    public boolean hasBeenSeenInThePast(long duration, TimeUnit timeUnit) {
        if (!hasAnyAdvertisingPacket()) {
            return false;
        }
        return getLatestAdvertisingPacket().getTimestamp() > System.currentTimeMillis() - timeUnit.toMillis(duration);
    }

    public float getRssi(RssiFilter filter) {
        return filter.filter(this);
    }

    public float getFilteredRssi() {
        return getRssi(createSuggestedWindowFilter());
    }

    protected void invalidateDistance() {
        shouldUpdateDistance = true;
    }

    public float getDistance() {
        if (shouldUpdateDistance) {
            distance = getDistance(createSuggestedWindowFilter());
            //yskim
            shouldUpdateDistance = false;
        }
        return distance;
    }



    public float getDistance(RssiFilter filter) {
        float filteredRssi = getRssi(filter);
        System.out.print("yskim befor dist  Rssi : "+rssi+"   filteredRssi "+filteredRssi+"\n");
        // TODO get real device elevation with 3D multilateration

        //return BeaconDistanceCalculator.calculateDistanceTo(this, filteredRssi);
        return BeaconDistanceCalculator.calculateDistanceTo(this, rssi);  //yskim no kalmalfilter
    }

    public float getEstimatedAdvertisingRange() {
        return BeaconUtil.getAdvertisingRange(transmissionPower);
    }

    public long getLatestTimestamp() {
        if (!hasAnyAdvertisingPacket()) {
            return 0;
        }
        return getLatestAdvertisingPacket().getTimestamp();
    }

    public WindowFilter createSuggestedWindowFilter() {
        WindowFilter wf=new KalmanFilter(getLatestTimestamp());  //yskim
        //System.out.print(wf);
        return wf;
    }

    /**
     * This function and its reverse are implemented with indicative naming in BeaconUtil.
     *
     * @deprecated use {@link BeaconUtil#AscendingRssiComparator} instead
     */
    @Deprecated
    public static Comparator<Beacon> RssiComparator = new Comparator<Beacon>() {
        public int compare(Beacon firstBeacon, Beacon secondBeacon) {
            if (firstBeacon.equals(secondBeacon)) {
                return 0;
            }
            return Integer.compare(firstBeacon.rssi, secondBeacon.rssi);
        }
    };

    @Override
    public String toString() {
        System.out.println("yskim Beacon Beacon{" +
                ", macAddress='" + macAddress + '\'' +
                ", rssi=" + rssi +
                ", calibratedRssi=" + calibratedRssi +
                ", calibratedDistance=" + calibratedDistance +
                ", transmissionPower=" + transmissionPower +
                ", advertisingPackets=" + advertisingPackets +
                '}');

        return "Beacon{" +
                ", macAddress='" + macAddress + '\'' +
                ", rssi=" + rssi +
                ", calibratedRssi=" + calibratedRssi +
                ", calibratedDistance=" + calibratedDistance +
                ", transmissionPower=" + transmissionPower +
                ", advertisingPackets=" + advertisingPackets +
                '}';
    }

    /*
        Getter & Setter
     */

    public String getMacAddress() {
        return macAddress;
    }


    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
        invalidateDistance();
    }

    public int getCalibratedRssi() {
        return calibratedRssi;
    }

    public void setCalibratedRssi(int calibratedRssi) {
        this.calibratedRssi = calibratedRssi;
        invalidateDistance();
    }

    public int getCalibratedDistance() {
        return calibratedDistance;
    }

    public void setCalibratedDistance(int calibratedDistance) {
        this.calibratedDistance = calibratedDistance;
        invalidateDistance();
    }

    public int getTransmissionPower() {
        return transmissionPower;
    }

    public void setTransmissionPower(int transmissionPower) {
        this.transmissionPower = transmissionPower;
    }


    public ArrayList<P> getYskimadvertisingPackets() {
        return yskimadvertisingPackets;
    }


    public ArrayList<P> getAdvertisingPackets() {
        return advertisingPackets;
    }

    public LocationProvider getLocationProvider() {
        return locationProvider;
    }

    public void setLocationProvider(BeaconLocationProvider<? extends Beacon> locationProvider) {
        this.locationProvider = locationProvider;
    }

}
