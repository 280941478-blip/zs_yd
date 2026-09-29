package cn.cdzs.module.system.job;

import cn.cdzs.framework.quartz.core.handler.JobHandler;
import cn.cdzs.framework.tenant.core.context.TenantContextHolder;
import cn.cdzs.framework.tenant.core.job.TenantJob;
import cn.cdzs.module.system.dal.dataobject.user.AdminUserDO;
import cn.cdzs.module.system.dal.mysql.user.AdminUserMapper;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;

@Component
public class DemoJob implements JobHandler {

    @Resource
    private AdminUserMapper adminUserMapper;

    @Override
    @TenantJob // 标记多租户
    public String execute(String param) {
        System.out.println("当前租户：" + TenantContextHolder.getTenantId());
        List<AdminUserDO> users = adminUserMapper.selectList();
        return "用户数量：" + users.size();
    }

}
