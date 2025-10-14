package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 添加购物车
     * @param shoppingCartDTO
     */
    public void add(ShoppingCartDTO shoppingCartDTO) {
        //先去购物车表查询当前菜品是否在购物车中，是则直接增加数量和金额，否则添加一条新数据
        ShoppingCart shop = shoppingCartMapper.getById(shoppingCartDTO);

        if (shop != null)  {
            //当前菜品或套餐已经在购物车中，则数量加一，金额增加对应的菜品或套餐金额
            shoppingCartMapper.updateCountAndAmount(shop);
            return;
        }

        ShoppingCart shoppingCart = ShoppingCart.builder()
                .createTime(LocalDateTime.now())
                .userId(BaseContext.getCurrentId())
                .build();
        //根据id判断添加的是菜品还是套餐
        //并根据id去数据库查询对应的菜品和套餐信息
        if (shoppingCartDTO.getDishId() == null) {
            Setmeal setmeal = setmealMapper.getById(shoppingCartDTO.getSetmealId());
            shoppingCart.setAmount(setmeal.getPrice());
            shoppingCart.setName(setmeal.getName());
            shoppingCart.setNumber(1);
            shoppingCart.setImage(setmeal.getImage());
            shoppingCart.setSetmealId(shoppingCartDTO.getSetmealId());
        } else {
            Dish dish = dishMapper.getById(shoppingCartDTO.getDishId());
            shoppingCart.setAmount(dish.getPrice());
            shoppingCart.setDishId(shoppingCartDTO.getDishId());
            shoppingCart.setName(dish.getName());
            shoppingCart.setNumber(1);
            shoppingCart.setImage(dish.getImage());
            shoppingCart.setDishFlavor(shoppingCartDTO.getDishFlavor());
        }


        shoppingCartMapper.add(shoppingCart);
    }
}
