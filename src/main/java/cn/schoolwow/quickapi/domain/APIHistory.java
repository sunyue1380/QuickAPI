package cn.schoolwow.quickapi.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**API更新历史*/
public class APIHistory {
    /**更新时间*/
    public Date updateTime = new Date();
    /**新增接口*/
    public List<String> addList = new ArrayList<>();
    /**变更接口*/
    public List<String> modifyList = new ArrayList<>();
    /**删除接口*/
    public List<API> deleteList = new ArrayList<>();
}
