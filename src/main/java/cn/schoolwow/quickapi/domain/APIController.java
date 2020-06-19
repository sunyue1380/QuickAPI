package cn.schoolwow.quickapi.domain;

import java.util.ArrayList;
import java.util.List;

public class APIController {
    /**是否被废弃*/
    public boolean deprecated;
    /**控制器*/
    private String name;
    /**控制器类名*/
    public String className;
    /**接口*/
    public List<API> apiList = new ArrayList<>();
    /**控制器类*/
    public transient Class clazz;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if(null==this.name||this.name.isEmpty()){
            this.name = name;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        APIController that = (APIController) o;

        return className != null ? className.equals(that.className) : that.className == null;
    }

    @Override
    public int hashCode() {
        return className != null ? className.hashCode() : 0;
    }
}
