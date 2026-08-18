package com.api.common.exception;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.api.common.constant.CommonConstants;
import com.api.common.model.CommonResponse;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class RestControllerExceptionAdvice {

        @ExceptionHandler(BusinessException.class)
        public ResponseEntity<String> customBusinessExceptionHandler(InvalidRefreshTokenException e) {
                log.error("[customBusinessExceptionHandler]" + e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }


        @ExceptionHandler(BusinessException.class)
        public ResponseEntity<CommonResponse<?>> customBusinessExceptionHandler(BusinessException e) {
                log.error("[customBusinessExceptionHandler]" + e.getMessage(), e);

                return new ResponseEntity<>(CommonResponse.builder()
                                .successOrNot(CommonConstants.NO_FLAG)
                                .statusCode(e.getStatusCode())
                                .data(e.getMessage())
                                .build(), HttpStatus.OK);
        }

        @ExceptionHandler({
                        ConstraintViolationException.class,
                        MethodArgumentTypeMismatchException.class,
                        HttpMessageNotReadableException.class
        })
        public ResponseEntity<CommonResponse<?>> constraintViolationExceptionHandler(Exception e) {
                log.warn("[constraintViolationExceptionHandler]" + e.getMessage());

                return new ResponseEntity<>(CommonResponse.builder()
                                .successOrNot(CommonConstants.NO_FLAG)
                                .statusCode(ErrorCode.PARAMETER_VALUE_ERROR.getCode())
                                .data("EXCEPTION")
                                .build(), HttpStatus.OK);
        }

        @ExceptionHandler({
                        MissingServletRequestParameterException.class,
        })
        public ResponseEntity<CommonResponse<?>> missingServletRequestParameterExceptionHandler(Exception e) {
                log.error("[missingServletRequestParameterExceptionHandler]" + e.getMessage(), e);

                return new ResponseEntity<>(CommonResponse.builder()
                                .successOrNot(CommonConstants.NO_FLAG)
                                .statusCode(ErrorCode.MANDATORY_PARAM_ERROR.getCode())
                                .build(), HttpStatus.OK);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<CommonResponse<?>> parameterValidationExceptionHandler(
                        MethodArgumentNotValidException e) {
                log.debug("[parameterValidationExceptionHandler] " + e.getMessage(), e);

                StringBuilder mandatoryParameterStringBuilder = new StringBuilder();
                List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
                for (int i = 0; i < fieldErrors.size();) {

                        mandatoryParameterStringBuilder.append(fieldErrors.get(i).getField());

                        if (++i < fieldErrors.size())
                                mandatoryParameterStringBuilder.append(",");
                }
                return new ResponseEntity<>(CommonResponse.builder()
                                .successOrNot(CommonConstants.NO_FLAG)
                                .statusCode(ErrorCode.MANDATORY_PARAM_ERROR.getCode())
                                .data(mandatoryParameterStringBuilder.toString())
                                .build(), HttpStatus.OK);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<CommonResponse<?>> customExceptionHandler(Exception e) {
                log.error("[customExceptionHandler]" + e.getMessage(), e);
                return new ResponseEntity<>(CommonResponse.builder()
                                .successOrNot(CommonConstants.NO_FLAG)
                                .statusCode(ErrorCode.INTERNAL_SERVER_ERROR.getCode())
                                .data(e.getClass().getSimpleName())
                                .build(), HttpStatus.OK);
        }
}
