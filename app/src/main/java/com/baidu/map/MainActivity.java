package com.baidu.map;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.map.adapter.SearchAddressAdapter;
import com.baidu.map.weigit.ClearEditText;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MapViewLayoutParams;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiNearbySearchOption;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;

import java.util.ArrayList;
import java.util.List;

import static com.baidu.map.R.layout.pop;

public class MainActivity extends AppCompatActivity implements OnGetPoiSearchResultListener {

    private MapView mMapView;
    private BaiduMap mBaiduMap;
    private boolean isJiaoTong = false;//时候开启交通地图
    boolean isFirstLoc = true; // 是否首次定位
    private LocationClient mLocClient;// 定位相关
    private MyLocationListenner myListener = new MyLocationListenner();
    private ClearEditText mEt_jiansuo;
    private PoiSearch mPoiSearch;
    private String mCity;
    private double mLatitude;
    private double mLongitude;
    private ListView mListView;
    private ArrayList<PoiInfo> mPoiInfos = new ArrayList<>();
    private Context mContext;
    private SearchAddressAdapter mSearchAddressAdapter;
    private View mPop;
    private TextView mTitle;
    private LatLng mLatLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        mMapView = (MapView) findViewById(R.id.map);
        mEt_jiansuo = (ClearEditText) findViewById(R.id.et_jiansuo);
        mListView = (ListView) findViewById(R.id.listView);
        mBaiduMap = mMapView.getMap();
        initLocation();
        initJianSuo();
        initPop();
        findViewById(R.id.tv_putong).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //普通地图
                mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
            }
        });
        findViewById(R.id.tv_weixin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //卫星地图
                mBaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
            }
        });
        findViewById(R.id.tv_kongbai).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //空白地图,基础地图瓦片将不会被渲染。在地图类型中设置为NONE，将不会使用流量下载基础地图瓦片图层。使用场景：与瓦片图层一起使用，节省流量，提升自定义瓦片图下载速度。
                mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NONE);
            }
        });
        findViewById(R.id.tv_jiaotong).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //交通地图
                if (isJiaoTong == false) {
                    mBaiduMap.setTrafficEnabled(true);
                    isJiaoTong = true;
                } else {
                    mBaiduMap.setTrafficEnabled(false);
                    isJiaoTong = false;
                }
            }
        });
        //定位自己的位置
        findViewById(R.id.iv_loction).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "正在定位中...", Toast.LENGTH_SHORT).show();
                isFirstLoc = true;
            }
        });
    }

    private void initPop() {
        mPop = View.inflate(mContext, pop, null);
        ViewGroup.LayoutParams params = new MapViewLayoutParams.Builder()
                .layoutMode(MapViewLayoutParams.ELayoutMode.mapMode)// 设置为经纬度模式
                .position(new LatLng(mLatitude, mLongitude))// 设置位置 不能null
                .width(MapViewLayoutParams.WRAP_CONTENT)
                .height(MapViewLayoutParams.WRAP_CONTENT)
                .build();
        mMapView.addView(mPop, params);
        mPop.setVisibility(View.GONE);
        mTitle = (TextView) mPop.findViewById(R.id.title);
    }

    private void initJianSuo() {
        //第一步，创建POI检索实例
        mPoiSearch = PoiSearch.newInstance();

        //第三步，设置POI检索监听者；
        mPoiSearch.setOnGetPoiSearchResultListener(this);

        mEt_jiansuo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals("")) {
                    mListView.setVisibility(View.VISIBLE);
                    PoiNearbySearchOption ndear = new PoiNearbySearchOption();
                    ndear.location(new LatLng(mLatitude, mLongitude)).keyword(s.toString()).radius
                            (1000 * 10).pageCapacity(10).pageNum(3);
                    mPoiSearch.searchNearby(ndear);
                } else {
                    mListView.setVisibility(View.GONE);
                }
            }
        });

        if (mSearchAddressAdapter == null) {
            mSearchAddressAdapter = new SearchAddressAdapter(mContext, mPoiInfos, mBaiduMap);
            mListView.setAdapter(mSearchAddressAdapter);
        } else {
            mSearchAddressAdapter.notifyDataSetChanged();
        }

        mSearchAddressAdapter.setOnItemListener(new SearchAddressAdapter.OnItemListener() {
            @Override
            public void onItemListener(PoiInfo poiInfo) {
                mTitle.setText(poiInfo.name);
                ViewGroup.LayoutParams params = new MapViewLayoutParams.Builder()
                        .layoutMode(MapViewLayoutParams.ELayoutMode.mapMode)// 设置为经纬度模式
                        .position(poiInfo.location)// 设置位置 不能null
                        .width(MapViewLayoutParams.WRAP_CONTENT)
                        .height(MapViewLayoutParams.WRAP_CONTENT)
                        .yOffset(-5)// 设置y轴偏移量 向上是负数 向下是正数
                        .build();
                mMapView.updateViewLayout(mPop, params);
                mPop.setVisibility(View.GONE);

            }
        });

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PoiInfo poiInfo = mPoiInfos.get(position);
                startActivity(new Intent(mContext,WalkingRouteActivity.class).putExtra("poiInfo",
                        poiInfo).putExtra("mLatLng",mLatLng));
            }
        });

    }


    private void initLocation() {
        // 开启定位图层
        mBaiduMap.setMyLocationEnabled(true);
        // 定位初始化
        mLocClient = new LocationClient(this);
        mLocClient.registerLocationListener(myListener);
        //缩放级别15
        // 设置地图缩放级别为15
        mBaiduMap.setMapStatus(MapStatusUpdateFactory
                .newMapStatus(new MapStatus.Builder().zoom(15).build()));

        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true); // 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setIsNeedAddress(true);//可选，设置是否需要地址信息，默认不需要
        option.setScanSpan(1000);//扫描间隔 单位毫秒
        option.setLocationMode(LocationClientOption.LocationMode.Battery_Saving);
        mLocClient.setLocOption(option);
        //设置箭头
        mBaiduMap.setMyLocationConfigeration(new MyLocationConfiguration(
                MyLocationConfiguration.LocationMode.NORMAL, true, null));
        mLocClient.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mMapView.onDestroy();
        mPoiSearch.destroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause();
    }

    @Override
    public void onGetPoiResult(PoiResult poiResult) {
        List<PoiInfo> allPoi = poiResult.getAllPoi();
        if (allPoi != null && !allPoi.isEmpty()) {
            mPoiInfos.clear();
            mPoiInfos.addAll(allPoi);
            mSearchAddressAdapter.notifyDataSetChanged();
        }
    }


    @Override
    public void onGetPoiDetailResult(PoiDetailResult poiDetailResult) {
    }

    @Override
    public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {
    }

    /**
     * 定位SDK监听函数
     */
    public class MyLocationListenner implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            // map view 销毁后不在处理新接收的位置
            if (location == null || mMapView == null) {
                return;
            }
            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(100).latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            mBaiduMap.setMyLocationData(locData);
            mCity = location.getCity();
            mLatLng=new LatLng(location.getLatitude(),location.getLongitude());
            if (isFirstLoc) {
                isFirstLoc = false;
                mLatitude = location.getLatitude();
                mLongitude = location.getLongitude();
                LatLng ll = new LatLng(mLatitude,
                        mLongitude);
                MapStatus.Builder builder = new MapStatus.Builder();
                builder.target(ll).zoom(18.0f);
                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
            }
        }


    }
}
