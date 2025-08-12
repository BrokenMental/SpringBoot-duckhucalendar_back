package duckhu.calendar.config;

import duckhu.calendar.exception.PublicDataApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;

import java.io.IOException;
import java.net.URI;

@Slf4j
public class PublicDataResponseErrorHandler extends DefaultResponseErrorHandler {

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        int statusCode = response.getStatusCode().value();
        return statusCode >= 400; // 4xx, 5xx 에러
    }

    @Override
    public void handleError(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
        int statusCodeValue = response.getStatusCode().value();
        HttpStatus status = HttpStatus.resolve(statusCodeValue);

        if (status == null) {
            throw new PublicDataApiException("알 수 없는 HTTP 상태 코드: " + statusCodeValue, statusCodeValue);
        }

        String urlInfo = url != null ? " URL: " + url : "";
        String methodInfo = method != null ? " Method: " + method : "";

        log.warn("공공데이터 API 호출 에러 - 상태코드: {}, 메시지: {}{}{}",
                status.value(), status.getReasonPhrase(), urlInfo, methodInfo);

        String errorMessage = getErrorMessage(status);
        throw new PublicDataApiException(errorMessage, status.value());
    }

    private String getErrorMessage(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "잘못된 요청입니다. API 파라미터를 확인해주세요.";
            case UNAUTHORIZED -> "인증에 실패했습니다. 서비스 키를 확인해주세요.";
            case FORBIDDEN -> "접근이 금지되었습니다. API 사용 권한을 확인해주세요.";
            case NOT_FOUND -> "요청한 리소스를 찾을 수 없습니다.";
            case TOO_MANY_REQUESTS -> "API 호출 한도를 초과했습니다. 잠시 후 다시 시도해주세요.";
            case INTERNAL_SERVER_ERROR -> "공공데이터 서버 내부 오류입니다.";
            case SERVICE_UNAVAILABLE -> "공공데이터 서비스를 사용할 수 없습니다.";
            case GATEWAY_TIMEOUT -> "공공데이터 서버 응답 시간이 초과되었습니다.";
            default -> "공공데이터 API 호출 중 오류가 발생했습니다. (상태코드: " + status.value() + ")";
        };
    }
}
