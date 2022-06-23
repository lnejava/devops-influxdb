package com.cmdi.influxdb.constant;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Component
@Data
@ConfigurationProperties(prefix = "influxdb")
public class MeasurementAndTime {

    private List<String> measurements;


    public Map<String,String> queryTime(){
        Map<String,String> queryTimes = new HashMap<>();
        for(String measurement: measurements){
            queryTimes.put(measurement,"");
        }
//        String convertValue = objectMapper.convertValue(queryTimes, String.class);
        return queryTimes;
    }

}
