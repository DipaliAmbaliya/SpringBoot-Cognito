package com.simform.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AttributeVerificationDto {
    private String attributeName;
    private String attributeValue;
    private String email;
    private String code;
}
