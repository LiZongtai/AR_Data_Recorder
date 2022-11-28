package tongji.lzt.ar_data_recorder;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.model.LatLng;
import com.amap.api.navi.AMapNavi;
import com.amap.api.navi.AMapNaviListener;
import com.amap.api.navi.AMapNaviView;
import com.amap.api.navi.AMapNaviViewListener;
import com.amap.api.navi.enums.NaviType;
import com.amap.api.navi.model.AMapCalcRouteResult;
import com.amap.api.navi.model.AMapLaneInfo;
import com.amap.api.navi.model.AMapModelCross;
import com.amap.api.navi.model.AMapNaviCameraInfo;
import com.amap.api.navi.model.AMapNaviCross;
import com.amap.api.navi.model.AMapNaviLink;
import com.amap.api.navi.model.AMapNaviLocation;
import com.amap.api.navi.model.AMapNaviPath;
import com.amap.api.navi.model.AMapNaviRouteGuideGroup;
import com.amap.api.navi.model.AMapNaviRouteGuideSegment;
import com.amap.api.navi.model.AMapNaviRouteNotifyData;
import com.amap.api.navi.model.AMapNaviStep;
import com.amap.api.navi.model.AMapNaviTrafficFacilityInfo;
import com.amap.api.navi.model.AMapServiceAreaInfo;
import com.amap.api.navi.model.AimLessModeCongestionInfo;
import com.amap.api.navi.model.AimLessModeStat;
import com.amap.api.navi.model.NaviInfo;
import com.amap.api.navi.model.NaviLatLng;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;
import com.amap.api.services.routepoisearch.RoutePOIItem;
import com.amap.api.services.routepoisearch.RoutePOISearch;
import com.amap.api.services.routepoisearch.RoutePOISearchQuery;
import com.amap.api.services.routepoisearch.RoutePOISearchResult;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import tongji.lzt.ar_data_recorder.packet.GpsNaviRoutePacket;
import tongji.lzt.ar_data_recorder.util.GpsNaviLink;
import tongji.lzt.ar_data_recorder.util.GpsNaviStep;
import tongji.lzt.ar_data_recorder.util.GpsPoint;
import tongji.lzt.ar_data_recorder.packet.GpsPoisPacket;
import tongji.lzt.ar_data_recorder.packet.GpsRealtimeNaviPacket;
import tongji.lzt.ar_data_recorder.packet.GpsRouteFillingStationPacket;
import tongji.lzt.ar_data_recorder.packet.GpsServiceAreaPacket;
import tongji.lzt.ar_data_recorder.util.NetPacket;

public class RouteNaviActivity extends Activity implements AMapNaviListener, AMapNaviViewListener, PoiSearch.OnPoiSearchListener, RoutePOISearch.OnRoutePOISearchListener {

    AMapNaviView mAMapNaviView;
    AMapNavi mAMapNavi;
    private ArrayList<RoutePOIItem> routePOIItemList = new ArrayList<RoutePOIItem>();

    GpsPoint GlobalRefPoint;
    GpsRealtimeNaviPacket GlobalRealtimePacket;
    GpsNaviRoutePacket GlobalNaviRoutePacket;
    GpsPoisPacket GlobalGpsPoisPacket;
    GpsServiceAreaPacket GlobalGpsSAPacket;
    GpsRouteFillingStationPacket GlobalGpsFSPacket;

    String filesDirPath;
    // 动态获得路径
    File file;
    // 输出流，把数据输出到文件中
    FileOutputStream fos;

    Boolean b_sendRoutePacket = false;
    Boolean b_sendRealtimePacket = false;
    Boolean b_sendPoisPacket = false;
    Boolean b_sendSAPacket = false;
    Boolean b_sendFSPacket = false;

    LatLonPoint des, enter; //目的地、停车场入口 用于后续寻路
    LatLng lastpoint;

