package tongji.lzt.ar_data_recorder.packet;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import tongji.lzt.ar_data_recorder.util.GpsNaviStep;
import tongji.lzt.ar_data_recorder.util.GpsPoint;
import tongji.lzt.ar_data_recorder.util.LittleEndianOutputStream;
import tongji.lzt.ar_data_recorder.util.NetPacket;

public class GpsNaviRoutePacket extends NetPacket {
    GpsPoint _ref; //获取当前路线方案的起点坐标。
    GpsPoint _center;
    GpsPoint _endPoint;
    int _coordsCount; //当前导航路线上坐标点的总数。
    GpsPoint[] _gpsNaviData;
    int allLength;
    int allTime;
    int _stepsCount; //当前导航路线上分段的总数。
    GpsNaviStep[] _gpsNaviSteps;
    int _type; //Normal and Parking

//    GpsPoint[] _lightList;
//    int _trafficLightCount; //获取红绿灯总数
//
//    String _mainRoadInfo;
//    NaviRouteGuideGroup [] _naviRouteGuideGroups;
//    NaviStep[] _naviSteps;



    public void setAllLengthTime(int allLen, int allT){
        allLength = allLen;
        allTime = allT;
    }

    public void setCenter(GpsPoint center){
        _center = center;
    }

    public void setEnd(GpsPoint end){
        _endPoint = end;
    }

    public void setNaviSteps(GpsNaviStep[] GpsNaviSteps) {
        _gpsNaviSteps = new GpsNaviStep[GpsNaviSteps.length];
        for (int i=0;i<GpsNaviSteps.length; i++)
        {
            _gpsNaviSteps[i] = GpsNaviSteps[i];
        }
        _stepsCount = GpsNaviSteps.length;
    }

    public void setNormalOrParkingType(int type){
        _type = type;
    }

    public GpsNaviRoutePacket(GpsPoint Ref, GpsPoint[] GpsNaviData){
        _gpsNaviData = new GpsPoint[GpsNaviData.length];
        _ref = Ref;
        _type = 0;
        for (int i=0;i<GpsNaviData.length;i++){
            _gpsNaviData[i] = GpsNaviData[i];
        }
        _coordsCount = GpsNaviData.length;
    }

    public GpsNaviRoutePacket(GpsPoint Ref, GpsPoint[] GpsNaviData, int type){
        _gpsNaviData = new GpsPoint[GpsNaviData.length];
        _ref = Ref;
        _type = type;
        for (int i=0;i<GpsNaviData.length;i++){
            _gpsNaviData[i] = GpsNaviData[i];
        }
        _coordsCount = GpsNaviData.length;
    }


    public int GetCommand(){
        return 1;
    }


    public void Write(ByteArrayOutputStream packWriter){
        LittleEndianOutputStream dataWriter = new LittleEndianOutputStream(packWriter);
        if (_gpsNaviData == null)
            return;
        try{
            dataWriter.writeDouble(_ref.latitude);
            dataWriter.writeDouble(_ref.longitude);
            dataWriter.writeDouble(_ref.distanceFromRef);
        }
        catch (IOException e){
            e.printStackTrace();
        }
        try{
            dataWriter.writeDouble(_center.latitude);
            dataWriter.writeDouble(_center.longitude);
            dataWriter.writeDouble(_center.distanceFromRef);
        }
        catch (IOException e){
            e.printStackTrace();
        }
        try{
            dataWriter.writeDouble(_endPoint.latitude);
            dataWriter.writeDouble(_endPoint.longitude);
            dataWriter.writeDouble(_endPoint.distanceFromRef);
        }
        catch (IOException e){
            e.printStackTrace();
        }

        try{
            dataWriter.writeInt(_coordsCount);
        }
        catch (IOException e){
            e.printStackTrace();
        }

        for (int i = 0; i < _gpsNaviData.length; ++i) {
            try{
                dataWriter.writeDouble(_gpsNaviData[i].latitude);
                dataWriter.writeDouble(_gpsNaviData[i].longitude);
                dataWriter.writeDouble(_gpsNaviData[i].distanceFromRef);
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }
        try{
            dataWriter.writeInt(_type);
        }
        catch (IOException e){
            e.printStackTrace();
        }
        try{
            dataWriter.writeInt(allLength);
        }
        catch (IOException e){
            e.printStackTrace();
        }
        try{
            dataWriter.writeInt(allTime);
        }
        catch (IOException e){
            e.printStackTrace();
        }
        try{
            dataWriter.writeInt(_stepsCount);
        }
        catch (IOException e){
            e.printStackTrace();
        }
        for(int i =0 ;i<_stepsCount;i++){
            try{
                dataWriter.writeInt(_gpsNaviSteps[i].iconType);
                dataWriter.writeInt(_gpsNaviSteps[i].stepLength);
                dataWriter.writeInt(_gpsNaviSteps[i].stepTime);
                dataWriter.writeInt(_gpsNaviSteps[i].stepStartIdx);
                dataWriter.writeInt(_gpsNaviSteps[i].stepEndIdx);
                dataWriter.writeInt(_gpsNaviSteps[i].stepTrafficLightCount);
                dataWriter.writeBoolean(_gpsNaviSteps[i].isArriveWayPoint);
                dataWriter.writeInt(_gpsNaviSteps[i].stepCoords);
                dataWriter.writeInt(_gpsNaviSteps[i].stepLinksCount);
                for (int j =0;j<_gpsNaviSteps[i].stepLinksCount;j++) {
                    dataWriter.writeInt(_gpsNaviSteps[i].stepLinks[j].roadNameLength);
                    dataWriter.write(_gpsNaviSteps[i].stepLinks[j].roadName);
                    dataWriter.writeInt(_gpsNaviSteps[i].stepLinks[j].length);
                    dataWriter.writeInt(_gpsNaviSteps[i].stepLinks[j].linkType);
                    dataWriter.writeInt(_gpsNaviSteps[i].stepLinks[j].roadClass);
                    dataWriter.writeInt(_gpsNaviSteps[i].stepLinks[j].roadType);
                    dataWriter.writeInt(_gpsNaviSteps[i].stepLinks[j].trafficStatus);
                    dataWriter.writeInt(_gpsNaviSteps[i].stepLinks[j].linkCoords);
                }
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }
    }
}
