package cn.cdzs.module.business.service.home;

import cn.cdzs.module.business.controller.admin.home.vo.BusinessHomeRespVO;

/** 业务模块入口服务。Controller 依赖接口，不依赖具体实现。 */
public interface BusinessHomeService {
    BusinessHomeRespVO getHome();
}
