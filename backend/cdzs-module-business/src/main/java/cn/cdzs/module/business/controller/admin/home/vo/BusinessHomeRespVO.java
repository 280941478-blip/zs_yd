package cn.cdzs.module.business.controller.admin.home.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Schema(description = "管理后台 - 业务模块说明 Response VO")
@Data
@Builder
public class BusinessHomeRespVO {
    @Schema(description = "模块名称")
    private String name;
    @Schema(description = "模块说明")
    private String description;
}
