package backend.auth.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.api.auth.dto.LoginRequest;
import com.api.auth.dto.LoginResponse;
import com.api.auth.service.AuthenService;
import com.api.common.constant.CommonConstants;
import com.api.common.model.CommonResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication API")
@RequestMapping()
public class AuthController {

    private final AuthenService authService;

    @Operation(summary = "Đăng nhập")
    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CommonResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(CommonResponse.success(authService.login(req)));
    }

    @Operation(summary = "Đăng xuất")
    @PostMapping(value = "/logout")
    public ResponseEntity<CommonResponse<String>> logout() {
        return ResponseEntity.ok(
                CommonResponse.success(authService.logout()));
    }

    // @Operation(summary = "Quên mật khẩu")
    // @GetMapping(value = "/forgotPassword/{username}")
    // public ResponseEntity<CommonResponse<String>>
    // forgotPassword(@PathVariable("username") String username) {
    // return ResponseEntity.ok(
    // CommonResponse.<String>builder()
    // .successOrNot(CommonConstants.YES_FLAG)
    // .statusCode(ErrorCode.SUCCESS.getCode())
    // .data(authService.handleForgotPassword(username))
    // .build());
    // }
}
