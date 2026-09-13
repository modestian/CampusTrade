package com.campus;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 校园二手交易平台启动类
 */
@SpringBootApplication
@MapperScan("com.campus.mapper")
@EnableScheduling
public class CampusTradeApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusTradeApplication.class, args);
    }
}
