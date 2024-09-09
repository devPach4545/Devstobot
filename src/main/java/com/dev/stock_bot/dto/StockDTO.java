package com.dev.stock_bot.dto;

import java.util.Date;

import lombok.Data;

@Data
public class StockDTO {
    private Date date;
    private String symbol;
    private double open;
    private double high;
    private double low;
    private double close;
    private long volume;
}
