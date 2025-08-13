package duckhu.calendar.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Vue.js SPA 라우팅을 지원하는 컨트롤러
 * /admin, /admin-login 등의 프론트엔드 라우트를 처리
 */
@Controller
public class SpaController {

    /**
     * Vue.js 라우터가 처리해야 하는 경로들을 Spring Boot에서 index.html로 포워딩
     */
    @RequestMapping(value = {"/admin", "/admin-login", "/settings"})
    public String spa() {
        return "forward:/index.html";
    }
}
