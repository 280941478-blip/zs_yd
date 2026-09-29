package cn.cdzs.module.example.dal.mysql;
import cn.cdzs.framework.mybatis.core.mapper.BaseMapperX;
import cn.cdzs.module.example.dal.dataobject.TaskDO;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface TaskMapper extends BaseMapperX<TaskDO> {}
