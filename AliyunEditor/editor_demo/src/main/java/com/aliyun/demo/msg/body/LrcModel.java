package com.aliyun.demo.msg.body;

import java.io.Serializable;
import java.util.List;

/**
 * Created by duke on 2018/7/24.
 */

public class LrcModel implements Serializable{
    public String lrc = "";
    public double leftTime;

    public LrcModel(String lrc, double leftTime) {
        this.lrc = lrc;
        this.leftTime = leftTime;
    }

    public LrcModel() {
    }

    @Override
    public String toString() {
        return "LrcModel{" +
                "lrc='" + lrc + '\'' +
                ", leftTime=" + leftTime +
                '}';
    }
}
