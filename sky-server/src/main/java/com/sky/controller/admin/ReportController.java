package com.sky.controller.admin;

import com.sky.dto.DataOverViewQueryDTO;
import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;

@RestController
@RequestMapping("/admin/report")
@Api(tags = "数据统计接口")
@Slf4j
public class ReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping("/turnoverStatistics")
    public Result<TurnoverReportVO> turnoverStatistics(@DateTimeFormat(pattern = "yyyy-MM-dd")
                                                       LocalDate begin,
                                                       @DateTimeFormat(pattern = "yyyy-MM-dd")
                                                       LocalDate end) {
        log.info("营业额统计");
        return Result.success(reportService.getTurnover(begin, end));
    }


    @GetMapping("/userStatistics")
    public Result<UserReportVO> userStatistics(@DateTimeFormat(pattern = "yyyy-MM-dd")
                                               LocalDate begin,
                                               @DateTimeFormat(pattern = "yyyy-MM-dd")
                                               LocalDate end) {
        log.info("用户数统计");
        return Result.success(reportService.getUserStatistics(begin, end));
    }

    @GetMapping("/ordersStatistics")
    public Result<OrderReportVO> orderStatistics(@DateTimeFormat(pattern = "yyyy-MM-dd")
                                                     LocalDate begin,
                                                 @DateTimeFormat(pattern = "yyyy-MM-dd")
                                                     LocalDate end) {
        log.info("订单数量统计");
        return Result.success(reportService.getOrderStatistics(begin, end));
    }


    @GetMapping("/top10")
    public Result<SalesTop10ReportVO> salesTop10(@DateTimeFormat(pattern = "yyyy-MM-dd")
                                                 LocalDate begin,
                                                 @DateTimeFormat(pattern = "yyyy-MM-dd")
                                                 LocalDate end) {
        log.info("销量前十统计");
        return Result.success(reportService.getTop10(begin, end));
    }

    @GetMapping("/export")
    public void export(HttpServletResponse response) {
        reportService.exportBusinessData(response);
    }
}
