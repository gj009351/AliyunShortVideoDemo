package com.aliyun.demo.msg.body;

import java.util.List;

/**
 * Created by duke on 2018/8/20.
 */

public class LrcListModel {

    private List<LrcModel> lrcModels;

    public LrcListModel(List<LrcModel> lrcModels) {
        this.lrcModels = lrcModels;
    }

    public List<LrcModel> getLrcModels() {
        return lrcModels;
    }
}
