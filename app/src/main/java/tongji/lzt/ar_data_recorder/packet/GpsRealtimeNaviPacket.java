package tongji.lzt.ar_data_recorder.packet;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import tongji.lzt.ar_data_recorder.util.GpsPoint;
import tongji.lzt.ar_data_recorder.util.LittleEndianOutputStream;
import tongji.lzt.ar_data_recorder.util.NetPacket;


public class GpsRealtimeNaviPacket extends NetPacket {
    public GpsPoint carLoc;
    public int naviType;
    public long timestamp;
    public float carDirection;
    public float carSpeed;
    public float accuracy;
    public double altitude;

    public int curLink;
    public int curPoint;
    public int curStep;
    public boolean isMatchNaviPath;

    public int curStepRetainDistance;
    public int curStepRetainTime;
    public int pathRetainDistance;
    public int pathRetainTime;
    public int remainingLightCount;
    public int curRoadNameLength;
    public byte[] curRoadName;
    public int nextRoadNameLength;
    public byte[] nextRoadName;

    public boolean bShowCross = false;
    public boolean bShowLaneInfo = false;

    public int laneCount;
    public boolean[] laneRecommended;
    public int[] laneAction;
    public int[] userAction;

    public GpsRealtimeNaviPacket(GpsPoint _carLoc, int _naviType, long _timestamp,
                                 float _carDirection, float _carSpeed, float _accuracy, double _altitude)
    {
        carLoc = _carLoc;
        naviType = _naviType;
        timestamp = _timestamp;
        carDirection = _carDirection;
        carSpeed = _carSpeed;
        accuracy = _accuracy;
        altitude = _altitude;
        curLink = 0;
        curPoint = 0;
        curStep = 0;
        isMatchNaviPath = true;
        curStepRetainDistance = -1;
        curStepRetainTime = -1;
        pathRetainDistance = -1;
        pathRetainTime = -1;
        remainingLightCount = -1;
        curRoadNameLength = 0;
        nextRoadNameLength = 0;
        laneCount = 0;
    }

    public void setRouteInfo(int curLinkIndex, int curPointIndex, int curStepIndex, boolean _isMatchNaviPath)
    {
        curLink = curLinkIndex;
        curPoint = curPointIndex;
        curStep = curStepIndex;
        isMatchNaviPath =_isMatchNaviPath;
    }

    public void setNaviInfo(int _curStepRetainDistance,int _curStepRetainTime, int _pathRetainDistance, int _pathRetainTime,
                            int _remainingLightCount, String _curRoadName, String _nextRoadName ){
        curStepRetainDistance = _curStepRetainDistance;
        curStepRetainTime = _curStepRetainTime;
        pathRetainDistance =_pathRetainDistance;
        pathRetainTime = _pathRetainTime;
        remainingLightCount = _remainingLightCount;
        byte[] curRoadNameBytes = new byte[0];
        try {
            curRoadNameBytes = _curRoadName.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        curRoadNameLength = curRoadNameBytes.length;
        curRoadName = curRoadNameBytes;

        byte[] nextRoadNameBytes = new byte[0];
        try {
            nextRoadNameBytes = _nextRoadName.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        nextRoadNameLength = nextRoadNameBytes.length;
        nextRoadName = nextRoadNameBytes;
    }

    public void setLaneInfo(int _laneCount, boolean [] _laneRecommended,
                            int [] _laneActions, int[] _userActions){
        laneCount =_laneCount;
        laneRecommended = new boolean[laneCount];
        laneRecommended = _laneRecommended;
        laneAction = new int[laneCount];
        laneAction = _laneActions;
        userAction = new int[laneCount];
        userAction = _userActions;
    }
    public int GetCommand(){
        return 2;
    }

    public void Write(ByteArrayOutputStream packWriter){
        LittleEndianOutputStream dataWriter = new LittleEndianOutputStream(packWriter);
        try{
            dataWriter.writeDouble(carLoc.latitude);
            dataWriter.writeDouble(carLoc.longitude);
            dataWriter.writeDouble(carLoc.distanceFromRef);
            dataWriter.writeInt(naviType);
            dataWriter.writeLong(timestamp);
            dataWriter.writeFloat(carDirection);
            dataWriter.writeFloat(carSpeed);
            dataWriter.writeFloat(accuracy);
            dataWriter.writeDouble(altitude);

            dataWriter.writeInt(curStep);
            dataWriter.writeInt(curLink);
            dataWriter.writeInt(curPoint);
            dataWriter.writeBoolean(isMatchNaviPath);

            dataWriter.writeInt(curStepRetainDistance);
            dataWriter.writeInt(curStepRetainTime);
            dataWriter.writeInt(pathRetainDistance);
            dataWriter.writeInt(pathRetainTime);
            dataWriter.writeInt(remainingLightCount);
            dataWriter.writeInt(curRoadNameLength);
            if(curRoadNameLength > 0)
            dataWriter.write(curRoadName);
            dataWriter.writeInt(nextRoadNameLength);
            if(nextRoadNameLength > 0)
            dataWriter.write(nextRoadName);

            dataWriter.writeBoolean(bShowCross);
            dataWriter.writeBoolean(bShowLaneInfo);

            dataWriter.writeInt(laneCount);
            for(int i=0;i<laneCount;i++){
                dataWriter.writeBoolean(laneRecommended[i]);
            }
            for(int i=0;i<laneCount;i++){
                dataWriter.writeInt(laneAction[i]);
            }
            for(int i=0;i<laneCount;i++){
                dataWriter.writeInt(userAction[i]);
                Log.i("userAction","userAction:"+userAction[i]);
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }
}
