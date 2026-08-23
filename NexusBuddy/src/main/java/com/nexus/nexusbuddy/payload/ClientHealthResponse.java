package com.nexus.nexusbuddy.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientHealthResponse {
    private Long clientConfigId;
    private String clientName;
    private Boolean isActive;
    private Long toolCount;
    private Long activeToolCount;
    private Long requestsLast24h;
    private Long errorsLast24h;
    private Double errorRateLast24h;
    private String lastRequestTime;
    private Boolean healthCheckStatus;
    private String connectionUrl;
    private String healthCheckPath;
}