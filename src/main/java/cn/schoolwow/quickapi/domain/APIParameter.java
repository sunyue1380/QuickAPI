package cn.schoolwow.quickapi.domain;

import java.lang.reflect.Parameter;

/**请求参数*/
public class APIParameter {
    /**参数名*/
    private String name;
    /**参数类型*/
    public String type;
    /**参数实体类型*/
    public String entityType;
    /**参数请求类型*/
    public String requestType = "text";
    /**参数位置(query,body)*/
    public String position = "body";
    /**描述*/
    private String description;
    /**是否必须*/
    public boolean required = true;
    /**默认值*/
    public String defaultValue = "";
    /**参数*/
    public transient Parameter parameter;

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

        APIParameter that = (APIParameter) o;

        return name != null ? name.equals(that.name) : that.name == null;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
