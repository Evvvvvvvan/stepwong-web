package com.example.stepwong.controller;

import com.example.stepwong.dto.AccountForm;
import com.example.stepwong.entity.ExecutionLog;
import com.example.stepwong.entity.StepAccount;
import com.example.stepwong.service.StepAccountService;
import com.example.stepwong.service.StepExecutionService;
import javax.validation.Valid;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AccountController {

    private final StepAccountService stepAccountService;
    private final StepExecutionService stepExecutionService;

    public AccountController(StepAccountService stepAccountService, StepExecutionService stepExecutionService) {
        this.stepAccountService = stepAccountService;
        this.stepExecutionService = stepExecutionService;
    }

    @GetMapping("/accounts")
    public String list(Model model) {
        List<StepAccount> accounts = stepAccountService.listCurrentUserAccounts();
        model.addAttribute("accounts", accounts);
        model.addAttribute("maskedPassword", stepAccountService.maskPassword());
        return "accounts/list";
    }

    @GetMapping("/accounts/new")
    public String createPage(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new AccountForm());
        }
        model.addAttribute("mode", "create");
        return "accounts/form";
    }

    @PostMapping("/accounts")
    public String create(
            @Valid @ModelAttribute("form") AccountForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", "create");
            return "accounts/form";
        }
        try {
            stepAccountService.create(form);
            redirectAttributes.addFlashAttribute("success", "账号已添加");
            return "redirect:/accounts";
        } catch (IllegalArgumentException e) {
            bindingResult.reject("accountError", e.getMessage());
            model.addAttribute("mode", "create");
            return "accounts/form";
        }
    }

    @GetMapping("/accounts/{id}/edit")
    public String editPage(@PathVariable Long id, Model model) {
        StepAccount account = stepAccountService.getCurrentUserAccount(id);
        model.addAttribute("form", stepAccountService.toForm(account));
        model.addAttribute("accountId", id);
        model.addAttribute("mode", "edit");
        return "accounts/form";
    }

    @PostMapping("/accounts/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") AccountForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("accountId", id);
            model.addAttribute("mode", "edit");
            return "accounts/form";
        }
        try {
            stepAccountService.update(id, form);
            redirectAttributes.addFlashAttribute("success", "账号已更新");
            return "redirect:/accounts";
        } catch (IllegalArgumentException e) {
            bindingResult.reject("accountError", e.getMessage());
            model.addAttribute("accountId", id);
            model.addAttribute("mode", "edit");
            return "accounts/form";
        }
    }

    @PostMapping("/accounts/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        stepAccountService.delete(id);
        redirectAttributes.addFlashAttribute("success", "账号已删除");
        return "redirect:/accounts";
    }

    @PostMapping("/accounts/{id}/execute")
    public String executeNow(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        ExecutionLog log = stepExecutionService.executeCurrentUserAccount(id);
        if (Boolean.TRUE.equals(log.getSuccess())) {
            redirectAttributes.addFlashAttribute("success", log.getMessage());
        } else {
            redirectAttributes.addFlashAttribute("error", log.getMessage());
        }
        return "redirect:/accounts";
    }

    @PostMapping("/accounts/{id}/reveal")
    public String reveal(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        String password = stepAccountService.revealPassword(id);
        redirectAttributes.addFlashAttribute("revealedAccountId", id);
        redirectAttributes.addFlashAttribute("revealedPassword", password);
        return "redirect:/accounts";
    }
}
