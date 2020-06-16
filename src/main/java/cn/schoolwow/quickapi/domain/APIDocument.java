package cn.schoolwow.quickapi.domain;

import java.util.*;

public class APIDocument {
    /**文档标题*/
    public String title = "QuickAPI";
    /**文档描述*/
    public String description;
    /**生成时间*/
    public Date date = new Date();
    /**路径前缀*/
    public String prefix = "";
    /**控制器*/
    public List<APIController> apiControllerList;
    /**实体类*/
    public Map<String,APIEntity> apiEntityMap = new HashMap<>();
    /**更新历史记录*/
    public List<APIHistory> apiHistoryList = new ArrayList<>();
}
