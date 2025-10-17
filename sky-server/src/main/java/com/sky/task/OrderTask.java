package com.sky.task;

import com.sky.constant.MessageConstant;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 处理超时未支付订单
     * 每分钟的第一秒执行一次
     */
    @Scheduled(cron = "1 * * * * ?")
    public void processOrderTimeOut() {
        log.info("处理支付超时订单...");
        List<Orders> ordersList = orderMapper.getNotPayOrders();
        for (Orders order : ordersList) {
            if (order.getOrderTime().plusMinutes(15).isBefore(LocalDateTime.now())) {
                order.setStatus(Orders.CANCELLED);
                order.setCancelReason(MessageConstant.PAY_TIMEOUT);
                order.setCancelTime(LocalDateTime.now());
                orderMapper.update(order);
            }
        }
    }

    /**
     * 将前一天的派送中订单改为完成
     * 每天凌晨1点执行一次
     */
    @Scheduled(cron = "* * 1 * * ?")
    public void processOrderDelivery() {
        log.info("处理派送超时订单");
        // 先拿到当前还在派送的订单
        List<Orders> deliveringOrders = orderMapper.getDeliveringOrders();
        // 目前只做简单的逻辑处理，后期可以优化
        for (Orders order : deliveringOrders) {
            if (order.getOrderTime().isBefore(LocalDateTime.now())) {
                order.setStatus(Orders.COMPLETED);
                order.setDeliveryTime(LocalDateTime.now());
                orderMapper.update(order);
            }
        }
    }
}
