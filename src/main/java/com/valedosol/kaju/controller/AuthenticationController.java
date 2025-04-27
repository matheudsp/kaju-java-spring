package com.valedosol.kaju.controller;


import com.valedosol.kaju.dto.LoginRequest;
import com.valedosol.kaju.dto.SignupRequest;
import com.valedosol.kaju.configs.security.JwtAuthenticationFilter;
import com.valedosol.kaju.service.AuthenticationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);


    @PostMapping("/signin")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response){
        try {
            String email = authenticationService.login(loginRequest, response);

            return new ResponseEntity<>(email+" está logado.", HttpStatus.OK);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.UNAUTHORIZED);
        }

    }


    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody SignupRequest signupRequest, HttpServletRequest request){
        try {
            authenticationService.registerAccount(signupRequest);
            return new ResponseEntity<>("Conta criada com sucesso.", HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.UNAUTHORIZED);
        }

    }
    @PostMapping("/signout")
    public ResponseEntity<?> logoutUser(HttpServletResponse response) {
        authenticationService.logoutUser(response);
        return new ResponseEntity("Você saiu!", HttpStatus.OK);
    }
}
