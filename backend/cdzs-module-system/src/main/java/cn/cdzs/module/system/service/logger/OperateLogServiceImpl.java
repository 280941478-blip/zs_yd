package cn.cdzs.module.system.service.logger;

import cn.cdzs.framework.common.pojo.PageResult;
import cn.cdzs.framework.common.util.object.BeanUtils;
import cn.cdzs.framework.common.biz.system.logger.dto.OperateLogCreateReqDTO;
import cn.cdzs.module.system.api.logger.dto.OperateLogPageReqDTO;
import cn.cdzs.module.system.controller.admin.logger.vo.operatelog.OperateLogPageReqVO;
import cn.cdzs.module.system.dal.dataobject.logger.OperateLogDO;
import cn.cdzs.module.system.dal.mysql.logger.OperateLogMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * 操作日志 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class OperateLogServiceImpl implements OperateLogService {

    @Resource
    private OperateLogMapper operateLogMapper;

    @Override
    public void createOperateLog(OperateLogCreateReqDTO createReqDTO) {
        OperateLogDO log = BeanUtils.toBean(createReqDTO, OperateLogDO.class);
        operateLogMapper.insert(log);
    }

    @Override
    public OperateLogDO getOperateLog(Long id) {
        return operateLogMapper.selectById(id);
    }

    @Override
    public PageResult<OperateLogDO> getOperateLogPage(OperateLogPageReqVO pageReqVO) {
        return operateLogMapper.selectPage(pageReqVO);
    }

    @Override
    public PageResult<OperateLogDO> getOperateLogPage(OperateLogPageReqDTO pageReqDTO) {
        return operateLogMapper.selectPage(pageReqDTO);
    }

}
