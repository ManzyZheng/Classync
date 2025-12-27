package com.classync.service;

import com.classync.entity.User;
import com.classync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    
    public User login(String account, String name) {
        Optional<User> existingUser = userRepository.findByAccount(account);
        if (existingUser.isPresent()) {
            return existingUser.get();
        }
        
        User newUser = new User();
        newUser.setAccount(account);
        newUser.setName(name);
        return userRepository.save(newUser);
    }
    
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
}

