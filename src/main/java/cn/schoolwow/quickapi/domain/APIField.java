package cn.schoolwow.quickapi.domain;

import java.lang.reflect.Field;

public class APIField {
    /**字段名*/
    public String name;
    /**字段类型*/
    public String className;
    /**描述*/
    public String description;
    /**是否忽略*/
    public boolean ignore;
    /**是否必须*/
    public boolean required;
    /**样例*/
    public String example;
    /**字段*/
    public transient Field field;
}
