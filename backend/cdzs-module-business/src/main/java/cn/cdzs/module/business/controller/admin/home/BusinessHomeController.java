package cn.cdzs.module.business.controller.admin.home;

import cn.cdzs.framework.common.pojo.CommonResult;
import cn.cdzs.module.business.controller.admin.home.vo.BusinessHomeRespVO;
import cn.cdzs.module.business.service.home.BusinessHomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.cdzs.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 业务模块")
@RestController
@RequestMapping("/business/home")
@RequiredArgsConstructor
public class BusinessHomeController {

    private final BusinessHomeService businessHomeService;

    @GetMapping("/get")
    @Operation(summary = "获取业务模块说明")
    @PreAuthorize("@ss.hasPermission('business:home:query')")
    public CommonResult<BusinessHomeRespVO> getHome() {
        return success(businessHomeService.getHome());
    }
}
