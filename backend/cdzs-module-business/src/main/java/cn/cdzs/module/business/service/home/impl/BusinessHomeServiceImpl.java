package cn.cdzs.module.business.service.home.impl;

import cn.cdzs.module.business.controller.admin.home.vo.BusinessHomeRespVO;
import cn.cdzs.module.business.service.home.BusinessHomeService;
import org.springframework.stereotype.Service;

/** 此处只提供模块说明；后续业务校验、事务和持久化编排放在各自的 ServiceImpl 中。 */
@Service
public class BusinessHomeServiceImpl implements BusinessHomeService {
    @Override
    public BusinessHomeRespVO getHome() {
        return BusinessHomeRespVO.builder()
                .name("业务模块")
                .description("项目业务统一在 business 目录中开发，按具体业务领域继续分包。")
                .build();
    }
}
