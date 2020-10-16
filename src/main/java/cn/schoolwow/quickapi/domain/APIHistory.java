package cn.schoolwow.quickapi.domain;

import java.util.*;

/**API更新历史*/
public class APIHistory {
    /**更新时间*/
    public Date updateTime = new Date();
    /**新增接口*/
    public Set<String> addList = new HashSet<>();
    /**变更接口*/
    public Set<String> modifyList = new HashSet<>();
    /**删除接口*/
    public Set<API> deleteList = new HashSet<>();
}
