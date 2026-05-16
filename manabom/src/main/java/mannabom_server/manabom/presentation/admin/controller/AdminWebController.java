package mannabom_server.manabom.presentation.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminWebController {

    @GetMapping("/admin")
    public String admin() {
        return "redirect:/admin/";
    }

    @GetMapping("/admin/")
    public String adminIndex() {
        return "forward:/admin/index.html";
    }
}
