package com.api.common.util;

import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.api.common.exception.BusinessException;
import com.api.common.exception.ErrorCode;


@Component
public class AesUtil {
    @Value("${security.algorithm}")
    String algorithm;

    @Value("${security.secret-key}")
    String secretKey;

    public String encrypt(String data) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), algorithm);
            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            byte[] encryptedBytes = cipher.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch(Exception e) {
            throw new BusinessException(ErrorCode.ENCRYPT_FAIL.getMessage(), ErrorCode.ENCRYPT_FAIL.getCode());
        }
    }

    public String decrypt(String encryptedData) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), algorithm);
            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
            return new String(decryptedBytes);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.DECRYPT_FAIL.getMessage(), ErrorCode.DECRYPT_FAIL.getCode());
        }
    }
}
