package cn.cdzs.module.example.service.impl;

import cn.cdzs.module.example.service.TaskService;

import cn.cdzs.framework.common.exception.ErrorCode;
import cn.cdzs.framework.common.pojo.PageResult;
import cn.cdzs.framework.common.util.object.BeanUtils;
import cn.cdzs.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.cdzs.framework.security.core.util.SecurityFrameworkUtils;
import cn.cdzs.module.example.controller.admin.task.vo.*;
import cn.cdzs.module.example.dal.dataobject.TaskDO;
import cn.cdzs.module.example.dal.mysql.TaskMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static cn.cdzs.framework.common.exception.util.ServiceExceptionUtil.exception;

/** Every operation is scoped to the authenticated owner AND the framework tenant interceptor. */
@Service @RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {
    private final TaskMapper mapper;
    private final Validator validator;
    private static final ErrorCode NOT_FOUND=new ErrorCode(1_900_001_001,"任务不存在或无权访问");
    private static final ErrorCode INVALID=new ErrorCode(1_900_001_002,"{} ");
    private static final ErrorCode DUPLICATE=new ErrorCode(1_900_001_003,"任务编码已存在");
    private Long owner() { return Objects.requireNonNull(SecurityFrameworkUtils.getLoginUserId(),"Authentication required"); }
    private LambdaQueryWrapperX<TaskDO> scope() {
        return new LambdaQueryWrapperX<TaskDO>().eq(TaskDO::getOwnerId,owner());
    }
    private void validate(TaskSaveReqVO request) {
        var violations=validator.validate(request);
        if(!violations.isEmpty()) throw exception(INVALID,violations.iterator().next().getMessage());
    }
    @Override
    public TaskDO get(Long id) {
        TaskDO row=mapper.selectOne(scope().eq(TaskDO::getId,id));
        if(row==null) throw exception(NOT_FOUND);
        return row;
    }
    @Override
    public PageResult<TaskDO> page(TaskPageReqVO request) {
        if(request.getPageSize()<1) throw exception(INVALID,"分页大小必须大于0");
        return mapper.selectPage(request,filter(request));
    }
    private LambdaQueryWrapperX<TaskDO> filter(TaskPageReqVO r) {
        return scope().likeIfPresent(TaskDO::getName,r.getName()).likeIfPresent(TaskDO::getCode,r.getCode())
                .eqIfPresent(TaskDO::getStatus,r.getStatus()).orderByDesc(TaskDO::getId);
    }
    @Override
    public List<TaskDO> export(TaskPageReqVO request) {
        var rows=mapper.selectList(filter(request).last("LIMIT 5001"));
        if(rows.size()>5000) throw exception(INVALID,"导出最多5000条，请缩小筛选范围");
        return rows;
    }
    @Transactional(rollbackFor=Exception.class)
    @Override
    public Long create(TaskSaveReqVO request) {
        validate(request);
        TaskDO row=BeanUtils.toBean(request,TaskDO.class);
        row.setId(null); row.setOwnerId(owner());
        try { mapper.insert(row); } catch(DuplicateKeyException e) { throw exception(DUPLICATE); }
        return row.getId();
    }
    @Transactional(rollbackFor=Exception.class)
    @Override
    public void update(TaskSaveReqVO request) {
        validate(request);
        if(request.getId()==null) throw exception(INVALID,"修改时必须提供id");
        get(request.getId());
        var update=new LambdaUpdateWrapper<TaskDO>().eq(TaskDO::getId,request.getId()).eq(TaskDO::getOwnerId,owner())
                .set(TaskDO::getCode,request.getCode()).set(TaskDO::getName,request.getName())
                .set(TaskDO::getStatus,request.getStatus()).set(TaskDO::getRemark,request.getRemark());
        try { if(mapper.update(new TaskDO(),update)!=1) throw exception(NOT_FOUND); }
        catch(DuplicateKeyException e) { throw exception(DUPLICATE); }
    }
    @Transactional(rollbackFor=Exception.class)
    @Override
    public void delete(Long id) {
        if(mapper.delete(scope().eq(TaskDO::getId,id))!=1) throw exception(NOT_FOUND);
    }
    @Transactional(rollbackFor=Exception.class)
    @Override
    public int importRows(List<TaskExcelVO> rows) {
        if(rows.isEmpty() || rows.size()>1000) throw exception(INVALID,"导入须为1至1000行");
        Set<String> codes=new HashSet<>();
        for(int i=0;i<rows.size();i++) {
            var request=BeanUtils.toBean(rows.get(i),TaskSaveReqVO.class);
            validate(request);
            if(!codes.add(request.getCode().toLowerCase(Locale.ROOT))) throw exception(INVALID,"导入文件存在重复编码");
        }
        for(var row:rows) create(BeanUtils.toBean(row,TaskSaveReqVO.class));
        return rows.size(); // duplicate or invalid input rolls back the entire import
    }
}
