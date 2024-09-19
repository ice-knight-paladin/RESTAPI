package com.example.test;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CalculatorController {

    @GetMapping("/")
    public String index() {
        return "calculator";
    }

    @PostMapping("/calculate")
    public String calculate(@RequestParam double num1, @RequestParam double num2, @RequestParam String operation, Model model) {
        double result = 0;

        switch (operation) {
            case "add":
                result = num1 + num2;
                break;
            case "subtract":
                result = num1 - num2;
                break;
            case "multiply":
                result = num1 * num2;
                break;
            case "divide":
                if (num2 != 0) {
                    result = num1 / num2;
                } else {
                    return "redirect:/?error=division_by_zero";
                }
                break;
        }

        model.addAttribute("result", result);
        return "calculator";
    }
}