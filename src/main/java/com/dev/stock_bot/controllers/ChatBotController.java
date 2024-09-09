package com.dev.stock_bot.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dev.stock_bot.dto.PromptDTO;
import com.dev.stock_bot.responses.ApiResponse;
import com.dev.stock_bot.services.ChatBotService;

@RestController
@RequestMapping("/ai/chat")
@CrossOrigin(origins = "http://localhost:3000")
public class ChatBotController {

    @Autowired
    ChatBotService chatBotService;

    
    @PostMapping
    public ResponseEntity<ApiResponse> getCoinDetails(@RequestBody PromptDTO prompt) throws Exception {

        ApiResponse response = chatBotService.getStockDetails(prompt.getPrompt());
      
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/general")
    public ResponseEntity<String> generalChatHandler(@RequestBody PromptDTO prompt) throws Exception {

        String response = chatBotService.simpleChat(prompt.getPrompt());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
