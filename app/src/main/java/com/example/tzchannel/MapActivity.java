package com.example.tzchannel;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class MapActivity extends AppCompatActivity implements View.OnClickListener, SensorEventListener
{
    private static final String Channle_Map_Title = "渠道信息管理";
    private MapView mMapView;
    private TextView mMapTitle;
    private BaiduMap mBaiduMap;
    private BDLocation mlocation;
    private LocationClient mLocationClient;  //定位监听
    private MyLocationListener mMyLocationListener;
    //private TextView positionText;
    private boolean isFirstLocate = true;
    private LocationClientOption mlocationOption;  //定位属性
    private MyLocationData locData;//定位坐标
    private InfoWindow mInfoWindow;//地图文字位置提醒
    private double mCurrentLat = 0.0;
    private double mCurrentLon = 0.0;
    private int mCurrentDirection = 0;
    private LatLng mDestinationPoint;//目的地坐标点
    private LatLng mCurrentPoint;//当前坐标点
    private Double lastX = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_map);
        mMapView = (MapView) findViewById(R.id.mapView);
        mBaiduMap = mMapView.getMap();
        mBaiduMap.setMyLocationEnabled(true);
        setTitle(Channle_Map_Title);

        //API版本高于23
        if(Build.VERSION.SDK_INT> Build.VERSION_CODES.LOLLIPOP)
        {
            List<String> permissionList = new ArrayList<>();
            if (ContextCompat.checkSelfPermission(MapActivity.this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.READ_PHONE_STATE);
            }//获取手机状态
            if (ContextCompat.checkSelfPermission(MapActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }//获取位置信息
            if (ContextCompat.checkSelfPermission(MapActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }//获取位置信息
            if (ContextCompat.checkSelfPermission(MapActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }//读写SD卡
            if (ContextCompat.checkSelfPermission(MapActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }//读写SD卡
            if (!permissionList.isEmpty()) {
                String[] permissions = permissionList.toArray(new String[permissionList.size()]);
                ActivityCompat.requestPermissions(MapActivity.this, permissions, 1);
            }
            else
                requestLocation();
        }
    }

    @Override
    public void onClick(View v){}

    private void requestLocation()
    {
        initLocationOption();
        //后续添加操作
    }

    /**
     * 设置页面标题
     *
     * @param title 标题文字
     */
    protected void setTitle(String title)
    {
        if (!TextUtils.isEmpty(title))
        {
            mMapTitle= (TextView) findViewById(R.id.MapTitle);
            mMapTitle.setText(title);
        }
    }



    /**
     * 初始化定位参数配置
     * @return
     */
    private void initLocationOption()
    {
        //定位服务的客户端。宿主程序在客户端声明此类，并调用，目前只支持在主线程中启动
        mLocationClient = new LocationClient(getApplicationContext());
        //声明LocationClient类实例并配置定位参数
        mlocationOption = new LocationClientOption();
        mMyLocationListener = new MyLocationListener();
        //注册监听函数
        mLocationClient.registerLocationListener(mMyLocationListener);
        //可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        mlocationOption.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        //可选，默认gcj02，设置返回的定位结果坐标系，如果配合百度地图使用，建议设置为bd09ll;
        mlocationOption.setCoorType("bd09ll");
        //mlocationOption.setCoorType("gcj02");
        //可选，默认0，即仅定位一次，设置发起连续定位请求的间隔需要大于等于1000ms才是有效的
        mlocationOption.setScanSpan(1000);
        //可选，设置是否需要地址信息，默认不需要
        mlocationOption.setIsNeedAddress(true);
        //可选，设置是否需要地址描述
        mlocationOption.setIsNeedLocationDescribe(true);
        //可选，设置是否需要设备方向结果
        mlocationOption.setNeedDeviceDirect(false);
        //可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
        mlocationOption.setLocationNotify(true);
        //可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
        mlocationOption.setIgnoreKillProcess(true);
        //可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
        mlocationOption.setIsNeedLocationDescribe(true);
        //可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
        mlocationOption.setIsNeedLocationPoiList(true);
        //可选，默认false，设置是否收集CRASH信息，默认收集
        mlocationOption.SetIgnoreCacheException(false);
        //可选，默认false，设置是否开启Gps定位
        mlocationOption.setOpenGps(true);
        //可选，默认false，设置定位时是否需要海拔信息，默认不需要，除基础定位版本都可用
        mlocationOption.setIsNeedAltitude(false);
        //设置打开自动回调位置模式，该开关打开后，期间只要定位SDK检测到位置变化就会主动回调给开发者，该模式下开发者无需再关心定位间隔是多少，定位SDK本身发现位置变化就会及时回调给开发者
        mlocationOption.setOpenAutoNotifyMode();
        //设置打开自动回调位置模式，该开关打开后，期间只要定位SDK检测到位置变化就会主动回调给开发者
        mlocationOption.setOpenAutoNotifyMode(3000,1, LocationClientOption.LOC_SENSITIVITY_HIGHT);
        //需将配置好的LocationClientOption对象，通过setLocOption方法传递给LocationClient对象使用
        mLocationClient.setLocOption(mlocationOption);
        //开始定位
        mLocationClient.start();
    }

    /**
     * 处理连续定位的地图UI变化
     */
    private Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            super.handleMessage(msg);
            BDLocation location = (BDLocation) msg.obj;
            LatLng LocationPoint = new LatLng(location.getLatitude(), location.getLongitude());

            //设当前位置图标
            setMarkerOptions(LocationPoint,R.mipmap.yd_marker);

            //打卡范围
//            mDestinationPoint = new LatLng(location.getLatitude() * 1.0001, location.getLongitude() * 1.0001);//假设公司坐标
//            setCircleOptions();
//            //计算两点距离,单位：米
//            mDistance = DistanceUtil.getDistance(mDestinationPoint, LocationPoint);
//            if (mDistance <= DISTANCE) {
//                //显示文字
//                setTextOption(mDestinationPoint, "您已在餐厅范围内", "#7ED321");
//                //目的地图标
//                setMarkerOptions(mDestinationPoint, R.mipmap.arrive_icon);
//                //按钮颜色
//                commit_bt.setBackgroundDrawable(getResources().getDrawable(R.mipmap.restaurant_btbg_yellow));
//                mBaiduMap.setMyLocationEnabled(false);
//            } else {
//                setTextOption(LocationPoint, "您不在餐厅范围之内", "#FF6C6C");
//                setMarkerOptions(mDestinationPoint, R.mipmap.restaurant_icon);
//                commit_bt.setBackgroundDrawable(getResources().getDrawable(R.mipmap.restaurant_btbg_gray));
//                mBaiduMap.setMyLocationEnabled(true);
//            }
//            mDistance_tv.setText("距离目的地：" + mDistance + "米");
//            //缩放地图
//            setMapZoomScale(LocationPoint);
        }
    };

    /***
     * 接收定位结果消息，并显示在地图上
     */
//    private BDAbstractLocationListener BDAblistener = new BDAbstractLocationListener()
//    {
//        @Override
//        public void onReceiveLocation(BDLocation location)
//        {
//            //定位方向
//            mCurrentLat = location.getLatitude();
//            mCurrentLon = location.getLongitude();
//            //骑手定位
//            locData = new MyLocationData.Builder()
//                    .direction(mCurrentDirection).latitude(location.getLatitude())
//                    .longitude(location.getLongitude()).build();
//            mBaiduMap.setMyLocationData(locData);
//            mBaiduMap.setMyLocationConfiguration(new MyLocationConfiguration(
//                    MyLocationConfiguration.LocationMode.NORMAL, true, null));
//
//            //更改UI
//            Message message = new Message();
//            message.obj = location;
//            mHandler.sendMessage(message);
//
//        }
//    };


    /**
     * 实现定位回调
     */

    public class MyLocationListener extends BDAbstractLocationListener
    {
        @Override
        public void onReceiveLocation(BDLocation location)
        {
            //此处的BDLocation为定位结果信息类，通过它的各种get方法可获取定位相关的全部结果
            //以下只列举部分获取经纬度相关（常用）的结果信息
            //更多结果信息获取说明，请参照类参考中BDLocation类中的说明

            //获取纬度信息
            double latitude = location.getLatitude();
            //获取经度信息
            double longitude = location.getLongitude();
            //获取定位精度，默认值为0.0f
            float radius = location.getRadius();
            //获取经纬度坐标类型，以LocationClientOption中设置过的坐标类型为准
            String coorType = location.getCoorType();
            //获取定位类型、定位错误返回码，具体信息可参照类参考中BDLocation类中的说明
            int errorCode = location.getLocType();

            //获取经纬度坐标类型，以LocationClientOption中设置过的坐标类型为准
            //获取定位类型、定位错误返回码，具体信息可参照类参考中BDLocation类中的说明
            if (location.getLocType() == BDLocation.TypeGpsLocation || location.getLocType() == BDLocation.TypeNetWorkLocation)
                navigateTo(location);
            mlocation=location;

        }
    }

    //navigateTo()函数用来更新地图，location为监听到的定位，调用navigateTo(mlocation)即可使地图定位中心显示在mlocation。
    private void navigateTo(BDLocation location)
    {
        if (location == null)
            return;
        else if (isFirstLocate) {
            LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
            MapStatusUpdate update = MapStatusUpdateFactory.newLatLng(ll);
            mBaiduMap.animateMapStatus(update);
            //zoom设置缩放等级，值越大，地点越详细
            MapStatus mMapSta0tus = new MapStatus.Builder()
                    .target(ll)
                    .zoom(19)
                    .build();
            //定义MapStatusUpdate对象，以便描述地图状态将要发生的变化
            MapStatusUpdate mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapSta0tus);
            //改变地图状态
            mBaiduMap.animateMapStatus(mMapStatusUpdate);
            isFirstLocate = false;
        }
        MyLocationData locationData = new MyLocationData.Builder().accuracy(20)//locData.accuracy = location.getRadius();//获取默认误差半径
                //accuracy设置精度圈大小
                //设置开发者获取到的方向信息，顺时针旋转0-360度
                .direction(100).latitude(location.getLatitude()).longitude(location.getLongitude()).build();
//        MyLocationData.Builder locationBuilder = new MyLocationData.Builder();
//        locationBuilder.latitude(location.getLatitude());
//        locationBuilder.longitude(location.getLongitude());
//        MyLocationData locationData = locationBuilder.build();
        mBaiduMap.setMyLocationData(locationData);

        //更改UI
        Message message = new Message();
        message.obj = location;
        mHandler.sendMessage(message);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocationClient.stop();
        mMapView.onDestroy();
        mBaiduMap.setMyLocationEnabled(false);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0) {
                    for (int result : grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(this, "必须同意所有权限才能使用本程序", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                    }
                    requestLocation();
                } else {
                    Toast.makeText(this, "发生未知错误", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        double x = sensorEvent.values[SensorManager.DATA_X];
        if (Math.abs(x - lastX) > 1.0) {
            mCurrentDirection = (int) x;
            locData = new MyLocationData.Builder()
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(mCurrentDirection).latitude(mCurrentLat)
                    .longitude(mCurrentLon).build();
            mBaiduMap.setMyLocationData(locData);
        }
        lastX = x;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}

    /**
     * 添加地图文字
     * @param point
     * @param str
     * @param color 字体颜色
     */
    private void setTextOption(LatLng point, String str, String color)
    {
        //使用MakerInfoWindow
        if (point == null) return;
        TextView view = new TextView(getApplicationContext());
        view.setBackgroundResource(R.mipmap.map_textbg);
        view.setPadding(0, 23, 0, 0);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setTextSize(14);
        view.setGravity(Gravity.CENTER);
        view.setText(str);
        view.setTextColor(Color.parseColor(color));
        mInfoWindow = new InfoWindow(view, point, 170);
        mBaiduMap.showInfoWindow(mInfoWindow);
    }

        /**
         * 设置marker覆盖物
         * @param ll   坐标
         * @param icon 图标
         */
    private void setMarkerOptions(LatLng ll, int icon)
    {
        if (ll == null) return;
        BitmapDescriptor bitmap = BitmapDescriptorFactory.fromResource(icon);
        MarkerOptions ooD = new MarkerOptions().position(ll).icon(bitmap);
        mBaiduMap.addOverlay(ooD);
    }

}
