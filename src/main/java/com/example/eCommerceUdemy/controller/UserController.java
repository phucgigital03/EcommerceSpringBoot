package com.example.eCommerceUdemy.controller;

import com.example.eCommerceUdemy.model.AppRole;
import com.example.eCommerceUdemy.model.Role;
import com.example.eCommerceUdemy.model.User;
import com.example.eCommerceUdemy.payload.SignUpMethodResponse;
import com.example.eCommerceUdemy.payload.UsersResponse;
import com.example.eCommerceUdemy.repository.RoleRepository;
import com.example.eCommerceUdemy.security.request.SignupRequest;
import com.example.eCommerceUdemy.security.response.MessageResponse;
import com.example.eCommerceUdemy.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final RoleRepository roleRepository;
    private final UserService userService;

    public UserController(RoleRepository roleRepository, UserService userService) {
        this.roleRepository = roleRepository;
        this.userService = userService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/roles")
    public ResponseEntity<?> getRoles() {
        List<Role> roles = roleRepository.findAll();
        if(roles.isEmpty()){
            return ResponseEntity.badRequest().body(new MessageResponse("No roles found!"));
        }
        List<Role> filterRoles = roles.stream()
                .filter(role -> !role.getRoleName().equals(AppRole.ROLE_ADMIN))
                .collect(Collectors.toList());
        return ResponseEntity.ok().body(filterRoles);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("")
    public ResponseEntity<?> getAllUsers() {
        List<UsersResponse> users = userService.findAllUsers();
        return ResponseEntity.ok().body(users);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<?> updateUserWithoutPassWordUser(
            @PathVariable Long userId,
            @Valid @RequestBody SignupRequest signUpRequest
    ) {
        String updatedUser = userService.updatedUser(userId,signUpRequest);
        return new ResponseEntity<>(updatedUser, HttpStatus.OK);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(
            @PathVariable Long userId
    ) {
        String deletedUser = userService.deletedUser(userId);
        return ResponseEntity.ok().body(deletedUser);
    }

    @GetMapping("/registerMethod")
    public ResponseEntity<?> getUserRegisterMethod() {
        List<SignUpMethodResponse> signUpMethodResponses = userService.getUserRegisterMethod();
        return ResponseEntity.ok().body(signUpMethodResponses);
    }
}
