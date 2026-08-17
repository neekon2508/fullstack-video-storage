package com.api.common.util;

import java.util.List;
import java.util.Map;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ValidateUtil {

    public static boolean isEmpty(final Object str) {
        if (str == null) {
            return true;
        }
        if ((str instanceof String) && (((String) str).trim().length() == 0)) {
            return true;
        }
        if (str instanceof Map) {
            return ((Map<?, ?>) str).isEmpty();
        }
        if (str instanceof List) {
            return ((List<?>) str).isEmpty();
        }
        if (str instanceof Object[]) {
            return (((Object[]) str).length == 0);
        }
        return false;
    }

}
