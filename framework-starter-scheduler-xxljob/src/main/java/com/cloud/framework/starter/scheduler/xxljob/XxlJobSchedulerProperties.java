package com.cloud.framework.starter.scheduler.xxljob;

import com.cloud.framework.core.naming.Namespaced;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
public class XxlJobSchedulerProperties implements Namespaced {
    private boolean enabled;

    private String adminAddresses;

    private String accessToken;

    @NotNull
    @Min(1)
    @Max(10)
    private Integer timeout = 3;

    private String namespace;

    private String address;

    private String ip;

    @NotNull
    @Positive
    private Integer port = 9999;

    private String logPath = "logs/xxl-job/jobhandler";

    @NotNull
    @Min(3)
    private Integer logRetentionDays = 30;

    @AssertTrue(message = "scheduler.xxljob.admin-addresses must be set when enabled is true")
    public boolean isAdminAddressesValid() {
        return !this.enabled || (this.adminAddresses != null && !this.adminAddresses.isBlank());
    }
}
