package com.sky.mapper;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ShoppingCartMapper {
    void add(ShoppingCart shoppingCart);

    @Select("select * from shopping_cart where dish_id = #{dishId} or setmeal_id = #{setmealId}")
    ShoppingCart getById(ShoppingCartDTO shoppingCartDTO);

    @Update("update shopping_cart set number = #{number} + 1, amount =  where id = #{id}")
    void updateCountAndAmount(ShoppingCart shop);
}
