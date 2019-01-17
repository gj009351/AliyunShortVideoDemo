package com.aliyun.demo.util;

import android.text.TextUtils;

import com.aliyun.demo.msg.body.LrcModel;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LrcReader {
    private static LrcReader instance = null;
    private List<LrcModel> mLrcs;

    /**
     * 得到对象实例
     */
    public static LrcReader getInstance() {
        if (instance == null)
            instance = new LrcReader();
        return instance;
    }

    /**
     * 获取LRC文件流
     *
     * @param iStream
     * @throws IOException
     */
    public List<LrcModel> getLrc(InputStream iStream) throws IOException {
        InputStreamReader iStreamReader = new InputStreamReader(iStream);
        BufferedReader reader = new BufferedReader(iStreamReader);
        mLrcs = new ArrayList<>();
        String line = null;
        while ((line = reader.readLine()) != null) {
            parseLine(line);
        }
        return mLrcs;
    }

    /**
     * @throws IOException
     */
    public List<LrcModel> getLrc(String filePath) throws IOException {
        FileInputStream inputStream = new FileInputStream(filePath);
        InputStreamReader iStreamReader = new InputStreamReader(inputStream);
        BufferedReader reader = new BufferedReader(iStreamReader);
        mLrcs = new ArrayList<>();
        String line = null;
        while ((line = reader.readLine()) != null) {
            parseLine(line);
        }
        return mLrcs;
    }

    /**
     * @throws IOException
     */
    public String getLrcString(String filePath) throws IOException {
        FileInputStream inputStream = new FileInputStream(filePath);
        InputStreamReader iStreamReader = new InputStreamReader(inputStream);
        BufferedReader reader = new BufferedReader(iStreamReader);
        mLrcs = new ArrayList<>();
        String line = null;
        StringBuilder sb = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            parseLine(line);
            sb.append(line);
            sb.append("\r\n");
        }
        if (mLrcs.isEmpty()) {
            return sb.toString();
        }
//        LogUtil.e("filepath:" + filePath + ", mlrcs:" + mLrcs);
        return modelsToString(mLrcs);
    }

    public String modelsToString(List<LrcModel> lrcModels) {
        StringBuilder sb = new StringBuilder();
        if (lrcModels != null && lrcModels.size() > 0) {
            for (int i = 0; i < lrcModels.size(); i++) {
                LrcModel lrcModel = lrcModels.get(i);
                sb.append(lrcModel.lrc);
                sb.append("\r\n");
            }
        }
//        LogUtil.e("lrcstring:" + sb.toString());
        return sb.toString();
    }

    /**
     * 逐行解析，将结果存入自定义的LrcModel中
     *
     * @param line
     */
    public void parseLine(String line) {
        String reg = "\\[([0-9]+:[0-9]+.[0-9]+)\\]";
        Pattern pattern = Pattern.compile(reg);
        Matcher matcher = pattern.matcher(line);
        while (matcher.find()) {
            String time = matcher.group();
            if (!TextUtils.isEmpty(time)) {
                LrcModel lrcModel = new LrcModel();
                lrcModel.leftTime = parseTime(time);
                lrcModel.lrc = line.substring(line.indexOf(time) + time.length());
                mLrcs.add(lrcModel);
            }
        }
    }

    /**
     * 解析时间，转换为毫秒格式
     *
     * @param time
     * @return
     */
    public Integer parseTime(String time) {
        String temp = time.substring(1, time.length() - 1);
        String[] s = temp.split(":");
        int min = Integer.parseInt(s[0]);
        String[] ss = s[1].split("\\.");
        int sec = Integer.parseInt(ss[0]);
        int mill = Integer.parseInt(ss[1]);
        return min * 60 * 1000 + sec * 1000 + mill * 10;
    }

}