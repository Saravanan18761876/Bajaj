package com.bajajfinserv;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;

@SpringBootApplication
public class Application {
    
    @Value("${registration.regNo:REG12347}")
    private String regNo;
    
    @Value("${registration.name:John Doe}")
    private String name;
    
    @Value("${registration.email:john@example.com}")
    private String email;
    
    private String webhookUrl;
    private String accessToken;
    
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        try {
            // Step 1: Generate webhook
            generateWebhook();
            
            // Step 2: Solve SQL problem based on registration number
            String finalQuery = solveSqlProblem();
            
            // Step 3: Submit solution to webhook
            submitSolution(finalQuery);
            
        } catch (Exception e) {
            System.err.println("Error during application startup: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void generateWebhook() {
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";
        
        // Create request body
        JSONObject requestBody = new JSONObject();
        requestBody.put("name", name);
        requestBody.put("regNo", regNo);
        requestBody.put("email", email);
        
        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);
        
        // Send POST request
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        
        if (response.getStatusCode() == HttpStatus.OK) {
            JSONObject responseJson = new JSONObject(response.getBody());
            webhookUrl = responseJson.getString("webhook");
            accessToken = responseJson.getString("accessToken");
            System.out.println("Webhook generated successfully: " + webhookUrl);
        } else {
            throw new RuntimeException("Failed to generate webhook. Status code: " + response.getStatusCode());
        }
    }
    
    private String solveSqlProblem() {
        // Based on the last two digits of regNo, determine which question to solve
        String lastTwoDigits = regNo.substring(regNo.length() - 2);
        int lastDigits = Integer.parseInt(lastTwoDigits);
        
        // Even or odd check
        if (lastDigits % 2 == 0) {
            // Even - Question 2 (not provided, but we'll handle the provided Question 1)
            // For this implementation, we'll use the provided Question 1
            return solveQuestion1();
        } else {
            // Odd - Question 1
            return solveQuestion1();
        }
    }
    
    private String solveQuestion1() {
        // SQL query to solve Question 1
        return "SELECT p.AMOUNT AS SALARY, " +
               "CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME) AS NAME, " +
               "FLOOR(DATEDIFF(CURDATE(), e.DOB) / 365.25) AS AGE, " +
               "d.DEPARTMENT_NAME " +
               "FROM PAYMENTS p " +
               "JOIN EMPLOYEE e ON p.EMP_ID = e.EMP_ID " +
               "JOIN DEPARTMENT d ON e.DEPARTMENT = d.DEPARTMENT_ID " +
               "WHERE DAY(p.PAYMENT_TIME) != 1 " +
               "ORDER BY p.AMOUNT DESC " +
               "LIMIT 1";
    }
    
    private void submitSolution(String finalQuery) {
        RestTemplate restTemplate = new RestTemplate();
        
        // Create request body
        JSONObject requestBody = new JSONObject();
        requestBody.put("finalQuery", finalQuery);
        
        // Set headers with JWT token
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", accessToken);
        
        HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);
        
        // Send POST request to webhook
        ResponseEntity<String> response = restTemplate.exchange(webhookUrl, HttpMethod.POST, entity, String.class);
        
        if (response.getStatusCode() == HttpStatus.OK) {
            System.out.println("Solution submitted successfully!");
            System.out.println("Response: " + response.getBody());
        } else {
            throw new RuntimeException("Failed to submit solution. Status code: " + response.getStatusCode());
        }
    }
}