package cn.iocoder.yudao.module.example.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

@TableName("example_task")
@Data
@EqualsAndHashCode(callSuper = true)
public class TaskDO extends TenantBaseDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private Integer status;
    private String remark;
    private Long ownerId;
}
