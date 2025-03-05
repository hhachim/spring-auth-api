package com.auth.api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/login")
public class LoginPageController {

    /**
     * Affiche la page de connexion
     */
    @GetMapping
    public String showLoginPage() {
        return "login";
    }
}