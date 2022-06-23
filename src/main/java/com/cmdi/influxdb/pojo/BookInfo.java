package com.cmdi.influxdb.pojo;

import lombok.Builder;
import lombok.Data;
import org.influxdb.annotation.Column;
import org.influxdb.annotation.Measurement;

@Data
@Builder
@Measurement(name = "bookInfo")
public class BookInfo {
    /**注意，Influx中时间戳是以UTC保存的，再保存和提取过程中需要注意时区转换*/
    @Column(name = "time")
    private String time;

    @Column(name = "book_name",tag = true)
    private String bookName;

    @Column(name = "book_id",tag = true)
    private String bookId;

    @Column(name = "book_msg")
    private String bookMsg;

    @Column(name = "book_price")
    private Double bookPrice;
}
