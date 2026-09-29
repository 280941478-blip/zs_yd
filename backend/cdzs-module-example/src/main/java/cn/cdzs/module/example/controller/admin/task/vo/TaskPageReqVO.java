package cn.cdzs.module.example.controller.admin.task.vo;
import cn.cdzs.framework.common.pojo.PageParam;
import jakarta.validation.constraints.*;
import lombok.*;
@Data @EqualsAndHashCode(callSuper=true)
public class TaskPageReqVO extends PageParam {
    @Size(max=100) private String name;
    @Size(max=32) private String code;
    @Min(0) @Max(2) private Integer status;
}
