package cn.schoolwow.quickapi.controller.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

/**
 * @apiNote 用户服务
 * */
@RestController
@RequestMapping("/user")
public class UserController {
    private Logger logger = LoggerFactory.getLogger(UserController.class);
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
}
