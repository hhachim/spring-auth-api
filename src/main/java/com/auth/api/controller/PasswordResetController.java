package com.auth.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.auth.api.model.User;
import com.auth.api.repository.UserRepository;
import com.auth.api.service.EmailService;
import com.auth.api.service.UserService;

@Controller
public class PasswordResetController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;
    
    @Autowired
    private EmailService emailService;

    @Value("${app.frontend-url:#{null}}")
    private String frontendUrl;

    /**
     * Affiche le formulaire de réinitialisation de mot de passe
     */
    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam(required = false) String token, 
                                       Model model) {
        // Si un frontend URL est configuré et qu'il est différent de l'URL du backend,
        // rediriger vers le frontend
        if (frontendUrl != null && !frontendUrl.isEmpty() && !frontendUrl.contains("localhost:8086")) {
            return "redirect:" + frontendUrl + "/reset-password?token=" + token;
        }
        
        // Vérifier si le token existe
        if (token == null || token.isEmpty()) {
            model.addAttribute("error", "Token manquant ou invalide");
            return "error";
        }
        
        // Vérifier si le token est valide
        if (!userRepository.findByResetPasswordToken(token).isPresent()) {
            model.addAttribute("error", "Token invalide ou expiré");
            return "error";
        }
        
        // Tout est ok, afficher le formulaire de réinitialisation
        model.addAttribute("token", token);
        return "reset-password-form";
    }

    /**
     * Traite la soumission du formulaire de réinitialisation de mot de passe
     */
    @PostMapping("/reset-password")
    public String processResetPasswordForm(@RequestParam("token") String token,
                                          @RequestParam("password") String password,
                                          @RequestParam("confirmPassword") String confirmPassword,
                                          RedirectAttributes redirectAttributes,
                                          Model model) {
        
        // Vérifier si les mots de passe correspondent
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Les mots de passe ne correspondent pas");
            model.addAttribute("token", token);
            return "reset-password-form";
        }
        
        try {
            // Récupérer l'utilisateur avant de réinitialiser le mot de passe
            User user = userRepository.findByResetPasswordToken(token)
                    .orElseThrow(() -> new RuntimeException("Token invalide"));
            
            // Réinitialiser le mot de passe
            userService.resetPasswordWithToken(token, password);
            
            // Envoyer un email de confirmation
            emailService.sendPasswordResetConfirmationEmail(user.getEmail());
            
            // Rediriger vers la page de succès
            redirectAttributes.addFlashAttribute("message", "Votre mot de passe a été réinitialisé avec succès.");
            return "redirect:/reset-password-success";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("token", token);
            return "reset-password-form";
        }
    }
    
    /**
     * Affiche la page de succès après la réinitialisation du mot de passe
     */
    @GetMapping("/reset-password-success")
    public String showResetPasswordSuccessPage() {
        return "reset-password-success";
    }
    
    /**
     * Page d'erreur générique
     */
    @GetMapping("/error")
    public String showErrorPage() {
        return "error";
    }
}