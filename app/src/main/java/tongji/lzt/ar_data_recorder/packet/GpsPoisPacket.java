package tongji.lzt.ar_data_recorder.packet;

import com.amap.api.services.core.PoiItem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import tongji.lzt.ar_data_recorder.util.LittleEndianOutputStream;
import tongji.lzt.ar_data_recorder.util.NetPacket;

public class GpsPoisPacket extends NetPacket {
    ArrayList<PoiItem> pois;

    public GpsPoisPacket(ArrayList<PoiItem> pois){
        this.pois =pois;
    }

    public int GetCommand(){
        return 9;
    }

    public void Write(ByteArrayOutputStream packWriter){
        LittleEndianOutputStream dataWriter = new LittleEndianOutputStream(packWriter);
        for (int i = 0; i < pois.size(); ++i) {
            try{
                PoiItem poi = pois.get(i);
                dataWriter.writeChars(poi.getPoiId());
                dataWriter.writeChar(',');
                dataWriter.writeChars(poi.getTitle().replaceAll("[,;]"," "));
                dataWriter.writeChar(',');
                dataWriter.writeChars(poi.getTypeDes().replace(';',' '));
                dataWriter.writeChar(',');
                dataWriter.writeChars(poi.getLatLonPoint().getLatitude()+",");
                dataWriter.writeChars(poi.getLatLonPoint().getLongitude()+";");
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }
    }
}