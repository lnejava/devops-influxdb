package com.cmdi.influxdb.timer;

import com.alibaba.fastjson.JSONObject;
import com.cmdi.influxdb.config.InfluxDBConfig;
import com.cmdi.influxdb.constant.MeasurementAndTime;
import com.cmdi.influxdb.pojo.BookInfo;
import com.cmdi.influxdb.utils.InfluxDBUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.influxdb.dto.Point;
import org.influxdb.dto.QueryResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@EnableScheduling
@Component
@Slf4j
public class InfluxDBTimer {
    @Resource
    private InfluxDBUtils influxDBUtils;

    @Resource
    private MeasurementAndTime measurementAndTime;
    @Resource
    private ObjectMapper objectMapper;
    /**
     * 探测influxdb数据是否正常，60秒探测一次
     */
    @Scheduled(fixedDelay = 60,timeUnit = TimeUnit.SECONDS)
    public void isHealthy(){
        boolean good = influxDBUtils.getInfluxDB().ping().isGood();
        if(!good){
            log.warn("心跳探测失败，请检查influxdb是否正常启动");
        }
        log.info("心跳探测成功：{}",good);
    }

    @Scheduled(cron = "0/60 * * * * ?")
    public void influxLogInfo(){
        log.info("==================开始执行定时任务查询================");
        Map<String,String> map = measurementAndTime.queryTime();
        String lastQueryTime = influxDBUtils.getLastQueryTime();
        System.out.println(lastQueryTime);
        System.out.println(map.toString());
        log.info("本地存储的查询时间为：",lastQueryTime);
        Map<String,String> convertValue = new HashMap<>();
        if(lastQueryTime != null && !lastQueryTime.isEmpty()){
            log.info("lastQueryTime不为null或不为空");
            try {
                convertValue = objectMapper.readValue(lastQueryTime, HashMap.class);
            } catch (JsonProcessingException e) {
                log.warn("lastQueryTime转换为map类型报错：",e.getMessage());
                e.printStackTrace();
            }
        }else {
            log.info("本地存储的时间为空或null，直接赋值map");
            convertValue = map;
        }
        String command = null;
        for(String measurement:measurementAndTime.getMeasurements()){
            lastQueryTime = convertValue.get(measurement);
            if(lastQueryTime == null || "".equals(lastQueryTime)){
                command = "SELECT * FROM "+influxDBUtils.getRetentionPolicy()+
                        "."+measurement+" ORDER BY time DESC LIMIT 10";
            }else {
                command = "SELECT * FROM "+influxDBUtils.getRetentionPolicy()+
                        "."+measurement+" where time >\'"+lastQueryTime +"\' ORDER BY time DESC LIMIT 10";
            }
            QueryResult query = influxDBUtils.query(command);
            List<Map<String, Object>> mapList = influxDBUtils.queryResultProcess(query);
            int size = mapList.size();
            if(size != 0){
                log.info(measurement+"表一共有"+size+"条记录");
                String updateQueryTime = (String) mapList.get(0).get("time");
                log.info("最新数据的时间为：",updateQueryTime);
                convertValue.put(measurement,updateQueryTime);
            }
            String jsonString = JSONObject.toJSONString(mapList);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            log.info("查询时间：{}，查询结果为：{}", formatter.format(LocalDateTime.now()),jsonString);

        }
        String convertValue1 = null;
        try {
            convertValue1 = objectMapper.writeValueAsString(convertValue);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        influxDBUtils.settLastQueryTime(convertValue1);

    }

}
