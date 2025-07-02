package cn.com.undefined.abdap_backend.util;

import cn.com.undefined.abdap_backend.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 响应工具类
 * 简化Controller层的响应构建
 */
public class ResponseUtil {
    
    /**
     * 成功响应
     */
    public static <T> ResponseEntity<ApiResponse<T>> success(T data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }
    
    /**
     * 成功响应（自定义消息）
     */
    public static <T> ResponseEntity<ApiResponse<T>> success(String message, T data) {
        return ResponseEntity.ok(ApiResponse.success(message, data));
    }
    
    /**
     * 创建响应（201 Created）
     */
    public static <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("创建成功", data));
    }
    
    /**
     * 创建响应（201 Created，自定义消息）
     */
    public static <T> ResponseEntity<ApiResponse<T>> created(String message, T data) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(message, data));
    }
    
    /**
     * 客户端错误响应（400 Bad Request）
     */
    public static <T> ResponseEntity<ApiResponse<T>> badRequest(String message) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, message));
    }
    
    /**
     * 未授权响应（401 Unauthorized）
     */
    public static <T> ResponseEntity<ApiResponse<T>> unauthorized(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(401, message));
    }
    
    /**
     * 服务器错误响应（500 Internal Server Error）
     */
    public static <T> ResponseEntity<ApiResponse<T>> internalError(String message) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, message));
    }
    
    /**
     * 自定义状态码响应
     */
    public static <T> ResponseEntity<ApiResponse<T>> status(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(ApiResponse.error(status.value(), message));
    }
}
