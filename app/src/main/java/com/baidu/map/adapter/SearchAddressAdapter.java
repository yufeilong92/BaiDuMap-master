package com.baidu.map.adapter;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.baidu.map.R;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.search.core.PoiInfo;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Administrator on 2016/10/25.
 */

public class SearchAddressAdapter extends BaseAdapter {
    private Context mContext;
    private ArrayList<PoiInfo> mPoiInfos;
    private BaiduMap mBaiduMap;

    public SearchAddressAdapter(Context context, ArrayList<PoiInfo> poiInfos, BaiduMap baiduMap) {
        this.mContext = context;
        this.mPoiInfos = poiInfos;
        this.mBaiduMap = baiduMap;
    }

    @Override
    public int getCount() {
        return mPoiInfos.size();
    }

    @Override
    public Object getItem(int position) {
        return mPoiInfos.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = View.inflate(mContext, R.layout.address, null);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        PoiInfo poiInfo = mPoiInfos.get(position);
        viewHolder.mItemAddressNameTv.setText(poiInfo.name);
        viewHolder.mItemAddressDetailTv.setText(poiInfo.address);

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(poiInfo.location).animateType(MarkerOptions.MarkerAnimateType.drop)
                .anchor(0.5f, 1.0f).title
                (poiInfo.name).zIndex(5).icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory
                .decodeResource(mContext.getResources(), R.drawable.icon_geo)));

        mBaiduMap.addOverlay(markerOptions);
        showPop(poiInfo);
        return convertView;
    }
    private OnItemListener mOnItemListener;

    public void setOnItemListener(OnItemListener listener) {
        this.mOnItemListener = listener;
    }

    public interface OnItemListener {
        void onItemListener(PoiInfo poiInfo);
    }

    private void showPop(PoiInfo poiInfo) {
        mOnItemListener.onItemListener(poiInfo);
    }

    static class ViewHolder {
        @Bind(R.id.item_address_name_tv)
        TextView mItemAddressNameTv;
        @Bind(R.id.item_address_detail_tv)
        TextView mItemAddressDetailTv;

        ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}
