package tongji.lzt.ar_data_recorder.util;

public class GpsNaviStep {
    public int iconType;
    public int stepLength;
    public int stepTime;
    public int stepStartIdx;
    public int stepEndIdx;
    public int stepTrafficLightCount;
    public Boolean isArriveWayPoint;
    public int stepCoords;
    public int stepLinksCount;
    public GpsNaviLink [] stepLinks;

    public GpsNaviStep(int IconType, int StepLength, int StepTime, int StepStartIdx,
                       int StepEndIdx, int StepTrafficLightCount, Boolean IsArriveWayPoint,
                       int StepCoords, GpsNaviLink [] StepLinks)
    {
        iconType = IconType;
        stepLength = StepLength;
        stepTime = StepTime;
        stepStartIdx = StepStartIdx;
        stepEndIdx = StepEndIdx;
        stepTrafficLightCount = StepTrafficLightCount;
        isArriveWayPoint = IsArriveWayPoint;
        stepCoords = StepCoords;
        stepLinksCount = StepLinks.length;
        stepLinks = new GpsNaviLink[stepLinksCount];
        for(int i=0;i<StepLinks.length;i++){
            stepLinks[i] = StepLinks[i];
        }
    }
}
