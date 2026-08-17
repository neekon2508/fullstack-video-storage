package com.api.common.model;


import com.api.common.constant.CommonConstants;
import com.api.common.exception.ErrorCode;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CommonResponse<T> {

    @Schema(example = CommonConstants.YES_FLAG)
    private String successOrNot;

    private String statusCode;

    private T data;

    public static <T> CommonResponse<T> success(T data) {
        return CommonResponse.<T>builder()
                .successOrNot(CommonConstants.YES_FLAG)
                .statusCode(ErrorCode.SUCCESS.getCode())
                .data(data)
                .build();
    }
}
