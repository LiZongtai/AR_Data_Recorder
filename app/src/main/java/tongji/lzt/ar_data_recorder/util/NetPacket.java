package tongji.lzt.ar_data_recorder.util;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;

public abstract class NetPacket {
    public static Boolean shouldLog = false;
    public InetAddress remoteEP = null;
    public double time = TimeExt.CurrentTimeInSeconds();

    public abstract int GetCommand();

    public abstract void Write(ByteArrayOutputStream packWriter);

    public static byte[] ToByteArray(NetPacket pack){
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        pack.Write(byteArrayOutputStream); //写入主要数据
        byte[] contentPack = byteArrayOutputStream.toByteArray();
        int packLength = contentPack.length;
        Log.i("NetFramework","packet Length:"+packLength);
        ByteArrayOutputStream netDataStream = new ByteArrayOutputStream();
        LittleEndianOutputStream dataOutputStream = new LittleEndianOutputStream(netDataStream);
        int idx = 0;
        while (packLength>=50000){
            try{
                dataOutputStream.writeChar(50001);
                dataOutputStream.writeByte((byte)pack.GetCommand());
                dataOutputStream.write(contentPack,idx,50000);
                idx += 50000;
                packLength -= 50000;
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }
        try{
            dataOutputStream.writeChar(packLength+1);
            dataOutputStream.writeByte((byte)pack.GetCommand());
            dataOutputStream.write(contentPack,idx,(int)packLength);
        }
        catch (IOException e){
            e.printStackTrace();
        }
        return  netDataStream.toByteArray();
    }

    public String toString(){
        return String.format("Packet [{0}]",GetCommand());
    }

}
