package com.example.tzchannel.suspensionfab;

import android.support.design.widget.FloatingActionButton;

/*
 * 描述:     TODO 按钮的点击事件
 */
public interface OnFabClickListener {

    /**
     * @param fab 按钮
     * @param tag 回调之前设置的tag
     */
    void onFabClick(FloatingActionButton fab, Object tag);
}

