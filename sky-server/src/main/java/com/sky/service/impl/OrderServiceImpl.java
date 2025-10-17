package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.service.ShoppingCartService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private ShoppingCartService shoppingCartService;

    /**
     * 订单提交
     *
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {
        //性能优化，先进行异常判断后进行逻辑处理，防止不必要开销
        Long userId = BaseContext.getCurrentId();
        // 根据地址id去拿地址和收货人信息
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        // 地址为空则抛异常
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        // 拿到当前购物车的菜品信息
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(userId)
                .build();
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        // 购物车为空，则抛异常
        if (shoppingCartList == null) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        // 复制属性
        Orders order = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,order);

        order.setUserId(userId);
        order.setStatus(Orders.PENDING_PAYMENT); // 待支付
        order.setOrderTime(LocalDateTime.now());
        order.setPayStatus(Orders.UN_PAID); // 未支付
        order.setNumber(String.valueOf(System.currentTimeMillis())); // 订单号

        // 补全地址信息
        order.setConsignee(addressBook.getConsignee());
        order.setPhone(addressBook.getPhone());
        // 拼接地址信息
        String address = addressBook.getProvinceName() + addressBook.getCityName()
                + addressBook.getDistrictName() + addressBook.getDetail();
        order.setAddress(address);
        // 插入订单表
        orderMapper.insert(order);

        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(order.getId());
            orderDetailList.add(orderDetail);
        }
        // 批量插入订单明细表
        orderDetailMapper.insertBatch(orderDetailList);

        // 清空购物车
        shoppingCartMapper.deleteByid(userId);

        // 封装返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(order.getId())
                .orderAmount(order.getAmount())
                .orderNumber(order.getNumber())
                .orderTime(order.getOrderTime())
                .build();
        return orderSubmitVO;
    }




    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);
/*

        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }
*/

        //构造成功支付结果，返回前端
        JSONObject jsonObject = new JSONObject();
        //jsonObject.put("code", "ORDERPAID");
        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));
        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    /**
     * 历史订单查询
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult history(OrdersPageQueryDTO ordersPageQueryDTO) {
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        // 设置分页参数
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        // 执行sql,先去拿到订单信息
        Page<Orders> historyOrders = orderMapper.list(ordersPageQueryDTO);

        List<OrderVO> list = new ArrayList<>();
        // 封装订单详细数据
        for (Orders order : historyOrders) {
            Long orderId = order.getId();
            List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orderId);

            OrderVO orderVo = new OrderVO();
            BeanUtils.copyProperties(order, orderVo);
            orderVo.setOrderDetailList(orderDetailList);
            list.add(orderVo);
        }

        return new PageResult(historyOrders.getTotal(), list);
    }


    /**
     * 订单详情
     * @param id
     * @return
     */
    public OrderVO orderDetail(Long id) {
        Orders order = orderMapper.getById(id);
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);
        vo.setOrderDetailList(orderDetailList);
        return vo;
    }


    /**
     * 用户取消订单
     * @param id
     */
    public void userCancel(Long id) throws Exception {
        Orders order = orderMapper.getById(id);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        if (order.getStatus() > Orders.TO_BE_CONFIRMED) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        if (order.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            //调用微信支付退款接口
            weChatPayUtil.refund(
                    order.getNumber(), //商户订单号
                    order.getNumber(), //商户退款单号
                    new BigDecimal(0.01),//退款金额，单位 元
                    new BigDecimal(0.01)//原订单金额
            );

            //支付状态修改为 退款
            order.setPayStatus(Orders.REFUND);
        }
        order.setStatus(Orders.CANCELLED);
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason("用户取消订单");
        orderMapper.update(order);
    }


    /**
     * 再来一单
     * @param id
     */
    public void repetition(Long id) {
        // 先去拿到订单的菜品或套餐
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        // 插入到购物车
        for (OrderDetail orderDetail : orderDetailList) {
            ShoppingCartDTO shoppingCartDTO = new ShoppingCartDTO();
            BeanUtils.copyProperties(orderDetail, shoppingCartDTO);
            shoppingCartService.add(shoppingCartDTO);
        }
    }

    /**
     * 订单条件查询
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        Page<Orders> orders = orderMapper.list(ordersPageQueryDTO);

        List<OrderVO> list = new ArrayList<>();
        for (Orders order : orders) {
            Long orderId = order.getId();
            List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orderId);
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(order, orderVO);
            String orderDishes = orderDetailList.stream()
                            .map(detail -> detail.getName())
                            .collect(Collectors.joining(", "));
            orderVO.setOrderDishes(orderDishes);
            list.add(orderVO);
        }

        return new PageResult(orders.getTotal(), list);
    }


    /**
     * 接单
     * @param ordersConfirmDTO
     */
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders order = Orders.builder()
                            .id(ordersConfirmDTO.getId())
                            .status(Orders.CONFIRMED)
                            .build();
        orderMapper.update(order);
    }

    /**
     * 拒绝订单
     * @param ordersRejectionDTO
     */
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        Long id = ordersRejectionDTO.getId();
        Orders order = orderMapper.getById(id);
        if (order.getPayStatus().equals(Orders.PAID)) {
            //调用微信支付退款接口
            weChatPayUtil.refund(
                    order.getNumber(), //商户订单号
                    order.getNumber(), //商户退款单号
                    new BigDecimal(0.01),//退款金额，单位 元
                    new BigDecimal(0.01)//原订单金额
            );

            //支付状态修改为 退款
            order.setPayStatus(Orders.REFUND);
        }
        order.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        order.setCancelTime(LocalDateTime.now());
        order.setStatus(Orders.CANCELLED);
        orderMapper.update(order);
    }

    /**
     * 获取不同状态的订单数量
     * @return
     */
    public OrderStatisticsVO getCountByStatus() {
        Integer toBeConfirmed = orderMapper.getCountByStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.getCountByStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.getCountByStatus(Orders.DELIVERY_IN_PROGRESS);

        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);

        return orderStatisticsVO;

    }

    /**
     * 派送订单
     * @param id
     */
    public void delivery(Long id) {
        Orders order = Orders.builder()
                .id(id)
                .status(Orders.DELIVERY_IN_PROGRESS)
                .build();
        orderMapper.update(order);
    }

    /**
     * 完成订单
     * @param id
     */
    public void complete(Long id) {
        Orders order = Orders.builder()
                .id(id)
                .status(Orders.COMPLETED)
                .deliveryTime(LocalDateTime.now())
                .build();
        orderMapper.update(order);
    }

    /**
     * 管理端取消订单
     * @param ordersCancelDTO
     */
    public void adminCancel(OrdersCancelDTO ordersCancelDTO) {
        Orders order = Orders.builder()
                .id(ordersCancelDTO.getId())
                .cancelReason(ordersCancelDTO.getCancelReason())
                .cancelTime(LocalDateTime.now())
                .status(Orders.CANCELLED)
                .build();
        orderMapper.update(order);
    }


}
