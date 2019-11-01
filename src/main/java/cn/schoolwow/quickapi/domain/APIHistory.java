package cn.schoolwow.quickapi.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**API更新历史*/
public class APIHistory {
    /**更新时间*/
    public Date updateTime = new Date();
    /**更新日志*/
    public List<String> contentList = new ArrayList<>();
}
