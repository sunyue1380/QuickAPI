package cn.schoolwow.quickapi.domain;

import java.util.ArrayList;
import java.util.List;

public class APIController {
    /**是否被废弃*/
    public boolean deprecated;
    /**控制器*/
    public String name;
    /**控制器类名*/
    public String className;
    /**接口*/
    public List<API> apiList = new ArrayList<>();
}
