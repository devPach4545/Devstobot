package com.dev.stock_bot.services;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.dev.stock_bot.dto.StockDTO;
import com.dev.stock_bot.responses.ApiResponse;
import com.dev.stock_bot.responses.FunctionResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ChatBotServiceImple implements ChatBotService {
    String GEMINI_API_KEY = "your key";
    String ALPHAVANTAGE_API_KEY = "your key";

    public String getSymbol(String companyName) throws Exception {
        // Construct the URL for SYMBOL_SEARCH with dynamic company name and API key
        String keyword = companyName; // Replace with the user-supplied company name
        String url = "https://www.alphavantage.co/query?function=SYMBOL_SEARCH&keywords="
                + keyword + "&apikey=" + ALPHAVANTAGE_API_KEY;

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // Get the URL and expect a Map response
        ResponseEntity<Map> responseBody = restTemplate.getForEntity(url, Map.class);

        if (responseBody != null && responseBody.getBody() != null) {
            Map<String, Object> body = responseBody.getBody();

            // Extract the best matching stock symbol
            java.util.List<Map<String, String>> bestMatches = (java.util.List<Map<String, String>>) body
                    .get("bestMatches");
            
            if (bestMatches != null && !bestMatches.isEmpty()) {
                String region = bestMatches.get(0).get("4. region");
                if(region.equals("United States")){
                    return bestMatches.get(0).get("1. symbol");
                }
                
            } else {
                throw new Exception("No matching symbols found for the company name: " + companyName);
            }
        }

        throw new Exception("Failed to retrieve stock symbol for the company name: " + companyName);
    }

    /**
     * This method should return the stock data for the given company name
     * 
     * @throws Exception
     */
    @Override
    public StockDTO makeApiRequest(String companyName) throws Exception {
        FunctionResponse res = getFunctionResponse(companyName);
        String stocListName = getSymbol(res.getCompanyName());
        System.out.println("STOCK COMPANY NAME"+stocListName);
        String url = "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=" +
                stocListName + "&apikey="
                + ALPHAVANTAGE_API_KEY;

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();

        HttpEntity<String> entity = new HttpEntity<>(headers);

        // Get the url and expect a Map response
        ResponseEntity<Map> responseBody = restTemplate.getForEntity(url, Map.class);

        if (responseBody != null && responseBody.getBody() != null) {
            Map<String, Object> body = responseBody.getBody();

            // Get the symbol from the meta data
            Map<String, String> metaData = (Map<String, String>) body.get("Meta Data");
            String symbol = metaData.get("2. Symbol");

            // Get the time series data
            Map<String, Map<String, String>> timeSeries = (Map<String, Map<String, String>>) body
                    .get("Time Series (Daily)");

            // Get the last refreshed date (the latest available date)
            String lastRefreshed = metaData.get("3. Last Refreshed");
            Map<String, String> latestData = timeSeries.get(lastRefreshed);

            // Create and populate the StockDTO
            StockDTO stockDTO = new StockDTO();
            try {
                stockDTO.setDate(new SimpleDateFormat("yyyy-MM-dd").parse(lastRefreshed)); // Parse date
            } catch (ParseException e) {
                e.printStackTrace();
            }
            stockDTO.setSymbol(symbol);
            stockDTO.setOpen(Double.parseDouble(latestData.get("1. open")));
            stockDTO.setHigh(Double.parseDouble(latestData.get("2. high")));
            stockDTO.setLow(Double.parseDouble(latestData.get("3. low")));
            stockDTO.setClose(Double.parseDouble(latestData.get("4. close")));
            stockDTO.setVolume(Long.parseLong(latestData.get("5. volume")));

            return stockDTO;
        }
        // Return a Stock DTO
        throw new Exception("Stock data not found");

    }

    /**
     * This method should return a simple chat response
     */
    @Override
    public String simpleChat(String prompt) {
    String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=" + GEMINI_API_KEY;

    // Set headers
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    // Create JSON object using Jackson
    Map<String, Object> requestBodyMap = new HashMap<>();
    
    Map<String, Object> userMessage = new HashMap<>();
    userMessage.put("role", "user");

    Map<String, String> textPart = new HashMap<>();
    textPart.put("text", prompt);

    userMessage.put("parts", new Map[] { textPart });
    requestBodyMap.put("contents", new Map[] { userMessage });

    ObjectMapper objectMapper = new ObjectMapper();
    String requestBody;
    try {
        // Convert the requestBodyMap to a JSON string
        requestBody = objectMapper.writeValueAsString(requestBodyMap);
    } catch (JsonProcessingException e) {
        throw new RuntimeException("Error converting request body to JSON", e);
    }

    // Prepare HTTP request
    HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
    RestTemplate restTemplate = new RestTemplate();

    // Send POST request
    ResponseEntity<String> response = restTemplate.postForEntity(GEMINI_API_URL, entity, String.class);

    return response.getBody();
}

    /**
     * This method should return api response for the stock details
     * 
     * @throws Exception
     */
    @Override
    public ApiResponse getStockDetails(String prompt) throws Exception {
        // Obtain data for the API request
    StockDTO stockApiResponse = makeApiRequest(prompt);
    FunctionResponse res = getFunctionResponse(prompt);

    // Prepare HTTP headers
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    // Prepare the API URL
    String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=" + GEMINI_API_KEY;

    // Use Jackson to construct the JSON object for the body
    Map<String, Object> requestBody = new HashMap<>();

    // Contents array
    Map<String, Object> userMessage = new HashMap<>();
    userMessage.put("role", "user");

    Map<String, String> textPart = new HashMap<>();
    textPart.put("text", prompt);
    userMessage.put("parts", new Map[] { textPart });

    Map<String, Object> modelMessage = new HashMap<>();
    modelMessage.put("role", "model");

    Map<String, Object> functionCall = new HashMap<>();
    functionCall.put("name", "getStockDetails");

    Map<String, String> functionArgs = new HashMap<>();
    functionArgs.put("stockName", res.getCompanyName());
    functionArgs.put("stockData", res.getCompanyData());
    functionCall.put("args", functionArgs);

    Map<String, Object> functionPart = new HashMap<>();
    functionPart.put("functionCall", functionCall);
    modelMessage.put("parts", new Map[] { functionPart });

    Map<String, Object> functionResponseMessage = new HashMap<>();
    functionResponseMessage.put("role", "function");

    Map<String, Object> functionResponse = new HashMap<>();
    functionResponse.put("name", "getStockDetails");

    Map<String, String> responseContent = new HashMap<>();
    responseContent.put("content", stockApiResponse.toString());
    functionResponse.put("response", responseContent);

    Map<String, Object> functionResponsePart = new HashMap<>();
    functionResponsePart.put("functionResponse", functionResponse);
    functionResponseMessage.put("parts", new Map[] { functionResponsePart });

    requestBody.put("contents", new Map[] { userMessage, modelMessage, functionResponseMessage });

    // Tools section
    Map<String, Object> tools = new HashMap<>();
    Map<String, Object> functionDeclarations = new HashMap<>();
    functionDeclarations.put("name", "getStockDetails");
    functionDeclarations.put("description", "Get stock market data from given currency object.");

    Map<String, Object> parameters = new HashMap<>();
    Map<String, Object> properties = new HashMap<>();
    
    Map<String, Object> stockNameProperty = new HashMap<>();
    stockNameProperty.put("type", "string");
    stockNameProperty.put("description", "The stock name or symbol.");
    properties.put("stockName", stockNameProperty);
    
    Map<String, Object> stockDataProperty = new HashMap<>();
    stockDataProperty.put("type", "string");
    stockDataProperty.put("description", "Detailed stock data including symbol, lastRefreshed, open, high, low, close, volume, date.");
    properties.put("stockData", stockDataProperty);

    parameters.put("type", "object");
    parameters.put("properties", properties);
    parameters.put("required", new String[] { "stockName", "stockData" });

    functionDeclarations.put("parameters", parameters);
    tools.put("functionDeclarations", new Map[] { functionDeclarations });
    requestBody.put("tools", new Map[] { tools });

    // Convert the request body to JSON string using Jackson
    ObjectMapper objectMapper = new ObjectMapper();
    String jsonBody;
    try {
        jsonBody = objectMapper.writeValueAsString(requestBody);
    } catch (JsonProcessingException e) {
        throw new RuntimeException("Error converting request body to JSON", e);
    }

    // Send the HTTP request
    HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
    RestTemplate restTemplate = new RestTemplate();
    ResponseEntity<String> response = restTemplate.postForEntity(GEMINI_API_URL, entity, String.class);
    String responseBody = response.getBody();

    // Parse the JSON response using Jackson
    JsonNode rootNode = objectMapper.readTree(responseBody);

    // Extract the "candidates" array
    JsonNode candidatesNode = rootNode.path("candidates");
    if (candidatesNode.isArray() && candidatesNode.size() > 0) {
        // Get the first candidate
        JsonNode firstCandidate = candidatesNode.get(0);
        // Extract the "content" object
        JsonNode contentNode = firstCandidate.path("content");

        // Extract the "parts" array
        JsonNode partsNode = contentNode.path("parts");
        if (partsNode.isArray() && partsNode.size() > 0) {
            // Extract the "text" field from the first part
            String extractedText = partsNode.get(0).path("text").asText();
            System.out.println("Extracted Text: " + extractedText);

            ApiResponse apiResponse = new ApiResponse();
            apiResponse.setMessage(extractedText);
            return apiResponse;
        }
    }

    // If parsing fails or no content is found
    ApiResponse apiResponse = new ApiResponse();
    apiResponse.setMessage("No valid response received from API.");
    return apiResponse;
    }

    public FunctionResponse getFunctionResponse(String prompt) throws Exception {

        String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=" + GEMINI_API_KEY;
    
        // Create the JSON structure using Jackson
        Map<String, Object> requestBody = new HashMap<>();
    
        // Build contents section
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("parts", new Map[]{Map.of("text", prompt)});
        requestBody.put("contents", new Map[]{userMessage});
    
        // Build tools section
        Map<String, Object> functionDeclaration = new HashMap<>();
        functionDeclaration.put("name", "getStockDetails");
        functionDeclaration.put("description", "Get the stock details from given stock object");
    
        // Parameters structure
        Map<String, Object> parameters = new HashMap<>();
        Map<String, Object> properties = new HashMap<>();
    
        properties.put("stockName", Map.of("type", "string", "description", "The stock name or symbol."));
        properties.put("companyName", Map.of("type", "string", "description", "The name of the company."));
    
        // StockData as an object
        Map<String, Object> stockDataProperties = new HashMap<>();
        stockDataProperties.put("symbol", Map.of("type", "string"));
        stockDataProperties.put("lastRefreshed", Map.of("type", "string"));
        stockDataProperties.put("open", Map.of("type", "string"));
        stockDataProperties.put("high", Map.of("type", "string"));
        stockDataProperties.put("low", Map.of("type", "string"));
        stockDataProperties.put("close", Map.of("type", "string"));
        stockDataProperties.put("volume", Map.of("type", "string"));
        stockDataProperties.put("date", Map.of("type", "string"));
    
        properties.put("stockData", Map.of("type", "object", "description", "Detailed stock data.", "properties", stockDataProperties));
        parameters.put("type", "object");
        parameters.put("properties", properties);
        parameters.put("required", new String[]{"stockName", "companyName", "stockData"});
    
        functionDeclaration.put("parameters", parameters);
        requestBody.put("tools", new Map[]{Map.of("function_declarations", new Map[]{functionDeclaration})});
    
        // Convert the requestBody map to JSON string
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(requestBody);
        } catch (Exception e) {
            throw new RuntimeException("Error converting request body to JSON", e);
        }
    
        // Prepare HTTP headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    
        // Make the HTTP request
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.postForEntity(GEMINI_API_URL, entity, String.class);
        String responseBody = response.getBody();
    
        // Parse the response using Jackson
        JsonNode rootNode = objectMapper.readTree(responseBody);
    
        // Extract stockName, companyName, and stockData from the response
        String stockName = extractJsonValue(rootNode, "stockName");
        String companyName = extractJsonValue(rootNode, "companyName");
        String stockData = extractJsonValue(rootNode, "stockData");
    
        // Create and populate the FunctionResponse object
        FunctionResponse functionResponse = new FunctionResponse();
        functionResponse.setFunctionName("getStockDetails");
        functionResponse.setCompanyName(companyName);
        functionResponse.setCompanyData(stockData);
    
        return functionResponse;
    }
    
    // Helper method to extract a value from the JSON response using Jackson
    private static String extractJsonValue(JsonNode rootNode, String key) {
        JsonNode valueNode = rootNode.findPath(key);
        return valueNode.isMissingNode() ? "Key not found" : valueNode.asText();
    }
}
