package cn.schoolwow.quickapi.domain;

import java.util.Date;
import java.util.List;

public class APIDocument {
    /**文档标题*/
    public String title = "QuickAPI";
    /**生成时间*/
    public Date date;
    /**控制器*/
    public List<APIController> apiControllerList;
}
