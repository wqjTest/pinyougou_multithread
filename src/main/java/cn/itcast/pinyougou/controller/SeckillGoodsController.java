package cn.itcast.pinyougou.controller;

import cn.itcast.pinyougou.pojo.TbSeckillGoods;
import cn.itcast.pinyougou.service.SeckillGoodsService;
import cn.itcast.pinyougou.utils.Result;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/seckillGoods")
public class SeckillGoodsController {

    @Resource
    private SeckillGoodsService seckillGoodsService;

    @RequestMapping("/findAll")

    public List<TbSeckillGoods> findAll(){
        return seckillGoodsService.findAll();
    }

    @RequestMapping("/findOne/{id}")
    public TbSeckillGoods findOne(@PathVariable("id") Long id){
        return seckillGoodsService.findOne(id);
    }

    @RequestMapping("/saveOrder/{id}")
    public Result saveOrder(@PathVariable("id") Long id){
        String userId = "jiuwenlong";
        return seckillGoodsService.saveOrder(id, userId);
    }
}
