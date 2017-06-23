package com.baidu.map;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.map.overlayutil.DrivingRouteOverlay;
import com.baidu.map.overlayutil.TransitRouteOverlay;
import com.baidu.map.overlayutil.WalkingRouteOverlay;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.route.BikingRouteResult;
import com.baidu.mapapi.search.route.DrivingRouteLine;
import com.baidu.mapapi.search.route.DrivingRoutePlanOption;
import com.baidu.mapapi.search.route.DrivingRouteResult;
import com.baidu.mapapi.search.route.IndoorRouteResult;
import com.baidu.mapapi.search.route.MassTransitRouteResult;
import com.baidu.mapapi.search.route.OnGetRoutePlanResultListener;
import com.baidu.mapapi.search.route.PlanNode;
import com.baidu.mapapi.search.route.RoutePlanSearch;
import com.baidu.mapapi.search.route.TransitRouteLine;
import com.baidu.mapapi.search.route.TransitRoutePlanOption;
import com.baidu.mapapi.search.route.TransitRouteResult;
import com.baidu.mapapi.search.route.WalkingRouteLine;
import com.baidu.mapapi.search.route.WalkingRoutePlanOption;
import com.baidu.mapapi.search.route.WalkingRouteResult;
import com.baidu.mapapi.utils.DistanceUtil;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.baidu.map.WalkingRouteActivity.Method.BuXin;
import static com.baidu.map.WalkingRouteActivity.Method.GongJiao;
import static com.baidu.map.WalkingRouteActivity.Method.ZiJia;

/**
 * Created by Administrator on 2016/10/25.
 */

public class WalkingRouteActivity extends AppCompatActivity {
    @Bind(R.id.map)
    MapView mMap;
    @Bind(R.id.bt_buxin)
    Button mBtBuxin;
    @Bind(R.id.bt_gongjiao)
    Button mBtGongjiao;
    @Bind(R.id.bt_zijia)
    Button mBtZijia;
    @Bind(R.id.tv_content)
    TextView mTvContent;
    @Bind(R.id.sl)
    ScrollView mSl;
    private PoiInfo mPoiInfo;
    private BaiduMap mBaiduMap;
    private LocationClient mLocationClient;
    private MyLocationListenner myListener = new MyLocationListenner();
    //    private LatLng mLatLng;
    private LatLng mLatLng;
    private boolean isFirstLoc = true; // 是否首次定位
    private Method mMethod = BuXin;
    private TransitRouteOverlay mTransitRouteOverlay;
    private DrivingRouteOverlay mDrivingRouteOverlay;
    private WalkingRouteOverlay mWalkingRouteOverlay;

