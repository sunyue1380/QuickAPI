package cn.schoolwow.quickapi.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @tag 后台
 * */
@RequestMapping("/admin")
@Controller
public class AdminController {
    /**
     * @brief 首页
     * */
    @GetMapping("/index")
    @ResponseBody
    public String register(
    ){
        return "hello";
    }
}
