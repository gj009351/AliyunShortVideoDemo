/*
 * Copyright (C) 2010-2017 Alibaba Group Holding Limited.
 */

package com.aliyun.alivcsolution;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

import com.aliyun.common.crash.CrashHandler;
import com.aliyun.demo.recorder.faceunity.FaceUnityManager;
import com.aliyun.downloader.DownloaderManager;
import com.aliyun.sys.AlivcSdkCore;
import com.aliyun.video.common.aliha.AliHaUtils;
import com.squareup.leakcanary.AndroidExcludedRefs;
import com.squareup.leakcanary.DisplayLeakService;
import com.squareup.leakcanary.ExcludedRefs;
import com.squareup.leakcanary.LeakCanary;

/**
 * Created by Mulberry on 2018/2/24.
 */
public class MutiApplication extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        FaceUnityManager.getInstance().setUp(this);
        com.aliyun.vod.common.httpfinal.QupaiHttpFinal.getInstance().initOkHttpFinal();
        //Logger.setDebug(true);
        initDownLoader();
        initLeakCanary();
        //初始化阿⾥云移动高可⽤SDK接⼊——崩溃分析
        AliHaUtils.initHa(this,null);
        //        localCrashHandler();
        //        new NativeCrashHandler().registerForNativeCrash(this);
        AlivcSdkCore.register(getApplicationContext());
        AlivcSdkCore.setLogLevel(AlivcSdkCore.AlivcLogLevel.AlivcLogDebug);
    }

    private void initDownLoader() {
        DownloaderManager.getInstance().init(this);
    }

    private void localCrashHandler() {
        CrashHandler catchHandler = CrashHandler.getInstance();
        catchHandler.init(getApplicationContext());
    }

    private void initLeakCanary() {
        //排除一些Android Sdk引起的泄漏
        ExcludedRefs excludedRefs = AndroidExcludedRefs.createAppDefaults()
            .instanceField("android.view.inputmethod.InputMethodManager", "sInstance")
            .instanceField("android.view.inputmethod.InputMethodManager", "mLastSrvView")
            .instanceField("com.android.internal.policy.PhoneWindow$DecorView", "mContext")
            .instanceField("android.support.v7.widget.SearchView$SearchAutoComplete", "mContext")
            .instanceField("android.app.ActivityThread$ActivityClientRecord", "activity")
            .instanceField("android.media.MediaScannerConnection", "mContext")
            .build();

        LeakCanary.refWatcher(this)
            .listenerServiceClass(DisplayLeakService.class)
            .excludedRefs(excludedRefs)
            .buildAndInstall();
    }
}
