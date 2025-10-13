package com.sky.controller.user;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController("userShopController")
@RequestMapping("/user/shop")
@Slf4j
@Api(tags = "用户端店铺相关接口")
public class ShopController {
    @Autowired
    private RedisTemplate redisTemplate;
    /**
     * 获取店铺营业状态
     * @return
     */
    @GetMapping("/status")
    public Result<Integer> getStatus() {
        Integer shopStatus = (Integer) redisTemplate.opsForValue().get("Shop_Status");
        log.info("用户端获取当前店铺营业状态：{}", shopStatus == 1 ? "营业中" : "打烊中");
        return Result.success(shopStatus);
    }
}
