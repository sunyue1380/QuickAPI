package cn.schoolwow.quickapi.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

/**
 * @apiNote 系统服务
 * */
@RestController
@RequestMapping("/system")
public class SystemController {
    private Logger logger = LoggerFactory.getLogger(SystemController.class);
    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * */
    @GetMapping(value = "/login")
    public boolean login(String username,String password){
        logger.info("[登录]用户名:{},密码:{}",username,password);
        return true;
    }

    /**
     * 注册
     * @param username 用户名
     * @param password 密码
     * */
    @PostMapping(value = "/register")
    public boolean register(
            @RequestParam(name = "username") String username,
            @RequestParam(name = "password") String password
    ){
        logger.info("[注册]用户名:{},密码:{}",username,password);
        return true;
    }

    /**
     * 忘记密码
     * @param user 用户
     * */
    @PostMapping(value = "/updatePassword")
    public boolean updatePassword(
            @RequestBody User user
    ){
        logger.info("[更新密码]用户名:{},密码:{}",user.getUsername(),user.getPassword());
        return true;
    }

    /**
     * 更新用户信息
     * @param user 用户
     * */
    @PutMapping(value = "/updateUser")
    public Object updateUser(
            @RequestBody User user
    ){
        return true;
    }

    /**
     * 更新用户扩展信息
     * @param userExtend 用户扩展信息
     * */
    @PutMapping(value = "/updatePassword")
    public Object updateUserExtend(
            @RequestBody UserExtend userExtend
    ){
        return true;
    }
}
