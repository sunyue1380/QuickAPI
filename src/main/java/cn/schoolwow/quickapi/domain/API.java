package cn.schoolwow.quickapi.domain;

import java.util.Arrays;

public class API {
    /**是否被废弃*/
    public boolean deprecated;
    /**请求方法*/
    public String[] methods;
    /**请求地址*/
    public String url;
    /**名称*/
    public String name;
    /**描述*/
    public String description;
    /**方法名*/
    public String methodName;
    /**请求编码*/
    public String contentType = "application/x-www-form-urlencoded";
    /**请求参数*/
    public APIParameter[] apiParameters = new APIParameter[0];
    /**返回值*/
    public String returnValue;
    /**请求参数实体类信息*/
    public String[] parameterEntityNameList = new String[0];
    /**返回实体类信息*/
    public String[] returnEntityNameList = new String[0];
    /**抛出异常*/
    public APIException[] apiExceptions = new APIException[0];

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
