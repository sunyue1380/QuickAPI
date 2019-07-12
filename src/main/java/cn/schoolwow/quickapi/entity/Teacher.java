package cn.schoolwow.quickapi.entity;

/**教师表*/
public class Teacher<User> {
    /**自增id*/
    private long id;
    /**老师名字*/
    private String name;
    /**学生*/
    private User user;
}
