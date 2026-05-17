package com.ihit.stock.controller;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

import java.beans.PropertyEditorSupport;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@ControllerAdvice
public class GlobalBindingAdvice {

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(LocalDate.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) throws IllegalArgumentException {
                if (text == null || text.isBlank()) {
                    setValue(null);
                    return;
                }
                String[] patterns = {"yyyy-MM-dd", "M/d/yy", "M/d/yyyy", "d/M/yy", "d/M/yyyy", "yyyy/MM/dd", "MM/dd/yyyy", "MM/dd/yy"};
                for (String pattern : patterns) {
                    try {
                        setValue(LocalDate.parse(text, DateTimeFormatter.ofPattern(pattern)));
                        return;
                    } catch (Exception ignored) {}
                }
            }
        });
    }
}