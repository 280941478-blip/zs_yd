package cn.cdzs.module.infra.dal.mysql.db;

import cn.cdzs.framework.mybatis.core.mapper.BaseMapperX;
import cn.cdzs.module.infra.dal.dataobject.db.DataSourceConfigDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据源配置 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface DataSourceConfigMapper extends BaseMapperX<DataSourceConfigDO> {
}
