package tongji.lzt.ar_data_recorder.util;

public class GpsPoint {
    public double latitude;
    public double longitude;
    public double distanceFromRef;
    public GpsPoint(double _latitude,double _longitude, double _distanceFromRef){
        latitude = _latitude;
        longitude = _longitude;
        distanceFromRef = _distanceFromRef;
    }
    public static GpsPoint refGpsPoint;
}
