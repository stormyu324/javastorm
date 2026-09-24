package com.javastorm.shop.service;

/** 面向用户的业务错误，消息会直接展示在页面上。 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
