package com.auth.api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/forgot-password")
public class ForgotPasswordPageController {

    /**
     * Affiche la page de demande de réinitialisation de mot de passe
     */
    @GetMapping
    public String showForgotPasswordPage() {
        return "forgot-password";
    }
}