package com.desofs.config;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.desofs.audit.AuditAction;
import com.desofs.audit.AuditService;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final AuditService auditService;

    public GlobalExceptionHandler(AuditService auditService) {
        this.auditService = auditService;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleUnexpected(Exception ex, HttpServletRequest request) {
        // log interno com detalhes completos
        log.error("Unexpected error on {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);

        // audit do erro de segurança/infra
        auditService.record(
            "system",
            AuditAction.UNEXPECTED_ERROR,
            "system",
            request.getRequestURI(),
            false,
            ex.getClass().getSimpleName() + ": " + ex.getMessage()
        );

        // resposta genérica ao cliente — sem stack trace, sem detalhes internos
        return ResponseEntity.status(500).body(Map.of("error", "An unexpected error occurred"));
    }
}