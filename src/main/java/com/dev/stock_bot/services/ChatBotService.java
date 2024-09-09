package com.dev.stock_bot.services;

import com.dev.stock_bot.dto.StockDTO;
import com.dev.stock_bot.responses.ApiResponse;

public interface ChatBotService {

    ApiResponse getStockDetails(String prompt) throws Exception;

    String simpleChat(String prompt);

    public String getSymbol(String companyName) throws Exception;

    public StockDTO makeApiRequest(String companyName) throws Exception;
}
