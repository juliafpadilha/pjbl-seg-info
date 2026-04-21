package com.pucpr.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pucpr.model.Usuario;
import com.pucpr.repository.UsuarioRepository;
import com.pucpr.service.JwtService;
import com.sun.net.httpserver.HttpExchange;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

// Classe responsável por gerenciar as requisições de Autenticação.
public class AuthHandler {
    private final UsuarioRepository repository;
    private final JwtService jwtService;
    private final ObjectMapper mapper = new ObjectMapper();

    public AuthHandler(UsuarioRepository repository, JwtService jwtService) {
        this.repository = repository;
        this.jwtService = jwtService;
    }

    // DTO para tratar a requisição de Login (evitar usar a entidade completa)
    public static class LoginRequest {
        public String email;
        public String senha;
    }
    
    // DTO para tratar a requisição de Cadastro
    public static class RegisterRequest {
        public String nome;
        public String email;
        public String senha;
        public String role;
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, String responseMessage) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] bytes = responseMessage.getBytes("UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    // Gerencia o processo de Login
    public void handleLogin(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1); // 405 Method Not Allowed
            return;
        }

        try {
            InputStream is = exchange.getRequestBody();
            LoginRequest req = mapper.readValue(is, LoginRequest.class);

            Optional<Usuario> userOpt = repository.findByEmail(req.email);

            if (!userOpt.isPresent() || !BCrypt.checkpw(req.senha, userOpt.get().getSenhaHash())) {
                sendJsonResponse(exchange, 401, "{\"erro\":\"E-mail ou senha inválidos\"}");
                return;
            }

            Usuario user = userOpt.get();
            String token = jwtService.generateToken(user);

            // Retorna o token JSON
            sendJsonResponse(exchange, 200, "{\"token\":\"" + token + "\"}");

        } catch (Exception e) {
            e.printStackTrace();
            sendJsonResponse(exchange, 500, "{\"erro\":\"Erro interno no servidor\"}");
        }
    }

    // Gerencia o processo de Cadastro (Registro)
    public void handleRegister(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        try {
            InputStream is = exchange.getRequestBody();
            RegisterRequest req = mapper.readValue(is, RegisterRequest.class);

            Optional<Usuario> existente = repository.findByEmail(req.email);
            if (existente.isPresent()) {
                sendJsonResponse(exchange, 400, "{\"erro\":\"E-mail já cadastrado\"}");
                return;
            }

            String senhaHash = BCrypt.hashpw(req.senha, BCrypt.gensalt(12));

            Usuario novoUsuario = new Usuario(req.nome, req.email, senhaHash, req.role != null ? req.role : "USER");
            repository.save(novoUsuario);

            sendJsonResponse(exchange, 201, "{\"mensagem\":\"Usuario cadastrado com sucesso\"}");

        } catch (Exception e) {
            e.printStackTrace();
            sendJsonResponse(exchange, 400, "{\"erro\":\"Dados invalidos ou ja cadastrados\"}");
        }
    }
}