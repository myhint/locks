package com.itbounds.demo.locks.config;

import lombok.Data;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Description 自定义配置
 * @Author blake
 * @Date 2020/4/12 10:40 下午
 * @Version 1.0
 */
@Configuration
@Data
public class MyConfig {

  @Bean
  public CuratorFramework curatorFramework() {
    return CuratorFrameworkFactory.newClient("115.159.152.204:2181",
        new RetryNTimes(10, 5000));
  }

}
