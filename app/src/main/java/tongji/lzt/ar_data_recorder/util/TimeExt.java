package tongji.lzt.ar_data_recorder.util;

public class TimeExt {
    public static double CurrentTimeInSeconds(){
        return (double)(System.currentTimeMillis()/1000.0);
    }
}
