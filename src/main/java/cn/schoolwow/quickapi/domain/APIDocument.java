package cn.schoolwow.quickapi.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class APIDocument {
    /**文档标题*/
    public String title = "QuickAPI";
    /**文档描述*/
    public String description;
    /**生成时间*/
    public Date date;
    /**路径前缀*/
    public String prefix = "";
    /**控制器*/
    public List<APIController> apiControllerList;
    /**实体类*/
    public Map<String,APIEntity> apiEntityMap;
    /**更新历史记录*/
    public List<APIHistory> apiHistoryList = new ArrayList<>();
}
