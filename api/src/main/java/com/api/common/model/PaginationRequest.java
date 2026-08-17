package com.api.common.model;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

import com.api.common.constant.CommonConstants;
import com.api.common.util.ValidateUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
public class PaginationRequest {

    @Builder.Default
    @Positive
    @Schema(description = "pageSize(default: 10)", example = "10")
    @JsonProperty("page_size")
    private Integer pageSize = 10;

    @Builder.Default
    @PositiveOrZero
    @Schema(description = "page(default: 0)", example = "0")
    private Integer page = 0;

    @JsonProperty("sorts")
    private List<SortRequest> sorts;

    @JsonIgnore
    public Pageable toPageable() {
        int validPage = (page != null && page >= 0) ? page : 0;
        int validSize = (pageSize != null && pageSize > 0) ? pageSize : 10;
        
        if (!ValidateUtil.isEmpty(sorts)) {
            List<Sort.Order> orders = sorts.stream()
                .filter(s->StringUtils.hasText(s.getField()))
                .map(s->new Sort.Order(
                    CommonConstants.ASC_FLAG.equalsIgnoreCase(s.getType())
                    ? Sort.Direction.ASC : Sort.DEFAULT_DIRECTION.DESC,
                    s.getField()
                ))
                .toList();
            return PageRequest.of(validPage, validSize, Sort.by(orders));
        }
        return PageRequest.of(validPage, validSize);
    }
}
