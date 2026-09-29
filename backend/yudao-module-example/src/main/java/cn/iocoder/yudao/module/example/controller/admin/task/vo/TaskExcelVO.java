package cn.iocoder.yudao.module.example.controller.admin.task.vo;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import lombok.Data;
@Data @ExcelIgnoreUnannotated
public class TaskExcelVO {
    @ExcelProperty("任务编码") private String code;
    @ExcelProperty("任务名称") private String name;
    @ExcelProperty("状态(0待办/1进行中/2完成)") private Integer status;
    @ExcelProperty("备注") private String remark;
}
