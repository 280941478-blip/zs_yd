package cn.cdzs.module.example.controller.admin.task.vo;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class TaskSaveReqVO {
    private Long id;
    @NotBlank @Pattern(regexp="[A-Za-z0-9_-]{1,32}", message="编码须为1至32位字母、数字、下划线或短横线")
    private String code;
    @NotBlank @Size(max=100)
    private String name;
    @NotNull @Min(0) @Max(2)
    private Integer status;
    @Size(max=500)
    private String remark;
}
