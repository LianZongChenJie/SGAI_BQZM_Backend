package org.jeecg.modules.bems.report.controller;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.jeecg.modules.bems.entity.RealData;
import org.jeecg.modules.bems.mdm.entity.Device;
import org.jeecg.modules.bems.mdm.service.IDeviceService;
import org.jeecg.modules.bems.report.config.ReportConfig;
import org.jeecg.modules.bems.report.dto.ReportDto;
import org.jeecg.modules.bems.service.IRealDataService;
import org.jeecgframework.poi.excel.ExcelExportUtil;
import org.jeecgframework.poi.excel.entity.TemplateExportParams;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 报表导出
 */
@RestController
@RequestMapping("/bems/report")
@AllArgsConstructor
public class ReportController {

    private final IDeviceService deviceService;

    private final IRealDataService realDataService;

    private final ReportConfig reportConfig;

    /**
     * 报表导出
     */
    @GetMapping("/export")
    public void export(HttpServletResponse response, ReportDto params) throws IOException {
        TemplateExportParams templateParams = new TemplateExportParams(reportConfig.getTemplatePath() + reportConfig.getTemplateDict().get(params.getTemplateId()));
        List<Device> devices = deviceService.findByType(Device.DEVICE_TYPE_MEASURING);
        // 获取设备表底值
        // @TODO findFirstByLtTimeDesc 已增加 startTime 参数，需补充传入
        Map<Long, BigDecimal> lData = realDataService.findFirstByLtTimeDesc(params.getStartTime().minusMonths(1), params.getStartTime().with(LocalTime.MAX))
                .stream()
                .collect(Collectors.toMap(RealData::getDeviceId, RealData::getValue));
        // @TODO findFirstByLtTimeDesc 已增加 startTime 参数，需补充传入
        Map<Long,BigDecimal> gData = realDataService.findFirstByLtTimeDesc(params.getStartTime(), params.getEndTime())
                .stream()
                .collect(Collectors.toMap(RealData::getDeviceId,RealData::getValue));
        Map<String,Object> dataMap = new HashMap<>();
        for(Device device : devices){
            BigDecimal l = lData.getOrDefault(device.getId(),BigDecimal.ZERO);
            BigDecimal g = gData.getOrDefault(device.getId(),BigDecimal.ZERO);
            if(StrUtil.isNotEmpty(params.getConvertInteger()) && "1".equals(params.getConvertInteger())){
                // 数据四舍五入,只要整数
                l = l.setScale(0, RoundingMode.HALF_UP);
                g = g.setScale(0, RoundingMode.HALF_UP);
            }
            dataMap.put("R_" + device.getId(),g.toPlainString());
            dataMap.put("L_" + device.getId(),l.toPlainString());
        }
        // 年份
        dataMap.put("R_year",params.getEndTime().getYear());
        dataMap.put("L_year",params.getStartTime().getYear());
        // 月份
        dataMap.put("R_month",params.getEndTime().getMonthValue());
        dataMap.put("L_month",params.getStartTime().getMonthValue());
        // 天
        dataMap.put("R_day",params.getEndTime().getDayOfMonth());
        dataMap.put("L_day",params.getStartTime().getDayOfMonth());
        try {
            Workbook workbook = ExcelExportUtil.exportExcel(templateParams, dataMap);
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);

                // 设置整个工作簿的公式重新计算
                workbook.setForceFormulaRecalculation(true);

                // 如果需要，也可以设置每个 sheet
                sheet.setForceFormulaRecalculation(true);
            }

            // 设置响应头
            response.setContentType("application/vnd.ms-excel");
            response.setCharacterEncoding("UTF-8");
            String fileName = reportConfig.getTemplateDict().get(params.getTemplateId());
            // 处理中文文件名，兼容不同浏览器
            String encodedFileName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-disposition",
                "attachment;filename=" + encodedFileName + ";filename*=UTF-8''" + encodedFileName);

            // 输出workbook到response中
            try (OutputStream os = response.getOutputStream()) {
                workbook.write(os);
                os.flush();
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
