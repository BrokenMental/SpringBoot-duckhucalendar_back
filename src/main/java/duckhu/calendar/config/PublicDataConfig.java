package duckhu.calendar.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * 공공데이터 API 설정
 */
@Configuration
public class PublicDataConfig {

    @Value("${public-data.api.holiday.base-url}")
    private String holidayApiBaseUrl;

    @Value("${public-data.api.holiday.service-key}")
    private String serviceKey;

    @Value("${public-data.api.holiday.timeout:10000}")
    private int timeout;

    @Value("${external.api.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${external.api.retry.delay:2000}")
    private long retryDelay;

    /**
     * 공공데이터 API 전용 RestTemplate 빈
     */
    @Bean(name = "publicDataRestTemplate")
    public RestTemplate publicDataRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);

        RestTemplate restTemplate = new RestTemplate(factory);

        // 에러 핸들러 추가
        restTemplate.setErrorHandler(new PublicDataResponseErrorHandler());

        return restTemplate;
    }

    /**
     * Getter 메서드들
     */
    public String getHolidayApiBaseUrl() {
        return holidayApiBaseUrl;
    }

    public String getServiceKey() {
        return serviceKey;
    }

    public int getTimeout() {
        return timeout;
    }

    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public long getRetryDelay() {
        return retryDelay;
    }
}
