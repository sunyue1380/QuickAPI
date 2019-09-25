package cn.schoolwow.quickapi.domain;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class APIDocument {
    /**文档标题*/
    public String title = "QuickAPI";
    /**文档标题*/
    public String description = "";
    /**生成时间*/
    public Date date;
    /**控制器*/
    public List<APIController> apiControllerList;
    /**实体类*/
    public Map<String,APIEntity> apiEntityMap;
}