    public enum Method {
        BuXin, GongJiao, ZiJia
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walkingroute);
        ButterKnife.bind(this);
        boolean b = initIntent();
        if (!b) {
            Toast.makeText(this, "数据为空", Toast.LENGTH_SHORT).show();
            return;
        }
        initBaiDuMap();
        initBuXin();
    }

    private void initBaiDuMap() {
        mBaiduMap = mMap.getMap();
        // 开启定位图层
        mBaiduMap.setMyLocationEnabled(true);

        // 定位初始化
        mLocationClient = new LocationClient(this);
        mLocationClient.registerLocationListener(myListener);

        // 设置地图缩放级别为15
        mBaiduMap.setMapStatus(MapStatusUpdateFactory
                .newMapStatus(new MapStatus.Builder().zoom(15).build()));

        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true); // 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setIsNeedAddress(true);//可选，设置是否需要地址信息，默认不需要
        option.setScanSpan(1000);//扫描间隔 单位毫秒
        option.setLocationMode(LocationClientOption.LocationMode.Battery_Saving);
        mLocationClient.setLocOption(option);
        //设置箭头
        mBaiduMap.setMyLocationConfigeration(new MyLocationConfiguration(
                MyLocationConfiguration.LocationMode.NORMAL, true, null));
        mLocationClient.start();

    }

    private boolean initIntent() {
        Intent intent = getIntent();
        mPoiInfo = intent.getParcelableExtra("poiInfo");
        mLatLng = intent.getParcelableExtra("mLatLng");
        if (mPoiInfo == null || mLatLng == null) {
            return false;
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mMap.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMap.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMap.onPause();
    }

    @OnClick({R.id.bt_buxin, R.id.bt_gongjiao, R.id.bt_zijia})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bt_buxin://步行
                mMethod = BuXin;
                cleanView();
                initBuXin();
                break;
            case R.id.bt_gongjiao://公交
                mMethod = GongJiao;
                cleanView();
                initGongJiao();
                break;
            case R.id.bt_zijia://自驾
                mMethod = ZiJia;
                cleanView();
                initZiJia();
                break;
        }
    }

    private void cleanView() {
        mBaiduMap.clear();
    }

    private void initBuXin() {
        RoutePlanSearch planSearch = RoutePlanSearch.newInstance();
        planSearch.setOnGetRoutePlanResultListener(new MyOnGetRoutePlanResultListener());

        WalkingRoutePlanOption option = new WalkingRoutePlanOption();
        PlanNode from = PlanNode.withLocation(mLatLng);
        PlanNode to = PlanNode.withLocation(mPoiInfo.location);

        option.from(from);// 设置起点
        option.to(to);// 设置终点
        planSearch.walkingSearch(option);

        double distance = DistanceUtil.getDistance(mLatLng, mPoiInfo.location);
        Log.e("tag", "距离:" + distance+"米");
    }

    private void initGongJiao() {
        RoutePlanSearch planSearch = RoutePlanSearch.newInstance();
        planSearch.setOnGetRoutePlanResultListener(new MyOnGetRoutePlanResultListener());

        TransitRoutePlanOption option = new TransitRoutePlanOption();
        PlanNode from = PlanNode.withLocation(mLatLng);
        PlanNode to = PlanNode.withLocation(mPoiInfo.location);
        option.city(mPoiInfo.city);// 设置城市
        option.from(from);
        option.to(to);
        option.policy(TransitRoutePlanOption.TransitPolicy.EBUS_TIME_FIRST);// 设置策略
        planSearch.transitSearch(option);

        double distance = DistanceUtil.getDistance(mLatLng, mPoiInfo.location);
        Log.e("tag", "距离:" + distance+"米");
    }

    private void initZiJia() {
        RoutePlanSearch planSearch = RoutePlanSearch.newInstance();
        planSearch.setOnGetRoutePlanResultListener(new MyOnGetRoutePlanResultListener());

        DrivingRoutePlanOption option = new DrivingRoutePlanOption();
        PlanNode from = PlanNode.withLocation(mLatLng);
        PlanNode to = PlanNode.withLocation(mPoiInfo.location);
        option.policy(DrivingRoutePlanOption.DrivingPolicy.ECAR_TIME_FIRST);// 设置策略
        option.from(from);// 设置起点
        option.to(to);// 设置终点
        planSearch.drivingSearch(option);

        //计算p1、p2两点之间的直线距离，单位：米
        double distance = DistanceUtil.getDistance(mLatLng, mPoiInfo.location);
        Log.e("tag", "距离:" + distance+"米");
    }

    class MyOnGetRoutePlanResultListener implements OnGetRoutePlanResultListener {
        /**
         * 步行结果
         *
         * @param
         */
        @Override
        public void onGetWalkingRouteResult(WalkingRouteResult result) {
            if (result == null
                    || SearchResult.ERRORNO.RESULT_NOT_FOUND == result.error) {
                Toast.makeText(getApplicationContext(), "未查询到结果", Toast.LENGTH_SHORT).show();
                return;
            }
            if (mMethod == BuXin) {
                mWalkingRouteOverlay = new MyWalkingRouteOverlay(mBaiduMap);
                mBaiduMap.setOnMarkerClickListener(mWalkingRouteOverlay);
                WalkingRouteLine line = result.getRouteLines().get(0);
                mWalkingRouteOverlay.setData(line);// 设置数据
                mWalkingRouteOverlay.addToMap();// 添加到地图上
                mWalkingRouteOverlay.zoomToSpan();// 自动缩放级别

                List<WalkingRouteLine.WalkingStep> allStep = line.getAllStep();
                String content = "";
                for (int i = 0; i < allStep.size(); i++) {
                    WalkingRouteLine.WalkingStep walkingStep = allStep.get(i);
                    String instructions = walkingStep.getInstructions();
                    if (i < allStep.size()-1) {
                        content += instructions + "\n";
                    } else {
                        content += instructions;
                    }
                }
                mTvContent.clearComposingText();
                mTvContent.setText(content);
                mSl.setVisibility(View.VISIBLE);
                setHeight();
            }
        }

        /**
         * 换乘路线结果回调
         *
         * @param
         */
        @Override
        public void onGetTransitRouteResult(TransitRouteResult result) {
            if (result == null
                    || SearchResult.ERRORNO.RESULT_NOT_FOUND == result.error) {
                Toast.makeText(getApplicationContext(), "未查询到结果", Toast.LENGTH_SHORT).show();
                return;
            }
            if (mMethod == GongJiao) {
                mTransitRouteOverlay = new MyTransitRouteOverlay(mBaiduMap);
                mBaiduMap.setOnMarkerClickListener(mTransitRouteOverlay);
                TransitRouteLine line = result.getRouteLines().get(0);
                mTransitRouteOverlay.setData(line);// 设置数据
                mTransitRouteOverlay.addToMap();// 添加到地图上
                mTransitRouteOverlay.zoomToSpan();// 自动缩放级别

                List<TransitRouteLine.TransitStep> allStep = line.getAllStep();
                String content = "";
                for (int i = 0; i < allStep.size(); i++) {
                    TransitRouteLine.TransitStep transitStep = allStep.get(i);
                    String instructions = transitStep.getInstructions();
                    if (i <allStep.size()-1) {
                        content += instructions + "\n";
                    } else {
                        content += instructions;
                    }

                }
                mTvContent.clearComposingText();
                mTvContent.setText(content);
                mSl.setVisibility(View.VISIBLE);
                setHeight();
            }

        }

        /**
         * 跨城公共交通路线结果回调
         *
         * @param massTransitRouteResult
         */
        @Override
        public void onGetMassTransitRouteResult(MassTransitRouteResult massTransitRouteResult) {

        }

        /**
         * 驾车路线结果回调
         *
         * @param result
         */
        @Override
        public void onGetDrivingRouteResult(DrivingRouteResult result) {
            if (result == null
                    || SearchResult.ERRORNO.RESULT_NOT_FOUND == result.error) {
                Toast.makeText(getApplicationContext(), "未查询到结果", Toast.LENGTH_SHORT).show();
                return;
            }
            if (mMethod == ZiJia) {
                mDrivingRouteOverlay = new MyDrivingRouteOverlay(mBaiduMap);
                mBaiduMap.setOnMarkerClickListener(mDrivingRouteOverlay);
                DrivingRouteLine line = result.getRouteLines().get(0);
                mDrivingRouteOverlay.setData(line);
                mDrivingRouteOverlay.addToMap();
                mDrivingRouteOverlay.zoomToSpan();
                List<DrivingRouteLine> routeLines = result.getRouteLines();
                String content = "";
                for (int i = 0; i < routeLines.size(); i++) {
                    DrivingRouteLine drivingRouteLine = routeLines.get(i);
                    List<DrivingRouteLine.DrivingStep> allStep = drivingRouteLine.getAllStep();
                    for (int j = 0; j < allStep.size(); j++) {
                        DrivingRouteLine.DrivingStep drivingStep = allStep.get(j);
                        String instructions = drivingStep.getInstructions();
                        if (j < allStep.size()-1) {
                            content += instructions + "\n";
                        } else {
                            content += instructions;
                        }
                    }
                }
                mTvContent.clearComposingText();
                mTvContent.setText(content);
                mSl.setVisibility(View.VISIBLE);
                setHeight();
            }
        }

        /**
         * 测量高度
         */
        private void setHeight() {
            final ViewGroup.LayoutParams layoutParams = mSl.getLayoutParams();
            mTvContent.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    mTvContent.getViewTreeObserver().removeOnPreDrawListener(this);
                    WindowManager windowManager = WalkingRouteActivity.this.getWindowManager();
                    int measuredHeight = mTvContent.getMeasuredHeight();
                    if(measuredHeight>=windowManager.getDefaultDisplay().getHeight()/2){
                        if(measuredHeight/2>=windowManager.getDefaultDisplay().getHeight()/2){
                            layoutParams.height=windowManager.getDefaultDisplay().getHeight()/2;
                        }else{
                            layoutParams.height= measuredHeight/2;
                        }
                    }else{
                        layoutParams.height=measuredHeight;
                    }
                    layoutParams.width=ViewGroup.LayoutParams.MATCH_PARENT;
                    mSl.setLayoutParams(layoutParams);
                    return true;
                }
            });

        }

        /**
         * 室内路线规划回调
         *
         * @param indoorRouteResult
         */
        @Override
        public void onGetIndoorRouteResult(IndoorRouteResult indoorRouteResult) {

        }

        /**
         * 骑行路线结果回调
         *
         * @param bikingRouteResult
         */
        @Override
        public void onGetBikingRouteResult(BikingRouteResult bikingRouteResult) {

        }
    }

    class MyLocationListenner implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(100).latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            mBaiduMap.setMyLocationData(locData);
            //            mLatLng = new LatLng(location.getLatitude(), location.getLongitude());
            if (isFirstLoc) {
                isFirstLoc = false;
                MapStatus.Builder builder = new MapStatus.Builder();
                builder.target(mLatLng).zoom(18.0f);
                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
            }
        }
    }

    class MyWalkingRouteOverlay extends WalkingRouteOverlay {

        public MyWalkingRouteOverlay(BaiduMap baiduMap) {
            super(baiduMap);
        }

        @Override
        public BitmapDescriptor getStartMarker() {
            return BitmapDescriptorFactory.fromResource(R.drawable.icon_st);
        }

        @Override
        public BitmapDescriptor getTerminalMarker() {
            return BitmapDescriptorFactory.fromResource(R.drawable.icon_en);
        }
    }

    private class MyTransitRouteOverlay extends TransitRouteOverlay {
        public MyTransitRouteOverlay(BaiduMap baiduMap) {
            super(baiduMap);
        }

        @Override
        public BitmapDescriptor getStartMarker() {
            return BitmapDescriptorFactory.fromResource(R.drawable.icon_st);
        }

        @Override
        public BitmapDescriptor getTerminalMarker() {
            return BitmapDescriptorFactory.fromResource(R.drawable.icon_en);
        }

    }

    class MyDrivingRouteOverlay extends DrivingRouteOverlay {

        public MyDrivingRouteOverlay(BaiduMap arg0) {
            super(arg0);
        }

        @Override
        public BitmapDescriptor getStartMarker() {
            return BitmapDescriptorFactory.fromResource(R.drawable.icon_st);//super.getStartMarker();
        }

        @Override
        public BitmapDescriptor getTerminalMarker() {
            return BitmapDescriptorFactory.fromResource(R.drawable.icon_en);//super.getTerminalMarker();
        }

    }
}
