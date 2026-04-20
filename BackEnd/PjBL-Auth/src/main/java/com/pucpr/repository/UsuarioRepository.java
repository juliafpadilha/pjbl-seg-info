package com.pucpr.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pucpr.model.Usuario;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UsuarioRepository {
    private final String FILE_PATH = "usuarios.json";
    private final ObjectMapper mapper = new ObjectMapper();

   
    public Optional<Usuario> findByEmail(String email) {
        List<Usuario> usuarios = findAll();
        // 2 & 3. Utilizamos Stream e filter p/ achar o registro de email igualitário (sem choro com caps lock)
        return usuarios.stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst(); // 4. Retorna automaticamente Optional<Usuario> se achar ou empty.
    }

    /**
     * Retorna todos os usuários cadastrados no arquivo JSON.
     */
    public List<Usuario> findAll() {
        File arquivo = new File(FILE_PATH);
        if (!arquivo.exists()) {
            return new ArrayList<>(); 
        }
        
        try {
            // 3. Mapping dinâmico TypeReference do Jackson.
            return mapper.readValue(arquivo, new TypeReference<List<Usuario>>(){});
        } catch (IOException e) {
            // Em caso de falha de leitura ou corrupção do JSON:
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Salva um novo usuário no arquivo JSON.
     */
    public void save(Usuario usuario) throws IOException {
        // 1. Obter lista preexistente
        List<Usuario> usuarios = findAll();
        
        boolean existe = usuarios.stream()
                .anyMatch(u -> u.getEmail().equalsIgnoreCase(usuario.getEmail()));
                
        if (existe) {
            // Interrompe imediatamente para nem tentar o cadastro
            throw new IllegalArgumentException("E-mail já cadastrado");
        }

        // 3. Adicionar
        usuarios.add(usuario);
        
        // 4. Salvar via default pretty printer (JSON formatado ao invez de em linha unica)
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(FILE_PATH), usuarios);
    }
}