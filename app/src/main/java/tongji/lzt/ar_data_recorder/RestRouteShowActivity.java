package tongji.lzt.ar_data_recorder;


import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.Poi;
import com.amap.api.navi.AMapNavi;
import com.amap.api.navi.AMapNaviListener;
import com.amap.api.navi.NaviSetting;
import com.amap.api.navi.model.AMapCalcRouteResult;
import com.amap.api.navi.model.AMapCarInfo;
import com.amap.api.navi.model.AMapLaneInfo;
import com.amap.api.navi.model.AMapModelCross;
import com.amap.api.navi.model.AMapNaviCameraInfo;
import com.amap.api.navi.model.AMapNaviCross;
import com.amap.api.navi.model.AMapNaviLocation;
import com.amap.api.navi.model.AMapNaviPath;
import com.amap.api.navi.model.AMapNaviRouteNotifyData;
import com.amap.api.navi.model.AMapNaviTrafficFacilityInfo;
import com.amap.api.navi.model.AMapServiceAreaInfo;
import com.amap.api.navi.model.AimLessModeCongestionInfo;
import com.amap.api.navi.model.AimLessModeStat;
import com.amap.api.navi.model.NaviInfo;
import com.amap.api.navi.model.NaviLatLng;
import com.amap.api.navi.view.PoiInputItemWidget;
import com.amap.api.navi.view.RouteOverLay;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.RouteSearch;
import com.amap.api.services.routepoisearch.RoutePOIItem;
import com.amap.api.services.routepoisearch.RoutePOISearch;
import com.amap.api.services.routepoisearch.RoutePOISearchQuery;
import com.amap.api.services.routepoisearch.RoutePOISearchResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import tongji.lzt.ar_data_recorder.search.SearchPoiActivity;

//计算得到的路径显示处理
public class RestRouteShowActivity extends Activity implements AMapNaviListener, OnClickListener, OnCheckedChangeListener, RoutePOISearch.OnRoutePOISearchListener {
    private boolean congestion, cost, hightspeed, avoidhightspeed;
    /**
     * 导航对象(单例)
     */
    private AMapNavi mAMapNavi;
    private AMap mAmap;
    private RouteSearch mRouteSearch;
    private DriveRouteResult mDriveRouteResult;
    /**
     * 地图对象
     */
    private MapView mRouteMapView;
    private Marker mStartMarker;
    private Marker mEndMarker;
    private NaviLatLng endLatlng = new NaviLatLng(31.286012,121.21416);
    private NaviLatLng startLatlng = new NaviLatLng(31.286012,121.21416);
    private List<NaviLatLng> startList = new ArrayList<NaviLatLng>();

    private LatLonPoint mStartPoint = new LatLonPoint(31.286012,121.21416);
    private LatLonPoint mEndPoint = new LatLonPoint(31.286012,121.21416);
    /**
     * 途径点坐标集合
     */
    private List<NaviLatLng> wayList = new ArrayList<NaviLatLng>();
    /**
     * 终点坐标集合［建议就一个终点］
     */
    private List<NaviLatLng> endList = new ArrayList<NaviLatLng>();
    /**
     * 保存当前算好的路线
     */
    private SparseArray<RouteOverLay> routeOverlays = new SparseArray<RouteOverLay>();

    /**
     * 当前用户选中的路线，在下个页面进行导航
     */
    private int routeIndex;
    /**
     * 路线的权值，重合路线情况下，权值高的路线会覆盖权值低的路线
     **/
    private int zindex = 1;
    /**
     * 路线计算成功标志位
     */
    private boolean calculateSuccess = false;
    private boolean chooseRouteSuccess = false;
    private EditText dataText;
    private EditText videoText;

    private myRoutePoiOverlay overlay;

    private List<RoutePOIItem> routePOIItemList = new ArrayList<RoutePOIItem>();

