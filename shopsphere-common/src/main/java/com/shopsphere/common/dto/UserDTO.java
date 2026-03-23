package com.shopsphere.common.dto;

import com.shopsphere.common.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private Long id;
    private String name;
    private String email;
    private Role role;
    private boolean active;
    private LocalDateTime createdAt;
}