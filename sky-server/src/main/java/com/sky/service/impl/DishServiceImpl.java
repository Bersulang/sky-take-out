package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Objects;

@Service
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 新增菜品，同时保存对应的口味数据
     * @param dishDTO
     */
    @Override
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();

        BeanUtils.copyProperties(dishDTO, dish);

        // 先向菜品表插入一条数据
        dishMapper.insert(dish);

        // 获取插入菜品的id
        Long dishId = dish.getId();

        List<DishFlavor> dishFlavors = dishDTO.getFlavors();

        // 为每个口味设置菜品id
        if (dishFlavors != null && !dishFlavors.isEmpty()) {
            dishFlavors.forEach(dishFlavor -> dishFlavor.setDishId(dishId));
        }

        // 批量插入口味数据
        dishFlavorMapper.insertBatch(dishFlavors);
    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult list(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());

        Page<DishVO> page = dishMapper.list(dishPageQueryDTO);

        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 删除菜品，同时删除对应的口味数据
     * @param ids
     */
    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        //判断菜品是否能删除——判断是否是在售状态
        for (Long id : ids) {
            Dish dish = dishMapper.getById(id);
            if (Objects.equals(dish.getStatus(), StatusConstant.ENABLE)) {
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }

        //判断菜品是否能删除——判断是否关联了套餐
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);

        if (setmealIds != null && !setmealIds.isEmpty()) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        for (Long id : ids) {
            dishMapper.deleteByid(id);

            dishFlavorMapper.delteByDishId(id);
        }
    }

    /**
     * 根据id查询菜品信息和对应的口味信息
     * @param id
     * @return
     */
    @Override
    public DishVO getByIdWithFlavor(Long id) {
        Dish dish = dishMapper.getById(id);

        DishVO dishVo = new DishVO();
        BeanUtils.copyProperties(dish, dishVo);

        List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);
        dishVo.setFlavors(dishFlavors);

        return dishVo;
    }

    /**
     * 修改菜品，同时修改对应的口味数据
     * @param dishDTO
     */
    @Override
    @Transactional
    public void update(DishDTO dishDTO) {
        Dish dish = new Dish();

        BeanUtils.copyProperties(dishDTO, dish);

        dishMapper.update(dish);

        // 删除原先风味数据
        dishFlavorMapper.delteByDishId(dishDTO.getId());

        // 重新插入新的风味数据
        List<DishFlavor> dishFlavors = dishDTO.getFlavors();

        if (dishFlavors != null && !dishFlavors.isEmpty()) {
            dishFlavors.forEach(flavor -> flavor.setDishId(dishDTO.getId()));

            dishFlavorMapper.insertBatch(dishFlavors);
        }


    }

    /**
     * 启用、禁用菜品
     * @param status
     * @param id
     */
    @Override
    public void setStatus(Integer status, Long id) {
        Dish dish = Dish.builder()
                .status(status)
                .id(id)
                .build();
        dishMapper.update(dish);
    }

    @Override
    public List<DishVO> getDishByCategoryId(Long categoryId) {
        return dishMapper.getDishByCategoryId(categoryId);
    }

}
