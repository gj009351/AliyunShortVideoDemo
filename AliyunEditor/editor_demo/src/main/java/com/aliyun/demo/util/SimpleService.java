package com.aliyun.demo.util;

import android.content.Context;

import com.aliyun.demo.editor.R;
import com.aliyun.demo.msg.body.LrcListModel;
import com.aliyun.video.common.utils.ThreadUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by duke on 2018/8/2.
 */

public class SimpleService {


    public static void getLrcFromRaw(final Context context, final OnServiceListener<LrcListModel> listener) {
        ThreadUtils.runOnSubThread(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream inputStream = context.getResources().openRawResource(R.raw.lrc_demo);
                    final LrcListModel model = new LrcListModel(LrcReader.getInstance().getLrc(inputStream));
                    ThreadUtils.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (listener != null) {
                                listener.onSuccess(model);
                            }
                        }
                    });

                } catch (final IOException e) {
                    e.printStackTrace();
                    ThreadUtils.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (listener != null) {
                                listener.onError(e);
                            }
                        }
                    });
                }
            }
        });
    }

    public interface OnServiceListener<T> {
        void onError(Throwable e);

        void onSuccess(T t);
    }

    public interface OnServiceProgressListener<T> {
        void onError(Throwable e);

        void onProgress(int progress);

        void onSuccess(T t);
    }

}
