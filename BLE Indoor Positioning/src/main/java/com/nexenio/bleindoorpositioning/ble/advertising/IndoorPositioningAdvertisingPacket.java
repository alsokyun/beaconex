package com.nexenio.bleindoorpositioning.ble.advertising;

import java.util.UUID;

/**
 * For advertising packets as specified in Apples <a href="http://www.blueupbeacons.com/docs/dev/Proximity%20Beacon%20Specification%20R1.pdf">Proximity
 * Beacon Specification</a>.
 */

public class IndoorPositioningAdvertisingPacket extends IBeaconAdvertisingPacket {

    //public final static UUID INDOOR_POSITIONING_UUID = UUID.fromString("03253fdd-55cb-44c2-a1eb-80c8355f8291");  //yskim
    //pnt 44790ba4-7eb3-4095-9e14-4b43ae67512b
    //hycon CCE99BED-E080-04C4-1A91-1A1A29B64111
    //cce99bed-e080-04c4-1a91-1a1a29b64111
    public final static UUID INDOOR_POSITIONING_UUID = UUID.fromString("cce99bed-e080-04c4-1a91-1a1a29b64111");  //yskim


    public IndoorPositioningAdvertisingPacket(byte[] data) {
        super(data);
    }

    public static boolean meetsSpecification(byte[] data) {
        return dataMatchesUuid(data, INDOOR_POSITIONING_UUID) && IBeaconAdvertisingPacket.meetsSpecification(data);
    }

}
