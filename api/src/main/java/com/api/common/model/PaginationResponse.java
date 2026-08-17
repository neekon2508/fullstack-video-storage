package com.api.common.model;

import java.util.List;

import org.springframework.data.domain.Page;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaginationResponse<T> {

    @Schema(description = "Tổng số bản ghi", example = "100")
    private Long totalCount;

    @Schema(description = "Tổng số trang", example = "10")
    private Integer totalPages;

    @Schema(description = "Trang hiện tại (0-indexed)", example = "0")
    private Integer page;

    @Schema(description = "Kích thước trang", example = "10")
    private Integer pageSize;

    @Schema(description = "Danh sách dữ liệu")
    private List<T> list;

    public PaginationResponse(List<T> list) {
        this.list = list;
    }

    public static <T> PaginationResponse<T> from(Page<T> page) {
        return PaginationResponse.<T>builder()
                .totalCount(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .page(page.getNumber())
                .pageSize(page.getSize())
                .list(page.getContent())
                .build();
    }

}
