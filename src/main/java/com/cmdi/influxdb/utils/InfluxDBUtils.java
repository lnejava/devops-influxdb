package com.cmdi.influxdb.utils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Data
@Slf4j
public class InfluxDBUtils {
    private String userName;
    private String password;
    private String url;
    private String database;

    /**数据保留策略*/
    private String retentionPolicy;
//    /**数据表*/
//    private String[] measurements;
    /**InfluxDB实例*/
    private InfluxDB influxDB;

    /**自定义http连接influxdb的builder，防止数量太大而导致的socket连接超时*/
    static OkHttpClient.Builder client = new OkHttpClient.Builder()
            .readTimeout(60,TimeUnit.SECONDS)
            .writeTimeout(60,TimeUnit.SECONDS)
            .connectTimeout(60,TimeUnit.SECONDS);

    public InfluxDBUtils(String userName,String password,String url,String database,String retentionPolicy){
            this.userName  = userName;
            this.password = password;
            this.url = url;
            this.database = database;
            this.retentionPolicy = retentionPolicy == null || "".equals(retentionPolicy) ? "autogen":retentionPolicy;
            this.influxDB = influxDBBuild();
    }

    /**
     * 连接Influx数据库
     * @return
     */
    private InfluxDB influxDBBuild(){
        log.info("influx db 初始化");
        if(influxDB == null){
            log.info("================连接influx db 数据库================");

//            influxDB = InfluxDBFactory.connect(url,userName,password,client);    // 有权限认证
            // 无权限认证
            influxDB = InfluxDBFactory.connect(url,client);
        }
        try{
//            createDB(database);   // 不存在则创建数据库
            influxDB.setDatabase(database);
        }catch (Exception e){
            log.error("create influx db failed,error:",e.getCause());
        }finally {
            influxDB.setRetentionPolicy(retentionPolicy);
        }
        influxDB.setLogLevel(InfluxDB.LogLevel.BASIC);
        return influxDB;
    }

    /**
     * 设置数据保留策略，default 策略名
     * database 数据库名
     * 30d 数据保存时间
     * 1 副本个数为1
     * 结尾 DEFAULT表示设置为默认策略
     */
    public void createRetentionPolicy(){
        String command = String.format("CREATE RETENTION POLICY \"%s\" ON \"%s\" DURATION %s REPLICATION %s DEFAULT",
                "default",database,"30d",1);
        this.query(command);
        log.info("创建数据库保持策略");
    }

    /**
     * 封装查询
     * 若需要分页的话：
     *  SELECT *FROM measurement WHERE 时间范围 LIMIT rows OFFSET (page - 1)*rows
     *  rows每页的条数，page页码（第几页）
     * @param command 查询语句
     * @return
     */
    public QueryResult query(String command){
        return influxDB.query(new Query(command,database));
    }

    /**
     * 实体类映射插入
     * @param clazz
     */
    public void insertByPoJo(Class<Object> clazz){
        Point.Builder builder = Point.measurementByPOJO(clazz.getClass());
        Point point = builder.addFieldsFromPOJO(clazz)
                .time(Long.parseLong(clazz.getName()+System.currentTimeMillis()), TimeUnit.MILLISECONDS)
                .build();
        influxDB.write(database,"",point);
    }
    /**
     * 插入 自定义time
     * @param measurement
     * @param tags
     * @param fields
     */
    public void insert(String measurement, Map<String,String> tags, Map<String,Object> fields){
        Point.Builder builder = Point.measurement(measurement);
        builder.time(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        builder.tag(tags);
        builder.fields(fields);
        influxDB.write(database,"",builder.build());
    }

    /**
     * influxdb开启UDP功能，默认端口：8089，默认数据库：udp，没有提供代码传数据库功能接口
     * @param measurement
     * @param tags
     * @param fields
     */
    public void insertUDP(String measurement,Map<String,String> tags,Map<String,Object> fields){
        Point.Builder builder = Point.measurement(measurement);
        builder.time(System.currentTimeMillis(),TimeUnit.MILLISECONDS);
        builder.tag(tags);
        builder.fields(fields);
        int udpPort = 8089;
        influxDB.write(udpPort,builder.build());
    }

    /**
     * 查询结果处理
     * @param queryResult
     * @return
     */
    public List<Map<String,Object>> queryResultProcess(QueryResult queryResult){
        List<Map<String,Object>> mapList = new ArrayList<>();
        List<QueryResult.Result> resultList = queryResult.getResults();
        // 把查询出的结果集合转换为对应的实体对象，聚合成list
        for(QueryResult.Result query:resultList){
            List<QueryResult.Series> seriesList = query.getSeries();
            if(seriesList != null && seriesList.size() != 0){
                for(QueryResult.Series series:seriesList){
                    List<String> columns = series.getColumns();
                    String[] keys = columns.toArray(new String[columns.size()]);
                    List<List<Object>> values = series.getValues();
                    if(values != null && values.size() != 0){
                        for(List<Object> value:values){
                            Map<String,Object> map = new HashMap<>(keys.length);
                            for(int i = 0;i<keys.length;i++){
                                map.put(keys[i],value.get(i));
                            }
                            mapList.add(map);
                        }
                    }
                }
            }
        }
        return mapList;
    }

    /**
     * 新建数据库
     * @param dbName
     */
    public void createDB(String dbName){
        log.info("===========尝试新建数据库==========");
        String command = String.format("CREATE DATABASE \"%s\"",dbName);
        query(command);
    }

    /**
     * 删除数据库
     * @param dbName
     */
    public void deleteDB(String dbName){
        String command = String.format("DROP DATABASE \"%s\"",dbName);
        query(command);

    }

    /**
     * 批量写入测点
     * @param batchPoints
     */
    public void batchInsert(BatchPoints batchPoints){
        influxDB.write(batchPoints);
    }

    /**
     * 批量写入数据
     * @param database
     * @param retentionPolicy
     * @param consistency 一致性
     * @param records
     */
    public void batchInsert(final String database,final String retentionPolicy,
                            final InfluxDB.ConsistencyLevel consistency,
                            final List<String> records){
        influxDB.write(database,retentionPolicy,consistency,records);
    }

    public void batchInsert(
            final InfluxDB.ConsistencyLevel consistency,
            final List<String> records){
        influxDB.write(database,"",consistency,records);
    }

    /**
     * 将查询的influx数据最新时间存入本地文档
     * @param lastTime
     */
    public void settLastQueryTime(String lastTime){
        File file1 = new File("/Documents");
        if(!file1.exists()){
            file1.mkdirs();
        }
        File file = new File("/Documents/lastTime.txt");
        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] bytes = lastTime.getBytes("utf-8");
            outputStream.write(bytes);
            outputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 查询存储在本地文件中的时间
     *
     * @return
     */
    public String getLastQueryTime(){
        File file = new File("/Documents/lastTime.txt");
        if(!file.exists()){
            return "";
        }
        StringBuilder builder = new StringBuilder();
        byte[] bytes = new byte[1024];
        int len = -1;
        FileInputStream fileInputStream = null;
        String content = "";
        try {
            fileInputStream = new FileInputStream(file);
            while((len = fileInputStream.read(bytes)) > 0){
                String s = new String(bytes, 0, len, Charset.forName("utf-8"));
                builder.append(s);
            }
            content = builder.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }
}
