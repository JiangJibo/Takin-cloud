package io.shulie.takin.cloud.sdk.model.response.report;

import lombok.Data;

/**
 * TODO
 *
 * @author 张天赐
 */
@Data
public class MetricesResponse {
    /**
     * 时间
     */
    private Long time;

    /**
     * tps
     */
    private Double avgTps;
}
