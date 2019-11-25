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
