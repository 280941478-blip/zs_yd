package cn.cdzs.module.example.controller.admin.task;

import cn.cdzs.framework.common.pojo.*;
import cn.cdzs.framework.common.util.object.BeanUtils;
import cn.cdzs.framework.excel.core.util.ExcelUtils;
import cn.cdzs.module.example.controller.admin.task.vo.*;
import cn.cdzs.module.example.service.TaskService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import static cn.cdzs.framework.common.pojo.CommonResult.success;

@RestController @RequestMapping("/example/task") @RequiredArgsConstructor
@Tag(name="管理后台 - 示例任务")
public class TaskController {
    private final TaskService service;
    @PostMapping("/create") @PreAuthorize("@ss.hasPermission('example:task:create')")
    @Operation(summary="创建示例任务")
    public CommonResult<Long> create(@Valid @RequestBody TaskSaveReqVO r) { return success(service.create(r)); }
    @PutMapping("/update") @PreAuthorize("@ss.hasPermission('example:task:update')")
    @Operation(summary="修改示例任务")
    public CommonResult<Boolean> update(@Valid @RequestBody TaskSaveReqVO r) { service.update(r);return success(true); }
    @DeleteMapping("/delete") @PreAuthorize("@ss.hasPermission('example:task:delete')")
    @Operation(summary="删除示例任务")
    public CommonResult<Boolean> delete(@RequestParam Long id) {service.delete(id);return success(true);}
    @GetMapping("/get") @PreAuthorize("@ss.hasPermission('example:task:query')")
    @Operation(summary="查看示例任务")
    public CommonResult<TaskRespVO> get(@RequestParam Long id) {return success(BeanUtils.toBean(service.get(id),TaskRespVO.class));}
    @GetMapping("/page") @PreAuthorize("@ss.hasPermission('example:task:query')")
    @Operation(summary="分页查询示例任务")
    public CommonResult<PageResult<TaskRespVO>> page(@Valid TaskPageReqVO r) {return success(BeanUtils.toBean(service.page(r),TaskRespVO.class));}
    @GetMapping("/export-excel") @PreAuthorize("@ss.hasPermission('example:task:export')")
    @Operation(summary="导出示例任务")
    public void export(@Valid TaskPageReqVO r,HttpServletResponse response) throws IOException {
        ExcelUtils.write(response,"示例任务.xlsx","任务",TaskExcelVO.class,BeanUtils.toBean(service.export(r),TaskExcelVO.class));
    }
    @GetMapping("/import-template") @PreAuthorize("@ss.hasPermission('example:task:import')")
    @Operation(summary="下载示例任务导入模板")
    public void template(HttpServletResponse response) throws IOException {
        ExcelUtils.write(response,"任务导入模板.xlsx","任务",TaskExcelVO.class,List.of());
    }
    @PostMapping("/import-excel") @PreAuthorize("@ss.hasPermission('example:task:import')")
    @Operation(summary="导入示例任务")
    public CommonResult<Integer> importExcel(@RequestParam MultipartFile file) throws IOException {
        return success(service.importRows(ExcelUtils.read(file,TaskExcelVO.class,1001)));
    }
}