    // 需要的权限
    private static final int ACTION_REQUEST_PERMISSIONS = 1;
    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rest_calculate);
        dataText = (EditText) findViewById(R.id.data_name); //car_number IP地址
        videoText = (EditText) findViewById(R.id.video_name);
        CheckBox congestion = (CheckBox) findViewById(R.id.congestion);
        CheckBox cost = (CheckBox) findViewById(R.id.cost);
        CheckBox hightspeed = (CheckBox) findViewById(R.id.hightspeed);
        CheckBox avoidhightspeed = (CheckBox) findViewById(R.id.avoidhightspeed);
        Button calculate = (Button) findViewById(R.id.calculate); //开始算路按钮
        Button startPoint = (Button) findViewById(R.id.startpoint); //选起点
        Button endPoint = (Button) findViewById(R.id.endpoint); //选终点
        Button selectroute = (Button) findViewById(R.id.selectroute); //选路径
        Button gpsnavi = (Button) findViewById(R.id.gpsnavi); //开始导航按钮
        Button emulatornavi = (Button) findViewById(R.id.emulatornavi); //模拟导航按钮
        calculate.setOnClickListener(this);
        startPoint.setOnClickListener(this);
        endPoint.setOnClickListener(this);
        selectroute.setOnClickListener(this);
        gpsnavi.setOnClickListener(this);
        emulatornavi.setOnClickListener(this);
        congestion.setOnCheckedChangeListener(this);
        cost.setOnCheckedChangeListener(this);
        hightspeed.setOnCheckedChangeListener(this);
        avoidhightspeed.setOnCheckedChangeListener(this);

        mRouteMapView = (MapView) findViewById(R.id.navi_view);
        mRouteMapView.onCreate(savedInstanceState);
        //mRouteMapView.
        mAmap = mRouteMapView.getMap();
        mAmap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(
                new LatLng(31.286012,121.21416), 13, 0 , 0
                 )
            )
        );
        // 初始化Marker添加到地图
        mStartMarker = mAmap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.start))));
        mEndMarker = mAmap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.end))));

        try {
            NaviSetting.updatePrivacyShow(getApplicationContext(), true, true);
            NaviSetting.updatePrivacyAgree(getApplicationContext(), true);
            mAMapNavi = AMapNavi.getInstance(getApplicationContext());
        } catch (com.amap.api.maps.AMapException e) {
            e.printStackTrace();
        }
        mAMapNavi.addAMapNaviListener(this);

        if (!checkPermissions(NEEDED_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS, ACTION_REQUEST_PERMISSIONS);
        }
    }

    private boolean checkPermissions(String[] neededPermissions) {
        if (neededPermissions == null || neededPermissions.length == 0) {
            return true;
        }
        boolean allGranted = true;
        for (String neededPermission : neededPermissions) {
            allGranted &= ContextCompat.checkSelfPermission(this, neededPermission) == PackageManager.PERMISSION_GRANTED;
        }
        return allGranted;
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onResume() {
        super.onResume();
        mRouteMapView.onResume();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onPause() {
        super.onPause();
        mRouteMapView.onPause();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mRouteMapView.onSaveInstanceState(outState);
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        startList.clear();
        wayList.clear();
        endList.clear();
        routeOverlays.clear();
        mAmap.clear();// 清理地图上的所有覆盖物
        mRouteMapView.onDestroy();
        /**
         * 当前页面只是展示地图，activity销毁后不需要再回调导航的状态
         */
        mAMapNavi.removeAMapNaviListener(this);
        mAMapNavi.destroy();

    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int id = buttonView.getId();
        switch (id) {
            case R.id.congestion:
                congestion = isChecked;
                break;
            case R.id.avoidhightspeed:
                avoidhightspeed = isChecked;
                break;
            case R.id.cost:
                cost = isChecked;
                break;
            case R.id.hightspeed:
                hightspeed = isChecked;
                break;
            default:
                break;
        }
    }

    @Override
    public void onInitNaviSuccess() {
    }

    @Override
    public void onCalculateRouteSuccess(int[] ints) {
        //清空上次计算的路径列表。
        routeOverlays.clear();
        mAmap.clear();// 清理地图上的所有覆盖物
        HashMap<Integer, AMapNaviPath> paths = mAMapNavi.getNaviPaths();
        for (int i = 0; i < ints.length; i++) {
            List<NaviLatLng> coorList = null;

            AMapNaviPath path = paths.get(ints[i]);
            if (path != null) {
                drawRoutes(ints[i], path);
                coorList = path.getCoordList();
//                Log.i("Path",coorList.size()+ coorList.toString());
            }
        }
    }

    private void drawRoutes(int routeId, AMapNaviPath path) {
        calculateSuccess = true;
        mAmap.moveCamera(CameraUpdateFactory.changeTilt(0));
        RouteOverLay routeOverLay = new RouteOverLay(mAmap, path, this);
        routeOverLay.setTrafficLine(false);
        routeOverLay.addToMap();
        routeOverlays.put(routeId, routeOverLay);
    }

    @Override
    public void onCalculateRouteFailure(int arg0) {
        calculateSuccess = false;
        Toast.makeText(getApplicationContext(), "计算路线失败，errorcode＝" + arg0, Toast.LENGTH_SHORT).show();
    }


    public void changeRoute() throws AMapException {
        if (!calculateSuccess) {
            Toast.makeText(this, "请先算路", Toast.LENGTH_SHORT).show();
            return;
        }
        /**
         * 计算出来的路径只有一条
         */
        if (routeOverlays.size() == 1) {
            chooseRouteSuccess = true;
            //必须告诉AMapNavi 你最后选择的哪条路
            mAMapNavi.selectRouteId(routeOverlays.keyAt(0));
            searchRoutePOI(RoutePOISearch.RoutePOISearchType.TypeGasStation,
                    mAMapNavi.getNaviPath());
            Toast.makeText(this, "导航距离:" + (mAMapNavi.getNaviPath()).getAllLength() + "m" + "\n" + "导航时间:" + (mAMapNavi.getNaviPath()).getAllTime() + "s", Toast.LENGTH_SHORT).show();
            return;
        }

        if (routeIndex >= routeOverlays.size()) {
            routeIndex = 0;
        }
        int routeID = routeOverlays.keyAt(routeIndex);
        //突出选择的那条路
        for (int i = 0; i < routeOverlays.size(); i++) {
            int key = routeOverlays.keyAt(i);
            routeOverlays.get(key).setTransparency(0.4f);
        }
        RouteOverLay routeOverlay = routeOverlays.get(routeID);
        if(routeOverlay != null){
            routeOverlay.setTransparency(1);
            /**把用户选择的那条路的权值弄高，使路线高亮显示的同时，重合路段不会变的透明**/
            routeOverlay.setZindex(zindex++);
        }
        //必须告诉AMapNavi 你最后选择的哪条路
        mAMapNavi.selectRouteId(routeID);
        Toast.makeText(this, "路线标签:" + mAMapNavi.getNaviPath().getLabels(), Toast.LENGTH_SHORT).show();
        routeIndex++;
        chooseRouteSuccess = true;

        searchRoutePOI(RoutePOISearch.RoutePOISearchType.TypeGasStation,
                mAMapNavi.getNaviPath());
        /**选完路径后判断路线是否是限行路线**/
//        AMapRestrictionInfo info = mAMapNavi.getNaviPath().getRestrictionInfo();
//        if (!TextUtils.isEmpty(info.getRestrictionTitle())) {
//            Toast.makeText(this, info.getRestrictionTitle(), Toast.LENGTH_SHORT).show();
//        }
    }

    private void searchRoutePOI(RoutePOISearch.RoutePOISearchType type,AMapNaviPath path) throws AMapException {
        Log.i("RouteSearch Mode","searchRoutePOI type "+type);
        if (overlay != null) {
            overlay.removeFromMap();
        }
        List<LatLonPoint> pathPoints = new ArrayList<LatLonPoint>();
        for (int i =0;i<path.getCoordList().size();i++){
            pathPoints.add(new LatLonPoint(path.getCoordList().get(i).getLatitude(),path.getCoordList().get(i).getLongitude()));
        }
        RoutePOISearchQuery query = new RoutePOISearchQuery(pathPoints, type, 500);
        final RoutePOISearch search = new RoutePOISearch(this, query);
        search.setPoiSearchListener(this);
        search.searchRoutePOIAsyn();
    }


    /**
     * 清除当前地图上算好的路线
     */
    private void clearRoute() {
        for (int i = 0; i < routeOverlays.size(); i++) {
            RouteOverLay routeOverlay = routeOverlays.valueAt(i);
            routeOverlay.removeFromMap();
        }
        routeOverlays.clear();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.calculate:
                clearRoute();
                if (avoidhightspeed && hightspeed) {
                    Toast.makeText(getApplicationContext(), "不走高速与高速优先不能同时为true.", Toast.LENGTH_LONG).show();
                }
                if (cost && hightspeed) {
                    Toast.makeText(getApplicationContext(), "高速优先与避免收费不能同时为true.", Toast.LENGTH_LONG).show();
                }
                /*
                 * strategyFlag转换出来的值都对应PathPlanningStrategy常量，用户也可以直接传入PathPlanningStrategy常量进行算路。
                 * 如:mAMapNavi.calculateDriveRoute(mStartList, mEndList, mWayPointList,PathPlanningStrategy.DRIVING_DEFAULT);
                 */
                int strategyFlag = 0;
                try {
                    strategyFlag = mAMapNavi.strategyConvert(congestion, avoidhightspeed, cost, hightspeed, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (strategyFlag >= 0) {
                    String carNumber = "沪A88888";
                    AMapCarInfo carInfo = new AMapCarInfo();
                    //设置车牌
                    carInfo.setCarNumber(carNumber);
                    //设置车牌是否参与限行算路
                    carInfo.setRestriction(true);
                    mAMapNavi.setCarInfo(carInfo);
                    mAMapNavi.calculateDriveRoute(startList, endList, wayList, strategyFlag);

                    mStartPoint = new LatLonPoint(startList.get(0).getLatitude(),startList.get(0).getLongitude());//起点
                    mEndPoint = new LatLonPoint(endList.get(0).getLatitude(),endList.get(0).getLongitude());//终点
                    //Route在mAMapNavi里，在context里。
                    Toast.makeText(getApplicationContext(), "策略:" + strategyFlag, Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.startpoint:
                Intent sintent = new Intent(RestRouteShowActivity.this, SearchPoiActivity.class);
                Bundle bundle = new Bundle();
                bundle.putInt("pointType", PoiInputItemWidget.TYPE_START);
                sintent.putExtras(bundle);
                startActivityForResult(sintent, 100);
                break;
            case R.id.endpoint:
                Intent eintent = new Intent(RestRouteShowActivity.this, SearchPoiActivity.class);
                Bundle ebundle = new Bundle();
                ebundle.putInt("pointType", PoiInputItemWidget.TYPE_DEST);
                eintent.putExtras(ebundle);
                startActivityForResult(eintent, 200);
                break;
            case R.id.selectroute:
                try {
                    changeRoute();
                } catch (AMapException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.gpsnavi:
                Intent gpsintent = new Intent(getApplicationContext(), RouteNaviActivity.class);
                gpsintent.putExtra("gps", true);
                String data_name = dataText.getText().toString();
                String video_name = videoText.getText().toString();
                gpsintent.putExtra("data",data_name);
                gpsintent.putExtra("video",video_name);
                startActivity(gpsintent); //启动活动
                break;
            case R.id.emulatornavi:
                Intent intent = new Intent(getApplicationContext(), RouteNaviActivity.class);
                intent.putExtra("gps", false);
                String data_name_2 = dataText.getText().toString();
                String video_name_2 = videoText.getText().toString();
                intent.putExtra("data",data_name_2);
                intent.putExtra("video",video_name_2);
                startActivity(intent); //启动活动
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null && data.getParcelableExtra("poi") != null) {
            clearRoute();
            Poi poi = data.getParcelableExtra("poi");
            if (requestCode == 100) {//起点选择完成
                startLatlng = new NaviLatLng(poi.getCoordinate().latitude, poi.getCoordinate().longitude);
                mStartMarker.setPosition(new LatLng(poi.getCoordinate().latitude, poi.getCoordinate().longitude));
                startList.clear();
                startList.add(startLatlng);
                mAmap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(
                                new LatLng(poi.getCoordinate().latitude, poi.getCoordinate().longitude),
                                13, 0 , 0)
                        )
                );
            }
            if (requestCode == 200) {//终点选择完成
                endLatlng = new NaviLatLng(poi.getCoordinate().latitude, poi.getCoordinate().longitude);
                mEndMarker.setPosition(new LatLng(poi.getCoordinate().latitude, poi.getCoordinate().longitude));
                endList.clear();
                endList.add(endLatlng);
//                mAmap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(
//                                new LatLng(poi.getCoordinate().latitude, poi.getCoordinate().longitude),
//                                13, 0 , 0)
//                        )
//                );
            }
        }
    }


    @Override
    public void onRoutePoiSearched(RoutePOISearchResult result, int errorCode) {
        if (errorCode == AMapException.CODE_AMAP_SUCCESS) {
            if(result != null){
                routePOIItemList.clear();
                routePOIItemList.addAll(result.getRoutePois());
                Log.i("RouteSearch Mode","count: "+result.getRoutePois().size());
                if (routePOIItemList != null && routePOIItemList.size() > 0) {
                    if (overlay != null) {
                        overlay.removeFromMap();
                    }
                    overlay = new myRoutePoiOverlay(mAmap, routePOIItemList);
                    overlay.addToMap();
                } else {
                    Toast.makeText(getApplicationContext(), "没有结果", Toast.LENGTH_LONG).show();
                }
            }
        }else{
            Toast.makeText(getApplicationContext(), "错误代码："+errorCode, Toast.LENGTH_LONG).show();
        }
    }


    /**
     * 自定义PoiOverlay
     *
     */

    private class myRoutePoiOverlay {
        private AMap mamap;
        private List<RoutePOIItem> mPois;
        private ArrayList<Marker> mPoiMarks = new ArrayList<Marker>();
        public myRoutePoiOverlay(AMap amap ,List<RoutePOIItem> pois) {
            mamap = amap;
            mPois = pois;
        }

        /**
         * 添加Marker到地图中。
         * @since V2.1.0
         */
        public void addToMap() {
            for (int i = 0; i < mPois.size(); i++) {
                Marker marker = mamap.addMarker(getMarkerOptions(i));
                RoutePOIItem item = mPois.get(i);
                marker.setObject(item);
                mPoiMarks.add(marker);
            }
        }

        /**
         * 去掉PoiOverlay上所有的Marker。
         *
         * @since V2.1.0
         */
        public void removeFromMap() {
            for (Marker mark : mPoiMarks) {
                mark.remove();
            }
        }

        /**
         * 移动镜头到当前的视角。
         * @since V2.1.0
         */
        public void zoomToSpan() {
            if (mPois != null && mPois.size() > 0) {
                if (mamap == null)
                    return;
                LatLngBounds bounds = getLatLngBounds();
                mamap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
            }
        }

        private LatLngBounds getLatLngBounds() {
            LatLngBounds.Builder b = LatLngBounds.builder();
            for (int i = 0; i < mPois.size(); i++) {
                b.include(new LatLng(mPois.get(i).getPoint().getLatitude(),
                        mPois.get(i).getPoint().getLongitude()));
            }
            return b.build();
        }

        private MarkerOptions getMarkerOptions(int index) {
            return new MarkerOptions()
                    .position(
                            new LatLng(mPois.get(index).getPoint()
                                    .getLatitude(), mPois.get(index)
                                    .getPoint().getLongitude()))
                    .title(getTitle(index)).snippet(getSnippet(index));
        }

        protected String getTitle(int index) {
            return mPois.get(index).getTitle();
        }

        protected String getSnippet(int index) {
            return mPois.get(index).getDistance() + "米  " + mPois.get(index).getDuration() + "秒";
        }

        /**
         * 从marker中得到poi在list的位置。
         *
         * @param marker 一个标记的对象。
         * @return 返回该marker对应的poi在list的位置。
         * @since V2.1.0
         */
        public int getPoiIndex(Marker marker) {
            for (int i = 0; i < mPoiMarks.size(); i++) {
                if (mPoiMarks.get(i).equals(marker)) {
                    return i;
                }
            }
            return -1;
        }

        /**
         * 返回第index的poi的信息。
         * @param index 第几个poi。
         * @return poi的信息。poi对象详见搜索服务模块的基础核心包（com.amap.api.services.core）中的类 <strong><a href="../../../../../../Search/com/amap/api/services/core/PoiItem.html" title="com.amap.api.services.core中的类">PoiItem</a></strong>。
         * @since V2.1.0
         */
        public RoutePOIItem getPoiItem(int index) {
            if (index < 0 || index >= mPois.size()) {
                return null;
            }
            return mPois.get(index);
        }
    }
    /**
     * ************************************************** 在算路页面，以下接口全不需要处理，在以后的版本中我们会进行优化***********************************************************************************************
     **/

    @Override
    public void OnUpdateTrafficFacility(AMapNaviTrafficFacilityInfo arg0) {


    }

    @Override
    public void OnUpdateTrafficFacility(AMapNaviTrafficFacilityInfo[] arg0) {


    }

    @Override
    public void hideCross() {


    }

    @Override
    public void hideLaneInfo() {


    }

    @Override
    public void notifyParallelRoad(int arg0) {


    }

    @Override
    public void onArriveDestination() {


    }

    @Override
    public void onArrivedWayPoint(int arg0) {


    }

    @Override
    public void onEndEmulatorNavi() {


    }

    @Override
    public void onGetNavigationText(int arg0, String arg1) {


    }

    @Override
    public void onGetNavigationText(String s) {

    }

    @Override
    public void onGpsOpenStatus(boolean arg0) {


    }

    @Override
    public void onInitNaviFailure() {


    }

    @Override
    public void onLocationChange(AMapNaviLocation arg0) {


    }

    @Override
    public void onNaviInfoUpdate(NaviInfo arg0) {


    }

    @Override
    public void updateCameraInfo(AMapNaviCameraInfo[] aMapCameraInfos) {

    }

    @Override
    public void onServiceAreaUpdate(AMapServiceAreaInfo[] amapServiceAreaInfos) {

    }

    @Override
    public void onReCalculateRouteForTrafficJam() {


    }

    @Override
    public void onReCalculateRouteForYaw() {


    }

    @Override
    public void onStartNavi(int arg0) {


    }

    @Override
    public void onTrafficStatusUpdate() {


    }

    @Override
    public void showCross(AMapNaviCross arg0) {


    }

    @Override
    public void showLaneInfo(AMapLaneInfo[] arg0, byte[] arg1, byte[] arg2) {


    }

    @Override
    public void updateAimlessModeCongestionInfo(AimLessModeCongestionInfo arg0) {


    }

    @Override
    public void onPlayRing(int i) {

    }

    @Override
    public void updateAimlessModeStatistics(AimLessModeStat arg0) {


    }

    @Override
    public void showModeCross(AMapModelCross aMapModelCross) {

    }

    @Override
    public void hideModeCross() {

    }

    @Override
    public void updateIntervalCameraInfo(AMapNaviCameraInfo aMapNaviCameraInfo, AMapNaviCameraInfo aMapNaviCameraInfo1, int i) {

    }

    @Override
    public void showLaneInfo(AMapLaneInfo aMapLaneInfo) {

    }

    @Override
    public void onCalculateRouteSuccess(AMapCalcRouteResult aMapCalcRouteResult) {

    }

    @Override
    public void onCalculateRouteFailure(AMapCalcRouteResult aMapCalcRouteResult) {
        calculateSuccess = false;
        Toast.makeText(getApplicationContext(), "计算路线失败，errorcode＝" + aMapCalcRouteResult.getErrorCode(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNaviRouteNotify(AMapNaviRouteNotifyData aMapNaviRouteNotifyData) {

    }

    @Override
    public void onGpsSignalWeak(boolean b) {

    }


}
