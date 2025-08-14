package com.example.bowchat;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/view")
public class ChatViewController {

    @GetMapping("/chat")
    public String chatView() {
        return "chat";
    }

    @GetMapping("/login")
    public String loginView() {
        return "login";
    }

    @GetMapping("/product")
    public String productsView() {
        return "products";
    }

    @GetMapping("/product/new")
    public String productsNewView() {
        return "product-new";
    }

    @GetMapping("/signup")
    public String signupView() {
        return "signup";
    }
}
