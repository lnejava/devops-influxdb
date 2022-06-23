package com.cmdi.influxdb;

import com.alibaba.fastjson.JSONObject;
import com.cmdi.influxdb.constant.MeasurementAndTime;
import com.cmdi.influxdb.pojo.BookInfo;
import com.cmdi.influxdb.utils.InfluxDBUtils;
import org.influxdb.dto.Point;
import org.influxdb.dto.QueryResult;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class DevopsInfluxdbApplicationTests {

	@Resource
	private InfluxDBUtils influxDBUtils;

	@Resource
	private MeasurementAndTime measurementAndTime;

	@Test
	void contextLoads() {

		Map<String, String> stringStringMap = measurementAndTime.queryTime();
		int size = stringStringMap.size();
		System.out.println(size);
		String s = stringStringMap.toString();
		System.out.println(s);

//		Random random = new Random();
//		for(int i = 0;i<2;i++){
//			BookInfo bookInfo = BookInfo.builder()
//					.bookId(String.valueOf(random.nextDouble()))
//					.bookName(String.valueOf(random.nextDouble()))
//					.bookMsg(String.valueOf(random.nextDouble()))
//					.bookPrice(random.nextDouble())
//					.build();
//			Point point = Point.measurementByPOJO(bookInfo.getClass())
//					.addFieldsFromPOJO(bookInfo)
//					.time(System.currentTimeMillis(), TimeUnit.MICROSECONDS)
//					.build();
//			influxDBUtils.getInfluxDB().write(influxDBUtils.getDatabase(),"",point);
//			System.out.println(bookInfo);
//
//		}
// InfluxDB支持分页查询,因此可以设置分页查询条件
//		String pageQuery = " LIMIT " + request.getPageSize() + " OFFSET " + ((request.getPageNum() - 1) * request.getPageSize());
//		String command = "SELECT * FROM autogen.bookInfo ORDER BY time DESC";
//		QueryResult query = influxDBUtils.query(command);
//		List<Map<String, Object>> mapList = influxDBUtils.queryResultProcess(query);
//		int size = mapList.size();
//		System.out.println(size);
//		System.out.println(mapList.toString());
//		String jsonString = JSONObject.toJSONString(mapList);
//		System.out.println(jsonString);
//		boolean good = influxDBUtils.getInfluxDB().ping().isGood();
//		System.out.println(good);
	}

}
