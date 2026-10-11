package de.team.betriebsrente.controller;

import de.team.betriebsrente.security.UserManagementService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {
    private final UserManagementService userManagement;

    public AdminUserController(UserManagementService userManagement) {
        this.userManagement = userManagement;
    }

    @GetMapping
    public String users(Model model) {
        model.addAttribute("users", userManagement.listUsers());
        return "admin-users";
    }

    @PostMapping
    public String create(String username, String password, String role, RedirectAttributes redirect) {
        try {
            userManagement.createUser(username, password, role);
            redirect.addFlashAttribute("success", "Benutzer wurde angelegt.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{username}/password")
    public String resetPassword(@PathVariable String username, String password, RedirectAttributes redirect) {
        try {
            userManagement.resetPassword(username, password);
            redirect.addFlashAttribute("success", "Passwort wurde geändert.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{username}/delete")
    public String delete(@PathVariable String username, Authentication authentication, RedirectAttributes redirect) {
        try {
            userManagement.deleteUser(username, authentication.getName());
            redirect.addFlashAttribute("success", "Benutzer wurde gelöscht.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/users";
    }
}
