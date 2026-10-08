package com.smartcity360.dto;

import com.smartcity360.model.User;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OfficerResponse {
    Long id;
    String name;
    String email;
    String department;
    String ward;

    public static OfficerResponse from(User user) {
        return OfficerResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .department(user.getDepartment())
                .ward(user.getWard())
                .build();
    }
}