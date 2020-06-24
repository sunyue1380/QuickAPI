package cn.schoolwow.quickapi.domain;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class API {
    /**是否被废弃*/
    public boolean deprecated;
    /**请求方法*/
    public String[] methods;
    /**请求地址*/
    public String url;
    /**名称*/
    private String name = "";
    /**描述*/
    private String description = "";
    /**作者*/
    public String author;
    /**日期*/
    public String since;
    /**请求编码*/
    public String contentType = "application/x-www-form-urlencoded";
    /**请求参数*/
    public List<APIParameter> apiParameters = new ArrayList<APIParameter>();
    /**返回值*/
    public String returnValue;
    /**请求参数实体类信息*/
    public List<String> parameterEntityNameList = new ArrayList<>();
    /**返回实体类信息*/
    public String[] returnEntityNameList = new String[0];
    /**抛出异常*/
    public APIException[] apiExceptions = new APIException[0];
    /**方法*/
    public transient Method method;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if(null==this.name||this.name.isEmpty()){
            this.name = name;
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        if(null==this.description||this.description.isEmpty()){
            this.description = description;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        API api = (API) o;

        if (!Arrays.equals(methods, api.methods)) return false;
        return url != null ? url.equals(api.url) : api.url == null;
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(methods);
        result = 31 * result + (url != null ? url.hashCode() : 0);
        return result;
    }
}
