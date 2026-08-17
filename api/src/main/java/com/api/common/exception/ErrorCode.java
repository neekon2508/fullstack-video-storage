package com.api.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

        // Success & General Fail
        SUCCESS("SUCCESS", "Thành công", HttpStatus.OK),
        FAIL("FAIL", "Thao tác thất bại", HttpStatus.BAD_REQUEST),

        // System & Validation
        APPROVAL_EXCEPTION("APPROVAL_EXCEPTION", "Lỗi trong quá trình phê duyệt", HttpStatus.BAD_REQUEST),
        MANDATORY_PARAM_ERROR("MANDATORY_PARAM_ERROR", "Thiếu tham số bắt buộc", HttpStatus.BAD_REQUEST),
        PARAMETER_VALUE_ERROR("PARAMETER_VALUE_ERROR", "Giá trị tham số không hợp lệ", HttpStatus.BAD_REQUEST),
        DUPLICATED_VALUE_ERROR("DUPLICATED_VALUE_ERROR", "Dữ liệu bị trùng lặp", HttpStatus.BAD_REQUEST),
        EXPECTATION_FAILED("EXPECTATION_FAILED", "Yêu cầu không đạt kỳ vọng xử lý", HttpStatus.EXPECTATION_FAILED),
        INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "Lỗi hệ thống nội bộ, vui lòng thử lại sau",
                        HttpStatus.INTERNAL_SERVER_ERROR),

        // Security & Auth
        AUTH_ERROR("AUTH_ERROR", "Lỗi xác thực người dùng", HttpStatus.UNAUTHORIZED),
        JWT_EXPIRED("JWT_EXPIRED", "Phiên làm việc đã hết hạn", HttpStatus.UNAUTHORIZED),
        INVALID_TOKEN("INVALID_TOKEN", "Token không hợp lệ", HttpStatus.UNAUTHORIZED),

        // Common DB
        DUPLICATE_KEY_EXCEPTION("DUPLICATE_KEY_EXCEPTION", "Khóa dữ liệu đã tồn tại trong hệ thống",
                        HttpStatus.BAD_REQUEST),

        // Session & User
        USER_NOT_FOUND("USER_NOT_FOUND", "Không tìm thấy thông tin người dùng", HttpStatus.NOT_FOUND),

        USER_UPDATE_FAIL("USER_UPDATE_FAIL", "Update user thất bại", HttpStatus.OK),
        CONTINUOUS_LOGIN_FAILURE("CONTINUOUS_LOGIN_FAILURE", "Đăng nhập thất bại quá số lần quy định",
                        HttpStatus.BAD_REQUEST),

        LOGIN_LOCKED("LOGIN_LOCKED", "Tài khoản tạm thời bị khóa", HttpStatus.FORBIDDEN),
        SESSION_EXPIRE("SESSION_EXPIRE", "Phiên đăng nhập đã hết hạn", HttpStatus.UNAUTHORIZED),
        NOT_AUTHORIZED_EXCEPTION("NOT_AUTHORIZED_EXCEPTION", "Bạn không có quyền thực hiện thao tác này",
                        HttpStatus.FORBIDDEN),
        NO_ROLE_ASSIGNED("NO_ROLE_ASSIGNED", "Tài khoản chưa được phân quyền", HttpStatus.FORBIDDEN),

        // Role
        DELETION_IMPOSSIBLE("DELETION_IMPOSSIBLE", "Không thể xóa dữ liệu này do đang có ràng buộc",
                        HttpStatus.BAD_REQUEST),
        ROLE_NOT_FOUND("ROLE_NOT_FOUND", "Không tìm thấy nhóm người dùng", HttpStatus.NOT_FOUND),
        // Shop
        DUPLICATE_SHOP_EXCEPTION("DUPLICATE_SHOP_EXCEPTION", "Cửa hàng đã tồn tại", HttpStatus.BAD_REQUEST),

        // Coupon
        COUPON_NOT_FOUND("COUPON_NOT_FOUND", "Không tìm thấy coupon", HttpStatus.NOT_FOUND),

        // Customer
        CUSTOMER_NOT_FOUND("CUSTOMER_NOT_FOUND", "Không tìm thấy khách hàng", HttpStatus.NOT_FOUND),

        CUSTOMER_ALREADY_EXIST("CUSTOMER_ALREADY_EXIST", "Khách hàng đã tồn tại", HttpStatus.BAD_REQUEST),

        // Product
        PRODUCT_NOT_FOUND("PRODUCT_NOT_FOUND", "Không tìm thấy sản phẩm", HttpStatus.NOT_FOUND),

        // Category
        CATEGORY_NOT_FOUND("CATEGORY_NOT_FOUND", "Không tìm thấy nhóm sản phẩm", HttpStatus.NOT_FOUND),

        // Excel
        FILE_NOT_FOUND("FILE_NOT_FOUND", "Không tìm thấy file", HttpStatus.BAD_REQUEST),
        FILE_INVALID_FORMAT("FILE_INVALID_FORMAT", "File không đúng định dạng", HttpStatus.BAD_REQUEST),
        FILE_INVALID_NAME("FILE_INVALID_NAME", "Tên file không hợp lệ", HttpStatus.BAD_REQUEST),

        FILE_READ_FAIL("FILE_READ_FAIL", "Đọc file thất bại", HttpStatus.BAD_REQUEST),
        // Bbs / Common Content
        FILE_WRITE_FAIL("FILE_WRITE_FAIL", "Ghi file thất bại", HttpStatus.BAD_REQUEST),

        NOT_EXIST_EXCEPTION("NOT_EXIST_EXCEPTION", "Dữ liệu yêu cầu không tồn tại", HttpStatus.NOT_FOUND),

        // File Upload / Download
        FILE_UPLOAD_FAILED("FILE_UPLOAD_FAILED", "Tải tệp lên thất bại", HttpStatus.BAD_REQUEST),
        FILE_DOWNLOAD_FAILED("FILE_DOWNLOAD_FAILED", "Tải tệp xuống thất bại", HttpStatus.BAD_REQUEST),

        // Translated Message
        MESSAGE_DEPLOY_FAILED("MESSAGE_DEPLOY_FAILED", "Triển khai bản dịch thất bại", HttpStatus.BAD_REQUEST),
        MESSAGE_READ_FAILED("MESSAGE_READ_FAILED", "Đọc thông tin bản dịch thất bại", HttpStatus.BAD_REQUEST),

        // Security / XSS
        XSS_FORBIDDEN_STRING_INCLUDE("XSS_FORBIDDEN_STRING_INCLUDE", "Dữ liệu chứa ký tự không an toàn (XSS)",
                        HttpStatus.BAD_REQUEST),

        ENCRYPT_FAIL("ENCRYPT_FAIL", "Encrypt thất bại", HttpStatus.BAD_REQUEST),

        DECRYPT_FAIL("DECRYPT_FAIL", "Decrypt thất bại", HttpStatus.BAD_REQUEST);

        private final String code;
        private final String message;
        private final HttpStatus httpStatus;

        ErrorCode(String code, String message, HttpStatus httpStatus) {
                this.code = code;
                this.message = message;
                this.httpStatus = httpStatus;
        }
}