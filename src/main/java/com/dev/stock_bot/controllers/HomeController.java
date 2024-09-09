package com.dev.stock_bot.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dev.stock_bot.responses.ApiResponse;
import com.dev.stock_bot.services.ChatBotService;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
public class HomeController {
    /**
     * This is the home controller and it returns a welcome message
     * 
     * @return ResponseEntity<ApiResponse>
     */

    @Autowired
    ChatBotService chatBotService;

    @GetMapping()
    public ResponseEntity<ApiResponse> homeController() throws Exception {

        ApiResponse response = new ApiResponse();

        response.setMessage("Welcome to Stock Bot");

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
