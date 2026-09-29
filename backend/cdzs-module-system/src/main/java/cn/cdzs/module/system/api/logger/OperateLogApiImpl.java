package cn.cdzs.module.system.api.logger;

import cn.cdzs.framework.common.pojo.PageResult;
import cn.cdzs.framework.common.util.object.BeanUtils;
import cn.cdzs.framework.common.biz.system.logger.dto.OperateLogCreateReqDTO;
import cn.cdzs.module.system.api.logger.dto.OperateLogPageReqDTO;
import cn.cdzs.module.system.api.logger.dto.OperateLogRespDTO;
import cn.cdzs.module.system.dal.dataobject.logger.OperateLogDO;
import cn.cdzs.module.system.service.logger.OperateLogService;
import org.dromara.core.trans.anno.TransMethodResult;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * 操作日志 API 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class OperateLogApiImpl implements OperateLogApi {

    @Resource
    private OperateLogService operateLogService;

    @Override
    public void createOperateLog(OperateLogCreateReqDTO createReqDTO) {
        operateLogService.createOperateLog(createReqDTO);
    }

    @Override
    @TransMethodResult
    public PageResult<OperateLogRespDTO> getOperateLogPage(OperateLogPageReqDTO pageReqDTO) {
        PageResult<OperateLogDO> operateLogPage = operateLogService.getOperateLogPage(pageReqDTO);
        return BeanUtils.toBean(operateLogPage, OperateLogRespDTO.class);
    }

}
