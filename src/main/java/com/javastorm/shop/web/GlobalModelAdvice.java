package com.javastorm.shop.web;

import com.javastorm.shop.service.BusinessException;
import com.javastorm.shop.service.Cart;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@ControllerAdvice
public class GlobalModelAdvice {

    private final Cart cart;

    public GlobalModelAdvice(Cart cart) {
        this.cart = cart;
    }

    @ModelAttribute("cartCount")
    public int cartCount() {
        return cart.getTotalQuantity();
    }

    @ExceptionHandler(BusinessException.class)
    public ModelAndView businessError(BusinessException e) {
        ModelAndView mv = new ModelAndView("error-page");
        mv.addObject("message", e.getMessage());
        mv.addObject("cartCount", cart.getTotalQuantity());
        return mv;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ModelAndView uploadTooLarge() {
        ModelAndView mv = new ModelAndView("error-page");
        mv.addObject("message", "上传的文件太大（单个图片最大 10MB，单次合计最大 50MB）");
        mv.addObject("cartCount", cart.getTotalQuantity());
        return mv;
    }
}
