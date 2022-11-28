package tongji.lzt.ar_data_recorder.util;

import java.io.UnsupportedEncodingException;

public class GpsNaviLink {
    public int roadNameLength;
    public byte[] roadName;
    public int length;
    public int linkType;
    public int roadClass;
    public int roadType;
    public int trafficStatus;
    public int linkCoords;

    public GpsNaviLink(String RoadName, int Length, int LinkType, int RoadClass,
                       int RoadType, int TrafficStatus,int LinkCoords){
        byte[] roadNameBytes = new byte[0];
        try {
            roadNameBytes = RoadName.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        roadNameLength = roadNameBytes.length;
        roadName = roadNameBytes;
        length = Length;
        linkType = LinkType;
        roadClass = RoadClass;
        roadType = RoadType;
        trafficStatus = TrafficStatus;
        linkCoords = LinkCoords;
    }

}
