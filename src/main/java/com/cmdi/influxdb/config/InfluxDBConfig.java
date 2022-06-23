package com.cmdi.influxdb.config;

import com.cmdi.influxdb.utils.InfluxDBUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class InfluxDBConfig {

    @Value("${spring.influx.user}")
    private String userName;

    @Value("${spring.influx.password}")
    private String password;

    @Value("${spring.influx.url}")
    private String url;

    @Value("${spring.influx.database}")
    private String database;

    @Value("${spring.influx.retentionPolicy}")
    private String retentionPolicy;

    /**
     * 获取InfluxDBUtils实例，注入到Spring容器
     * @return InfluxDBUtils
     */
    @Bean
    public InfluxDBUtils influxDBUtils(){
        return new InfluxDBUtils(userName,password,url,database,retentionPolicy);
    }

}
