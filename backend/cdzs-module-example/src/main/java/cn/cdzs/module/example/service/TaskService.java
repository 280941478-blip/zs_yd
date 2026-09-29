package cn.cdzs.module.example.service;

import cn.cdzs.framework.common.pojo.PageResult;
import cn.cdzs.module.example.controller.admin.task.vo.TaskExcelVO;
import cn.cdzs.module.example.controller.admin.task.vo.TaskPageReqVO;
import cn.cdzs.module.example.controller.admin.task.vo.TaskSaveReqVO;
import cn.cdzs.module.example.dal.dataobject.TaskDO;
import java.util.List;

/** 示例任务服务接口：具体业务规则和事务位于 TaskServiceImpl。 */
public interface TaskService {
    TaskDO get(Long id);
    PageResult<TaskDO> page(TaskPageReqVO request);
    List<TaskDO> export(TaskPageReqVO request);
    Long create(TaskSaveReqVO request);
    void update(TaskSaveReqVO request);
    void delete(Long id);
    int importRows(List<TaskExcelVO> rows);
}
