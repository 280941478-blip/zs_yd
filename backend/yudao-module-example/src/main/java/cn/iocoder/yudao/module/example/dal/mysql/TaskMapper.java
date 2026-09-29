package cn.iocoder.yudao.module.example.dal.mysql;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.example.dal.dataobject.TaskDO;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface TaskMapper extends BaseMapperX<TaskDO> {}
