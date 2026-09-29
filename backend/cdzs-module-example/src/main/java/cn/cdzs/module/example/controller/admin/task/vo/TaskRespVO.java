package cn.cdzs.module.example.controller.admin.task.vo;
import java.time.LocalDateTime;
import lombok.Data;
@Data
public class TaskRespVO {
    private Long id;
    private String code;
    private String name;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
}