    LatLng refLatLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_base_navi);

        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            filesDirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/AR_Data/";
            // 动态获得路径
            file = new File(filesDirPath,"ar_data_0.txt");
            try {
                fos = new FileOutputStream(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        // 输出流，把数据输出到文件中


        mAMapNaviView = (AMapNaviView) findViewById(R.id.navi_view);
        mAMapNaviView.onCreate(savedInstanceState);
        mAMapNaviView.setAMapNaviViewListener(this);

        try {
            mAMapNavi = AMapNavi.getInstance(getApplicationContext());

        } catch (com.amap.api.maps.AMapException e) {
            e.printStackTrace();
        }
        //创建一个AMapNavi导航对象。
        //参数:
        //context - 对应的Context
        //返回:
        //导航对象(单例)。
        Log.i("Version Info",AMapNavi.getVersion());

        mAMapNavi.addAMapNaviListener(this);
        //添加导航事件回调监听。
        //参数:
        //naviListener - 导航回调监听

        mAMapNavi.setUseInnerVoice(true, true);
        mAMapNavi.setEmulatorNaviSpeed(50);

        boolean gps = getIntent().getBooleanExtra("gps", false);

        new Thread(runnable).start();

        Log.i("MyNetwork","Connect Succeeds");
        if (gps) {
            mAMapNavi.startNavi(NaviType.GPS);
            //开始导航。实时导航会自动打开手机卫星定位功能。模拟导航则不需要使用定位功能。
            //参数:
            //naviType - 导航类型，1:实时导航 2:模拟导航。
            //返回:
            //启动成功或者失败。true是成功，false是失败。
        } else {
            mAMapNavi.startNavi(NaviType.EMULATOR);
        }
        AMapNaviPath myPath = mAMapNavi.getNaviPath();
        //获取当前规划的路线方案（路线结果），驾车、骑行与步行共用这一个对象
        //返回:
        //当前规划的路线信息。
        //MyPath Context
        Log.i("MyPath","AllLength:" + myPath.getAllLength());
        Log.i("MyPath","AllTime:" + myPath.getAllTime());



        GlobalRealtimePacket = new GpsRealtimeNaviPacket(
                new GpsPoint(0.0,0.0,0.0),
                0,(long)0,0,0,0,0.0);

//        CoordinateConverter converter = new CoordinateConverter(this);
//        converter.from(CoordinateConverter.CoordType.)
// sourceLatLng待转换坐标点 LatLng类型
//        converter.coord(sourceLatLng);
// 执行转换操作
//        LatLng desLatLng = converter.convert();

//        NaviLatLng	getStartPoint()

//        获取当前路线方案的起终点坐标。
        Log.i("MyPath","Count:"+myPath.getCoordList().size());
        NaviLatLng firstPoint = myPath.getStartPoint();
        Log.i("MyPath","StartPoint:"+firstPoint.toString());
        NaviLatLng endPoint = myPath.getEndPoint();
        des = new LatLonPoint(endPoint.getLatitude(), endPoint.getLongitude());
        enter = des.copy();

        Log.i("MyPath","EndPoint:"+endPoint.toString());
//        LatLng refLatLng = new LatLng(firstPoint.getLatitude(),firstPoint.getLongitude());
//        converter.from(CoordinateConverter.CoordType.GPS);
//        converter.coord(refLatLng);
//        LatLng refLatLngRes = converter.convert();
//        Log.i("MyPath","StartPointConvertion:"+refLatLngRes.toString());
        refLatLng = new LatLng(firstPoint.getLatitude(),firstPoint.getLongitude());

        GlobalRefPoint = new GpsPoint(firstPoint.getLatitude(),firstPoint.getLongitude(),0.0);
        GpsPoint.refGpsPoint = GlobalRefPoint;
        GpsPoint[] routePoints = new GpsPoint[myPath.getCoordList().size()];

        for (int i=0;i<myPath.getCoordList().size();i++){
            LatLng latLng = new LatLng(myPath.getCoordList().get(i).getLatitude(),myPath.getCoordList().get(i).getLongitude());
            Log.i("MyPath","Index:"+i+" Gps:"+myPath.getCoordList().get(i).toString()
                    +" disToRef:"+ AMapUtils.calculateLineDistance(latLng,refLatLng));
            GpsPoint _routePoint = new GpsPoint(myPath.getCoordList().get(i).getLatitude(),myPath.getCoordList().get(i).getLongitude(),
                    AMapUtils.calculateLineDistance(latLng,refLatLng));
            routePoints[i] = _routePoint;
        }
        GlobalNaviRoutePacket = new GpsNaviRoutePacket(GlobalRefPoint,routePoints,0);
        GlobalNaviRoutePacket.setAllLengthTime(myPath.getAllLength(),myPath.getAllTime());

        LatLng pathCenterLatLng = new LatLng(myPath.getCenterForPath().getLatitude(),myPath.getCenterForPath().getLongitude());
        GpsPoint naviPathCenter = new GpsPoint(pathCenterLatLng.latitude,pathCenterLatLng.longitude,
                AMapUtils.calculateLineDistance(pathCenterLatLng,refLatLng));
        GlobalNaviRoutePacket.setCenter(naviPathCenter);

        LatLng pathEndLatLng = new LatLng(myPath.getEndPoint().getLatitude(),myPath.getEndPoint().getLongitude());
        GpsPoint naviPathEnd = new GpsPoint(pathEndLatLng.latitude,pathEndLatLng.longitude,
                AMapUtils.calculateLineDistance(pathCenterLatLng,refLatLng));
        GlobalNaviRoutePacket.setEnd(naviPathEnd);

        GpsNaviStep[] naviRouteSteps = new GpsNaviStep[myPath.getStepsCount()];
        for (int i =0;i<myPath.getStepsCount();i++){
            GpsNaviLink[] StepLinks = new GpsNaviLink[myPath.getSteps().get(i).getLinks().size()];
            for (int j =0;j<StepLinks.length;j++){

                StepLinks[j] = new GpsNaviLink(myPath.getSteps().get(i).getLinks().get(j).getRoadName(),
                        myPath.getSteps().get(i).getLinks().get(j).getLength(),
                        myPath.getSteps().get(i).getLinks().get(j).getLinkType(),
                        myPath.getSteps().get(i).getLinks().get(j).getRoadClass(),
                        myPath.getSteps().get(i).getLinks().get(j).getRoadType(),
                        myPath.getSteps().get(i).getLinks().get(j).getTrafficStatus(),
                        myPath.getSteps().get(i).getLinks().get(j).getCoords().size());
            }
            naviRouteSteps[i] = new GpsNaviStep(myPath.getSteps().get(i).getIconType(),
                    myPath.getSteps().get(i).getLength(),
                    myPath.getSteps().get(i).getTime(),
                    myPath.getSteps().get(i).getStartIndex(),
                    myPath.getSteps().get(i).getEndIndex(),
                    myPath.getSteps().get(i).getTrafficLightCount(),
                    myPath.getSteps().get(i).isArriveWayPoint(),
                    myPath.getSteps().get(i).getCoords().size(), StepLinks);
        }

        GlobalNaviRoutePacket.setNaviSteps(naviRouteSteps);

        List<AMapNaviStep> naviSteps = myPath.getSteps();
        for (int i =0;i<naviSteps.size();i++){
            AMapNaviStep naviStep = naviSteps.get(i);
            List<NaviLatLng> coordsList = naviStep.getCoords();
            Log.i("MyPath","第"+i+"个step的坐标点个数："+coordsList.size());
            for (int j =0;j<coordsList.size();j++){
                Log.i("MyPath","第"+j+"个坐标点in 第"+i+"个step："+coordsList.get(j).toString());
            }
            Log.i("MyPath","第"+i+"个step的iconType："+naviStep.getIconType());
            Log.i("MyPath","第"+i+"个step的length："+naviStep.getLength());
            Log.i("MyPath","第"+i+"个step的time："+naviStep.getTime());
            Log.i("MyPath","第"+i+"个step的起点index："+naviStep.getStartIndex());
            Log.i("MyPath","第"+i+"个step的终点index："+naviStep.getEndIndex());
            Log.i("MyPath","第"+i+"个step的红绿灯总数："+naviStep.getTrafficLightCount());
            Log.i("MyPath","第"+i+"个step是否经过途经点："+naviStep.isArriveWayPoint());
            List<AMapNaviLink> naviLinksList = naviStep.getLinks();
            for (int j =0;j<naviLinksList.size();j++) {
                Log.i("MyPath", "第" + j + "个link in 第" + i + "个step的length：" + naviLinksList.get(j).getLength());
                Log.i("MyPath", "第" + j + "个link in 第" + i + "个step的道路type：" + naviLinksList.get(j).getLinkType());
                Log.i("MyPath", "第" + j + "个link in 第" + i + "个step的Link道路等级：" + naviLinksList.get(j).getRoadClass());
                Log.i("MyPath", "第" + j + "个link in 第" + i + "个step的Link道路名称：" + naviLinksList.get(j).getRoadName());
                Log.i("MyPath", "第" + j + "个link in 第" + i + "个step的Link道路类型：" + naviLinksList.get(j).getRoadType());
                Log.i("MyPath", "第" + j + "个link in 第" + i + "个step的交通状态：" + naviLinksList.get(j).getTrafficStatus());
                List<NaviLatLng> linkCoordsList = naviLinksList.get(j).getCoords();
                Log.i("MyPath", "第" + i + "个step的第" + j + "个link的坐标点个数：" + linkCoordsList.size());
                for (int k = 0; k < linkCoordsList.size(); k++) {
                    Log.i("MyPath", "第" + i + "个step的第" + j + "个link的第" + k + "个坐标点：" + linkCoordsList.get(k).toString());
                }
            }
        }

        List<AMapNaviRouteGuideGroup> routeGuideGroups = myPath.getNaviGuideList();
        for (int i =0;i<routeGuideGroups.size();i++){
            Log.i("MyPath","第"+i+"个路线详情分组的起点坐标："+routeGuideGroups.get(i).getGroupEnterCoord().toString());
            Log.i("MyPath","第"+i+"个路线详情分组的转向："+routeGuideGroups.get(i).getGroupIconType());
            Log.i("MyPath","第"+i+"个路线详情分组的长度 (米)："+routeGuideGroups.get(i).getGroupLen());
            Log.i("MyPath","第"+i+"个路线详情分组名称："+routeGuideGroups.get(i).getGroupName());
            Log.i("MyPath","第"+i+"个路线详情分组的预计行驶时间（单位秒)："+routeGuideGroups.get(i).getGroupTime());
            Log.i("MyPath","第"+i+"个路线详情分组的红绿灯数量："+routeGuideGroups.get(i).getTrafficLightsCount());
            List<AMapNaviRouteGuideSegment> routeGuideSegments = routeGuideGroups.get(i).getSegments();
            for (int j =0;j<routeGuideSegments.size();j++){
                Log.i("MyPath","第"+i+"个路线详情分组第"+j+"个segment的详细描述："+routeGuideSegments.get(j).getDescription());
                Log.i("MyPath","第"+i+"个路线详情分组第"+j+"个segment的分段的转向："+routeGuideSegments.get(j).getStepIconType());
                Log.i("MyPath","第"+i+"个路线详情分组第"+j+"个segment是否经过途经点："+routeGuideSegments.get(j).isArriveWayPoint());
            }
        }

        b_sendRoutePacket = true;

        /// 搜索终点附近停车场
        PoiSearch.Query query = new PoiSearch.Query("", "150900", "");
        query.setDistanceSort(true);
        PoiSearch poiSearch = null;
        try {
            poiSearch = new PoiSearch(this, query);
        } catch (AMapException e) {
            e.printStackTrace();
        }
        poiSearch.setOnPoiSearchListener(this);
        poiSearch.setBound(new PoiSearch.SearchBound(new LatLonPoint(endPoint.getLatitude(),
                endPoint.getLongitude()), 2000));//设置周边搜索的中心点以及半径
        poiSearch.searchPOIAsyn();

        lastpoint = new LatLng(firstPoint.getLatitude(),firstPoint.getLongitude());
        PoiSearch.Query noQuery = new PoiSearch.Query("", "110100|110200|060100");
        noQuery.setDistanceSort(true);
        PoiSearch noPoiSearch = null;
        try {
            noPoiSearch = new PoiSearch(this, noQuery);
        } catch (AMapException e) {
            e.printStackTrace();
        }
        noPoiSearch.setOnPoiSearchListener(this);
        noPoiSearch.setBound(new PoiSearch.SearchBound(new LatLonPoint(lastpoint.latitude,
                lastpoint.longitude), 4000));//设置周边搜索的中心点以及半径
        noPoiSearch.searchPOIAsyn();

        try {
            searchRoutePOI(RoutePOISearch.RoutePOISearchType.TypeGasStation,
                    mAMapNavi.getNaviPath());
        } catch (AMapException e) {
            e.printStackTrace();
        }

    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
//            String serverName = getIntent().getStringExtra("server");
//            int serverPort = getIntent().getIntExtra("port",8889);

            while (true){
                if(b_sendRoutePacket){
                    byte[] routePacketByteArray = NetPacket.ToByteArray(GlobalNaviRoutePacket);
                    Log.i("MyNetwork","RoutePacketSize:"+routePacketByteArray.length);
                    // TODO
                    // 写入字节流
                    try {
                        fos.write(routePacketByteArray);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    b_sendRoutePacket = false;
                }
                if (b_sendRealtimePacket){
                    byte[] realtimePacketByteArray = NetPacket.ToByteArray(GlobalRealtimePacket);
                    // TODO
                    try {
                        fos.write(realtimePacketByteArray);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    b_sendRealtimePacket = false;
                }
                if(b_sendPoisPacket){
                    byte[] poisPacketByteArray = NetPacket.ToByteArray(GlobalGpsPoisPacket);
                    // TODO
                    try {
                        fos.write(poisPacketByteArray);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    b_sendPoisPacket = false;
                }
                if (b_sendSAPacket){
                    byte[] serviceAreaPacketByteArray = NetPacket.ToByteArray(GlobalGpsSAPacket);
                    // TODO
                    try {
                        fos.write(serviceAreaPacketByteArray);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    b_sendSAPacket = false;
                }
                if (b_sendFSPacket){
                    byte[] fillingStationPacketByteArray = NetPacket.ToByteArray(GlobalGpsFSPacket);
                    // TODO
                    try {
                        fos.write(fillingStationPacketByteArray);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    b_sendFSPacket = false;
                }
            }
        }
    };
    @Override
    protected void onResume() {
        super.onResume();
        mAMapNaviView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mAMapNaviView.onPause();
        //        仅仅是停止你当前在说的这句话，一会到新的路口还是会再说的
        //
        //        停止导航之后，会触及底层stop，然后就不会再有回调了，但是讯飞当前还是没有说完的半句话还是会说完
        //        mAMapNavi.stopNavi();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAMapNaviView.onDestroy();
        //释放导航对象资源
        //退出时调用此接口释放导航资源，在调用此接口后不能再调用AMapNavi类里的其它接口。
        mAMapNavi.stopNavi();
        //停止导航，包含实时导航和模拟导航。

        /**
         * 当前页面不销毁AmapNavi对象。
         * 因为可能会返回到RestRouteShowActivity页面再次进行路线选择，然后再次进来导航。
         * 如果销毁了就没办法在上一个页面进行选择路线了。
         * 但是AmapNavi对象始终销毁，那我们就需要在上一个页面用户回退时候销毁了。
         */
        mAMapNavi.removeAMapNaviListener(this);
        //移除导航对象的监听。
        //参数:
        //naviListener - 监听listener。

        // 清空缓存
        try {
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 关闭流

    }

    @Override
    public void onInitNaviFailure() {
        //导航初始化失败时的回调函数。
        Toast.makeText(this, "init navi Failed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onInitNaviSuccess() {
        //导航初始化成功时的回调函数。
    }

    @Override
    public void onStartNavi(int type) {
        //启动导航后的回调函数
        //CRUISE
        //巡航模式（数值：3）
        //EMULATOR
        //模拟导航（数值：2）
        //	GPS
        //实时导航（数值：1）
        //	NONE
        //未开始导航（数值：-1）

    }

    @Override
    public void onTrafficStatusUpdate() {
        //当前方路况光柱信息有更新时回调函数。 注意：该接口仅驾车模式有效
    }


    @Override
    public void onPoiSearched(PoiResult poiResult, int i) {
        if (i==1000) {
            PoiSearch.Query thisQuery = poiResult.getQuery();
            ArrayList<PoiItem> pois = poiResult.getPois();
            if (!pois.isEmpty()){
                Log.i("Poi","Get POI category " + thisQuery.getCategory());
                if (thisQuery.getCategory().equals("150900")) {  // 查询停车场
                    enter = pois.get(0).getEnter();
                    if (enter == null)
                        enter = pois.get(0).getLatLonPoint();
                }else{  // 查询附近
                    GlobalGpsPoisPacket = new GpsPoisPacket(pois);
                    b_sendPoisPacket = true;
                    for (int k=0;k<pois.size();k++){
                        Log.i("Poi","POI " + pois.get(k).getTitle() + " " + pois.get(k).getTypeDes());
                    }
                    Log.i("Poi","Get POI" + " Get poi succeed: " + pois.size());
                }
            }else
                Toast.makeText(getApplicationContext(), "目的地附近找不到停车场", Toast.LENGTH_SHORT).show();
        }else Log.i("Poi","Error Code=" + String.valueOf(i));

    }

    public static float calDis(LatLonPoint a, LatLonPoint b) {
        return AMapUtils.calculateLineDistance(new LatLng(a.getLatitude(),a.getLongitude()),
                new LatLng(b.getLatitude(),b.getLongitude()));
    }

    @Override
    public void onPoiItemSearched(PoiItem poiItem, int errorCode) {

    }


    @Override
    public void onLocationChange(AMapNaviLocation location) {
        //当位置信息有更新时的回调函数。
        Log.i("RealLocation","GpsPoint:"+location.getCoord().toString()); //Lat Lng
        Log.i("RealLocation","CarDirection:"+location.getBearing()); //float
        Log.i("RealLocation","CarAccuracy:"+location.getAccuracy()); //float
        Log.i("RealLocation","CarSpeed:"+location.getSpeed()); //float
        Log.i("RealLocation","Timestamp:"+location.getTime()); //Long
        Log.i("RealLocation","CurStep:"+location.getCurStepIndex()); //Long
        Log.i("RealLocation","CurLink:"+location.getCurLinkIndex()); //Long
        Log.i("RealLocation","CurPoint:"+location.getCurPointIndex()); //Long

        //UInt16 naviDistance? 距离下一转弯点的距离 m
        LatLng latLng = new LatLng(location.getCoord().getLatitude(),location.getCoord().getLongitude());
        GpsPoint _carLoc = new GpsPoint(location.getCoord().getLatitude(),location.getCoord().getLongitude(),
                AMapUtils.calculateLineDistance(latLng,new LatLng(GlobalRefPoint.latitude,GlobalRefPoint.longitude)));

        // 每开过200米搜索附近200米范围的POI
        if (AMapUtils.calculateLineDistance(latLng,lastpoint)>2000
        ){
            PoiSearch.Query noQuery = new PoiSearch.Query("", "110100|110200|060100");
            //110100	风景名胜	公园广场	公园广场
            //110200	风景名胜	风景名胜	风景名胜
            //060100	购物服务    商场
            noQuery.setDistanceSort(true);
            PoiSearch noPoiSearch = null;
            try {
                noPoiSearch = new PoiSearch(this, noQuery);
            } catch (AMapException e) {
                e.printStackTrace();
            }
            noPoiSearch.setOnPoiSearchListener(this);
            noPoiSearch.setBound(new PoiSearch.SearchBound(new LatLonPoint(lastpoint.latitude,
                    lastpoint.longitude), 4000));//设置周边搜索的中心点以及半径
            noPoiSearch.searchPOIAsyn();
            lastpoint = latLng.clone();
        }

        GlobalRealtimePacket.carLoc = _carLoc;
        GlobalRealtimePacket.timestamp = location.getTime();
        GlobalRealtimePacket.carDirection = location.getBearing();
        GlobalRealtimePacket.carSpeed = location.getSpeed();
        GlobalRealtimePacket.accuracy = location.getAccuracy();
        GlobalRealtimePacket.altitude = location.getAltitude();
        GlobalRealtimePacket.setRouteInfo(location.getCurLinkIndex(),location.getCurPointIndex(),location.getCurStepIndex(),location.isMatchNaviPath());

        b_sendRealtimePacket = true;

    }

    @Override
    public void onGetNavigationText(int type, String text) {
        //导航播报信息回调函数。
        //参数:
        //type - 播报类型枚举，详情见 NaviTTSType
        //text - 播报文案
        Log.i("Navi","Type=" + String.valueOf(type) +",NaviText="+ text);
    }

    @Override
    public void onGetNavigationText(String s) {
        //已过时。
        //导航播报信息回调函数。
        //参数:
        //text - 播报文字。
        Log.i("Navi","NaviText=" + s);
    }

    @Override
    public void onEndEmulatorNavi() {
        //模拟导航停止后回调函数。
        onArriveDestination();
    }

    @Override
    public void onArriveDestination() {
        //到达目的地后回调函数。
//        if (!naviOver && des!=enter){
//            naviOver = true;
//            mAMapNavi.stopNavi();
//            startList.clear();
//            startList.add(new NaviLatLng(des.getLatitude(),des.getLongitude()));
//            wayList.clear();
//            endList.clear();
//            endList.add(new NaviLatLng(enter.getLatitude(),enter.getLongitude()));
//            int strategyFlag = 0;
//            try {
//                strategyFlag = mAMapNavi.strategyConvert(true, true, true, false, true);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            if (strategyFlag >= 0) {
//                String carNumber = "沪A8888";
//                AMapCarInfo carInfo = new AMapCarInfo();
//                //设置车牌
//                carInfo.setCarNumber(carNumber);
//                //设置车牌是否参与限行算路
//                carInfo.setRestriction(false);
//                mAMapNavi.setCarInfo(carInfo);
//                mAMapNavi.calculateDriveRoute(startList, endList, wayList, strategyFlag);
//                Toast.makeText(getApplicationContext(), "策略:" + strategyFlag, Toast.LENGTH_LONG).show();
//            }
//        }
    }

    @Override
    public void onCalculateRouteFailure(int errorInfo) {
        //已过时。 该方法在6.1.0版本废弃，但是还会正常回调，建议使用AMapNaviListener.onCalculateRouteFailure(AMapCalcRouteResult) 方法替换
        //步行或者驾车路径规划失败后的回调函数。
        Toast.makeText(getApplicationContext(), "计算停车路径失败，errorcode＝" + errorInfo, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onReCalculateRouteForYaw() {
        //偏航后准备重新规划路线前的通知回调。
        //此方法只是通知准备重算事件，开发者不需要在方法中触发算路逻辑，SDK内部会进行算路。

    }

    @Override
    public void onReCalculateRouteForTrafficJam() {
        //驾车导航时，当前方遇到拥堵时准备重新规划路线前的通知回调。
        //此方法只是通知准备重算事件，开发者不需要在方法中触发算路逻辑，SDK内部会进行算路。
    }

    @Override
    public void onArrivedWayPoint(int wayID) {
        //驾车路径导航到达某个途经点的回调函数。 注意：该接口仅驾车模式有效
        //参数:
        //wayID - 到达途径点的编号，标号从0开始，依次累加。
    }

    @Override
    public void onGpsOpenStatus(boolean enabled) {
        //用户手机位置信息设置是否开启的回调函数。
        //参数:
        //enabled - true,开启;false,未开启。
    }

    @Override
    public void onNaviSetting() {
        //界面右下角设置按钮的点击回调
    }

    @Override
    public void onNaviMapMode(int isLock) {
        //导航视角变化回调
        //参数:
        //naviMode - 导航视角，0:车头朝上状态；1:正北朝上模式。

    }

    @Override
    public void onNaviCancel() {
        //导航页面左下角返回按钮点击后弹出的『退出导航』对话框中选择『确定』后的回调接口。
        finish();
    }

    @Override
    public void onNaviTurnClick() {
        //已过时。
        //界面左上角转向操作的点击回调

    }

    @Override
    public void onNextRoadClick() {
        //已过时。
        //界面下一道路名称的点击回调

    }

    @Override
    public void onScanViewButtonClick() {
        //界面全览按钮的点击回调。
    }

    @Override
    public void updateCameraInfo(AMapNaviCameraInfo[] aMapCameraInfos) {
        //导航过程中的摄像头信息回调函数 注意：该接口仅驾车模式有效
        //参数:
        //infoArray - 摄像头对象数组
    }

    @Override
    public void onServiceAreaUpdate(AMapServiceAreaInfo[] amapServiceAreaInfos) {
        //服务区信息回调函数 注意：该接口仅驾车模式有效
        //参数:
        //infoArray - 服务区对象数组
        ArrayList<AMapServiceAreaInfo> saInfoAL = new ArrayList<AMapServiceAreaInfo>();
        for (int i = 0;i<amapServiceAreaInfos.length;i++){
            if (amapServiceAreaInfos[i].getType()!=0) continue;
            saInfoAL.add(amapServiceAreaInfos[i]);
            Log.i("serviceArea",amapServiceAreaInfos[i].getName());
            Log.i("serviceArea","location:"+amapServiceAreaInfos[i].getCoordinate().toString());
            Log.i("serviceArea","Dist:"+amapServiceAreaInfos[i].getRemainDist());
            Log.i("serviceArea","Type:"+amapServiceAreaInfos[i].getType());
        }
        if(saInfoAL.size()>0){
            GlobalGpsSAPacket = new GpsServiceAreaPacket(saInfoAL);
            b_sendSAPacket = true;
        }
    }

    //增加Info的类型
    @Override
    public void onNaviInfoUpdate(NaviInfo naviinfo) {
        //导航引导信息回调。
        //参数:
        //naviInfo - 导航信息类对象。

        //传输数据包
        Log.i("RealTimeInfo","Type:" + naviinfo.getIconType()+"\n"
                +"RetainDistance:" + naviinfo.getCurStepRetainDistance()+"\n"
                +"CurStep:"+naviinfo.getCurStep()+"\n"
                +"CurLink:"+naviinfo.getCurLink()+"\n"
                +"CurPoint:"+naviinfo.getCurPoint()+"\n"
                +"CurRoadName:"+naviinfo.getCurrentRoadName()+"\n"
                +"NextRoadName:"+naviinfo.getNextRoadName()+"\n"
                +"CurIconType:"+naviinfo.getIconType()+"\n"
                +"CurNaviType:"+naviinfo.getNaviType()
        );
        GlobalRealtimePacket.naviType = naviinfo.getIconType();
        GlobalRealtimePacket.setNaviInfo(naviinfo.getCurStepRetainDistance(), naviinfo.getCurStepRetainTime(),
                naviinfo.getPathRetainDistance(),naviinfo.getPathRetainTime(),
                naviinfo.getRouteRemainLightCount(),naviinfo.getCurrentRoadName(),
                naviinfo.getNextRoadName());
    }

    @Override
    public void OnUpdateTrafficFacility(AMapNaviTrafficFacilityInfo aMapNaviTrafficFacilityInfo) {
        //已过时。
        //已过期，请注册并使用AimlessModeListener.onUpdateTrafficFacility(AMapNaviTrafficFacilityInfo[]) 注意：该接口仅驾车模式有效
    }

    @Override
    public void showCross(AMapNaviCross aMapNaviCross) {
        //显示路口放大图回调(实景图)。 注意：该接口仅驾车模式有效
        //参数:
        //aMapNaviCross - 路口放大图类，可以获得此路口放大图bitmap
        GlobalRealtimePacket.bShowCross = true;
    }

    @Override
    public void hideCross() {
        //关闭路口放大图回调(实景图)。 注意：该接口仅驾车模式有效
        GlobalRealtimePacket.bShowCross = false;
    }

    @Override
    public void onCalculateRouteSuccess(int[] ints) {
        //已过时。 该方法在6.1.0版本废弃，但是还会正常回调，建议使用AMapNaviListener.onCalculateRouteSuccess(AMapCalcRouteResult) 方法替换
        //算路成功回调 注意：该接口仅驾车模式有效
        AMapNaviPath myPath = mAMapNavi.getNaviPath();
        NaviLatLng firstPoint = myPath.getStartPoint();
        NaviLatLng endPoint = myPath.getEndPoint();

        des = new LatLonPoint(endPoint.getLatitude(), endPoint.getLongitude());
        enter = des.copy();

        refLatLng = new LatLng(firstPoint.getLatitude(),firstPoint.getLongitude());
        GlobalRefPoint = new GpsPoint(firstPoint.getLatitude(),firstPoint.getLongitude(),0.0);
        GpsPoint.refGpsPoint = GlobalRefPoint;
        GpsPoint[] routePoints = new GpsPoint[myPath.getCoordList().size()];

        GlobalNaviRoutePacket = new GpsNaviRoutePacket(GlobalRefPoint,routePoints,0);
        GlobalNaviRoutePacket.setAllLengthTime(myPath.getAllLength(),myPath.getAllTime());

        LatLng pathCenterLatLng = new LatLng(myPath.getCenterForPath().getLatitude(),myPath.getCenterForPath().getLongitude());
        GpsPoint naviPathCenter = new GpsPoint(pathCenterLatLng.latitude,pathCenterLatLng.longitude,
                AMapUtils.calculateLineDistance(pathCenterLatLng,refLatLng));
        GlobalNaviRoutePacket.setCenter(naviPathCenter);

        LatLng pathEndLatLng = new LatLng(myPath.getEndPoint().getLatitude(),myPath.getEndPoint().getLongitude());
        GpsPoint naviPathEnd = new GpsPoint(pathEndLatLng.latitude,pathEndLatLng.longitude,
                AMapUtils.calculateLineDistance(pathCenterLatLng,refLatLng));
        GlobalNaviRoutePacket.setEnd(naviPathEnd);

        GpsNaviStep[] naviRouteSteps = new GpsNaviStep[myPath.getStepsCount()];
        for (int i =0;i<myPath.getStepsCount();i++){
            GpsNaviLink[] StepLinks = new GpsNaviLink[myPath.getSteps().get(i).getLinks().size()];
            for (int j =0;j<StepLinks.length;j++){

                StepLinks[j] = new GpsNaviLink(myPath.getSteps().get(i).getLinks().get(j).getRoadName(),
                        myPath.getSteps().get(i).getLinks().get(j).getLength(),
                        myPath.getSteps().get(i).getLinks().get(j).getLinkType(),
                        myPath.getSteps().get(i).getLinks().get(j).getRoadClass(),
                        myPath.getSteps().get(i).getLinks().get(j).getRoadType(),
                        myPath.getSteps().get(i).getLinks().get(j).getTrafficStatus(),
                        myPath.getSteps().get(i).getLinks().get(j).getCoords().size());
            }
            naviRouteSteps[i] = new GpsNaviStep(myPath.getSteps().get(i).getIconType(),
                    myPath.getSteps().get(i).getLength(),
                    myPath.getSteps().get(i).getTime(),
                    myPath.getSteps().get(i).getStartIndex(),
                    myPath.getSteps().get(i).getEndIndex(),
                    myPath.getSteps().get(i).getTrafficLightCount(),
                    myPath.getSteps().get(i).isArriveWayPoint(),
                    myPath.getSteps().get(i).getCoords().size(), StepLinks);
        }

        GlobalNaviRoutePacket.setNaviSteps(naviRouteSteps);
        b_sendRoutePacket = true;

//        if (!naviOver && des!=enter){
//            naviOver = true;
//            mAMapNavi.stopNavi();
//            startList.clear();
//            startList.add(new NaviLatLng(des.getLatitude(),des.getLongitude()));
//            wayList.clear();
//            endList.clear();
//            endList.add(new NaviLatLng(enter.getLatitude(),enter.getLongitude()));
//            int strategyFlag = 0;
//            try {
//                strategyFlag = mAMapNavi.strategyConvert(true, true, true, false, true);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            if (strategyFlag >= 0) {
//                String carNumber = "沪A8888";
//                AMapCarInfo carInfo = new AMapCarInfo();
//                //设置车牌
//                carInfo.setCarNumber(carNumber);
//                //设置车牌是否参与限行算路
//                carInfo.setRestriction(false);
//                mAMapNavi.setCarInfo(carInfo);
//                mAMapNavi.calculateDriveRoute(startList, endList, wayList, strategyFlag);
//                Toast.makeText(getApplicationContext(), "策略:" + strategyFlag, Toast.LENGTH_LONG).show();
//            }
//        }


    }

    @Override
    public void notifyParallelRoad(int i) {
        //已过时。 已过期，请使用ParallelRoadListener.notifyParallelRoad(AMapNaviParallelRoadStatus)
        //通知当前是否显示平行路切换。 注意：该接口仅驾车模式有效
        //参数:
        //parallelRoadType - 0表示隐藏 1 表示显示主路 2 表示显示辅路
    }

    @Override
    public void OnUpdateTrafficFacility(AMapNaviTrafficFacilityInfo[] aMapNaviTrafficFacilityInfos) {
        //已过时。 已过期，建议使用AimlessModeListener.onUpdateTrafficFacility(AMapNaviTrafficFacilityInfo[])
        //巡航模式（无路线规划）下，道路设施信息更新回调。 注意：该接口仅驾车模式有效
    }

    @Override
    public void updateAimlessModeStatistics(AimLessModeStat aimLessModeStat) {
        //已过时。 已过期，建议使用AimlessModeListener.updateAimlessModeStatistics(AimLessModeStat)
        //巡航模式（无路线规划）下，统计信息更新回调。 连续5个点大于15km/h后开始回调。 注意：该接口仅驾车模式有效
    }

    @Override
    public void updateAimlessModeCongestionInfo(AimLessModeCongestionInfo aimLessModeCongestionInfo) {
        //已过时。 已过期，建议使用AimlessModeListener.updateAimlessModeCongestionInfo(AimLessModeCongestionInfo)
        //巡航模式（无路线规划）下，拥堵信息更新回调. 当前方无拥堵信息时，回调的AimLessModeCongestionInfo对象为空。 注意：该接口仅驾车模式有效
    }

    @Override
    public void onPlayRing(int i) {
        //回调各种类型的提示音，类似高德导航"叮". 注意：该接口仅驾车模式有效
        //参数:
        //type - 提示音类型，可以根据类型自定义播放声音
    }

    @Override
    public void onLockMap(boolean isLock) {
        //已过时。 请使用 AMapNaviViewListener.onNaviViewShowMode(int)
        //是否锁定地图的回调。
        //参数:
        //isLock - true代表锁车状态，地图未锁定。false代表非锁车状态，地图锁定。
    }

    @Override
    public void onNaviViewLoaded() {
        //导航view加载完成回调。
    }

    @Override
    public void onMapTypeChanged(int i) {
        //AMapNaviView地图白天黑夜模式切换回调
        //参数:
        //mapType - 枚举值参考AMap类 3-夜间模式 4-白天模式
    }

    @Override
    public void onNaviViewShowMode(int i) {
        //导航视图展示模式变化回调
        //参数:
        //showMode - 展示模式，具体类型可参考AMapNaviViewShowMode
    }

    @Override
    public boolean onNaviBackClick() {
        //导航页面左下角"退出"按钮的点击回调
        //返回:
        //返回值：false-由SDK主动弹出『退出导航』对话框，true-SDK不主动弹出『退出导航』对话框，由用户自定义
        return false;
    }


    @Override
    public void showModeCross(AMapModelCross aMapModelCross) {
        //显示路口放大图回调(模型图)。 注意：该接口仅驾车模式有效
        GlobalRealtimePacket.bShowCross = true;
    }

    @Override
    public void hideModeCross() {
        //关闭路口放大图回调(模型图)。 注意：该接口仅驾车模式有效
        GlobalRealtimePacket.bShowCross = false;
    }

    @Override
    public void updateIntervalCameraInfo(AMapNaviCameraInfo aMapNaviCameraInfo, AMapNaviCameraInfo aMapNaviCameraInfo1, int i) {
        //导航过程中的区间测速信息回调函数 注意：该接口仅驾车模式有效
        //参数:
        //startCameraInfo - 区间测速起点信息
        //endCameraInfo - 区间测速终点信息
        //status - 具体类型可参考CarEnterCameraStatus
    }

    @Override
    public void showLaneInfo(AMapLaneInfo aMapLaneInfo) {
        int [] backLanes = aMapLaneInfo.backgroundLane;
        int [] frontLanes = aMapLaneInfo.frontLane;
        int laneCount = aMapLaneInfo.laneCount;
        Log.i("LaneInfo","LaneCount:"+laneCount);
        for (int i=0;i<backLanes.length;i++){
            Log.i("LaneInfo","backLane:"+backLanes[i]);
        }
        for (int i=0;i<frontLanes.length;i++){
            Log.i("LaneInfo","frontLane:"+frontLanes[i]);
        }
        //显示道路信息回调。 注意：该接口仅驾车模式有效
        //参数:
        //laneInfo - 道路信息，可获得当前道路信息，可用于用户使用自己的素材完全自定义显示。
//        GlobalRealtimePacket.bShowLaneInfo = true;
    }
    @Override
    public void showLaneInfo(AMapLaneInfo[] laneInfos, byte[] laneBackgroundInfo, byte[] laneRecommendedInfo) {
        //已过时。 建议使用AMapNaviListener.showLaneInfo(AMapLaneInfo) 方法替换
        //显示道路信息回调。 注意：该接口仅驾车模式有效
        //参数:
        //laneInfos - 道路信息数组，可获得当前道路信息，可用于用户使用自己的素材完全自定义显示。
        //laneBackgroundInfo - 道路背景数据数组，可用于装载官方的DriveWayView，并显示。
        //laneRecommendedInfo - 道路推荐数据数组，可用于装载官方的DriveWayView，并显示。
        GlobalRealtimePacket.bShowLaneInfo = true;
        boolean[] isRecommanded = new boolean[laneInfos.length];
        int [] laneAction = new int[laneInfos.length];
        int [] userAction = new int[laneInfos.length];
        for(int i=0;i<laneInfos.length;i++){

            isRecommanded[i] = laneInfos[i].isRecommended();
            laneAction[i] = (int)laneInfos[i].getLaneTypeIdArray()[0];
            userAction[i] = (int)laneInfos[i].getLaneTypeIdArray()[1];
            Log.i("LaneInfo","第"+i+"条车道的信息typeIdArray:"
                    +laneAction[i]
                    +" "+userAction[i]
                    +" 此条道可行不："+isRecommanded[i]);
        }

        GlobalRealtimePacket.setLaneInfo(laneInfos.length,isRecommanded,laneAction,userAction);
    }

    @Override
    public void hideLaneInfo() {
        //关闭道路信息回调。 注意：该接口仅驾车模式有效
        GlobalRealtimePacket.bShowLaneInfo = false;
    }

    @Override
    public void onCalculateRouteSuccess(AMapCalcRouteResult aMapCalcRouteResult) {
        //路线规划成功回调，包括算路、导航中偏航、用户改变算路策略、行程点等触发的重算，
        // 具体算路结果可以通过AMapCalcRouteResult获取 可以通过CalcRouteResult获取算路错误码、算路类型以及路线id


    }

    @Override
    public void onCalculateRouteFailure(AMapCalcRouteResult aMapCalcRouteResult) {
        //路线规划失败回调，包括算路、导航中偏航、用户改变算路策略、行程点等触发的重算，
        // 具体算路结果可以通过AMapCalcRouteResult获取 可以通过CalcRouteResult获取算路错误码、算路类型以及路线id
    }

    @Override
    public void onNaviRouteNotify(AMapNaviRouteNotifyData aMapNaviRouteNotifyData) {
        //导航过程中道路信息通知 注意：该接口仅驾车模式有效
        //导航过程中针对拥堵区域、限行区域、禁行区域、道路封闭等情况的躲避通知。
        //通知和避让信息结果可以通过AMapNaviRouteNotifyData获取
    }

    @Override
    public void onGpsSignalWeak(boolean b) {
        //手机卫星定位信号强弱变化的回调
    }

    private void searchRoutePOI(RoutePOISearch.RoutePOISearchType type,AMapNaviPath path) throws AMapException {
        List<LatLonPoint> pathPoints = new ArrayList<LatLonPoint>();
        for (int i =0;i<path.getCoordList().size();i++){
            pathPoints.add(new LatLonPoint(path.getCoordList().get(i).getLatitude(),path.getCoordList().get(i).getLongitude()));
        }
        RoutePOISearchQuery query = new RoutePOISearchQuery(pathPoints, type, 500);
        final RoutePOISearch search = new RoutePOISearch(this, query);
        search.setPoiSearchListener(this);
        search.searchRoutePOIAsyn();
    }

    @Override
    public void onRoutePoiSearched(RoutePOISearchResult result, int errorCode) {
        if (errorCode == AMapException.CODE_AMAP_SUCCESS) {
            if(result != null){
                routePOIItemList.clear();
                routePOIItemList.addAll(result.getRoutePois());
                Log.i("Poi","RouteSearch count: "+result.getRoutePois().size());
                if (routePOIItemList == null || routePOIItemList.size() <= 0) {
                    Toast.makeText(getApplicationContext(), "沿途搜索加油站没有结果", Toast.LENGTH_LONG).show();
                }
                else{
                    GlobalGpsFSPacket = new GpsRouteFillingStationPacket(routePOIItemList);
                    b_sendFSPacket = true;
                }
            }
        }else{
            Toast.makeText(getApplicationContext(), "错误代码："+errorCode, Toast.LENGTH_LONG).show();
        }
    }
}