package tongji.lzt.ar_data_recorder.packet;

import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.model.LatLng;
import com.amap.api.services.routepoisearch.RoutePOIItem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import tongji.lzt.ar_data_recorder.util.GpsPoint;
import tongji.lzt.ar_data_recorder.util.LittleEndianOutputStream;
import tongji.lzt.ar_data_recorder.util.NetPacket;

public class GpsRouteFillingStationPacket extends NetPacket {
    ArrayList<RoutePOIItem> fsPOIItems;
    public GpsRouteFillingStationPacket(ArrayList<RoutePOIItem> _fsPOIItems){
        fsPOIItems = _fsPOIItems;
    }
    public int GetCommand(){
        return 53;
    }

    public void Write(ByteArrayOutputStream packWriter){
        LittleEndianOutputStream dataWriter = new LittleEndianOutputStream(packWriter);
        try {
            dataWriter.writeInt(fsPOIItems.size());
        }
        catch (IOException e){
            e.printStackTrace();
        }
        for (int i =0;i<fsPOIItems.size();i++){
            try{
                LatLng latLng = new LatLng(fsPOIItems.get(i).getPoint().getLatitude(),fsPOIItems.get(i).getPoint().getLongitude());
                LatLng refLatLng = new LatLng(GpsPoint.refGpsPoint.latitude,GpsPoint.refGpsPoint.longitude);

                GpsPoint saLocation = new GpsPoint(fsPOIItems.get(i).getPoint().getLatitude(),fsPOIItems.get(i).getPoint().getLongitude(),AMapUtils.calculateLineDistance(latLng,refLatLng));

                byte[] saName = new byte[0];

                try {
                    saName = fsPOIItems.get(i).getTitle().getBytes("UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                dataWriter.writeDouble(saLocation.latitude);
                dataWriter.writeDouble(saLocation.longitude);
                dataWriter.writeDouble(saLocation.distanceFromRef);
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
