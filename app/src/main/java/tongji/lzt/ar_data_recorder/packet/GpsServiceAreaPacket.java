package tongji.lzt.ar_data_recorder.packet;

import com.amap.api.navi.model.AMapServiceAreaInfo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import tongji.lzt.ar_data_recorder.util.GpsPoint;
import tongji.lzt.ar_data_recorder.util.LittleEndianOutputStream;
import tongji.lzt.ar_data_recorder.util.NetPacket;

public class GpsServiceAreaPacket extends NetPacket {
    ArrayList<AMapServiceAreaInfo> serviceAreaInfos;

    public GpsServiceAreaPacket(ArrayList<AMapServiceAreaInfo> _serviceAreaInfos)
    {
        serviceAreaInfos = _serviceAreaInfos;
    }

    public int GetCommand(){
        return 153;
    }

    public void Write(ByteArrayOutputStream packWriter){
        LittleEndianOutputStream dataWriter = new LittleEndianOutputStream(packWriter);
        try {
            dataWriter.writeInt(serviceAreaInfos.size());
        }
        catch (IOException e){
            e.printStackTrace();
        }
        for (int i =0;i<serviceAreaInfos.size();i++){
            try{
                GpsPoint saLocation = new GpsPoint(serviceAreaInfos.get(i).getCoordinate().getLatitude(),serviceAreaInfos.get(i).getCoordinate().getLongitude(),0.0);
                int saDistance = serviceAreaInfos.get(i).getRemainDist();

                byte[] saName = new byte[0];

                try {
                    saName = serviceAreaInfos.get(i).getName().getBytes("UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                dataWriter.writeDouble(saLocation.latitude);
                dataWriter.writeDouble(saLocation.longitude);
                dataWriter.writeDouble(saLocation.distanceFromRef);
                dataWriter.writeInt(saDistance);
                dataWriter.writeInt(saName.length);
                if(saName.length > 0)
                    dataWriter.write(saName);
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }

    }
}
