package cn.itcast.pinyougou.service.impl;

import cn.itcast.pinyougou.mapper.TbSeckillGoodsMapper;
import cn.itcast.pinyougou.pojo.TbSeckillGoods;
import cn.itcast.pinyougou.service.SeckillGoodsService;
import cn.itcast.pinyougou.thread.OrderCreateThread;
import cn.itcast.pinyougou.utils.IdWorker;
import cn.itcast.pinyougou.utils.OrderRecord;
import cn.itcast.pinyougou.utils.Result;
import cn.itcast.pinyougou.utils.SystemConst;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
@Transactional
public class SeckillGoodsServiceImpl implements SeckillGoodsService {

    @Resource
    private RedisTemplate redisTemplate;

    @Override
    public List<TbSeckillGoods> findAll() {
        return redisTemplate.boundHashOps(TbSeckillGoods.class.getSimpleName()).values();
    }

    @Override
    public TbSeckillGoods findOne(Long id) {
        return (TbSeckillGoods) redisTemplate.boundHashOps(TbSeckillGoods.class.getSimpleName()).get(id);
    }

    @Resource
    private IdWorker idWorker;
    @Resource
    private TbSeckillGoodsMapper seckillGoodsMapper;
    @Resource
    private ThreadPoolTaskExecutor executor;
    @Resource
    private OrderCreateThread orderCreateThread;

    @Override
    public Result saveOrder(Long id, String userId) {
        //0.从用户的set集合中判断用户是否已下单
        Boolean member = redisTemplate.boundSetOps(SystemConst.CONST_USER_ID_PREFIX + id).isMember(userId);
        if(member){
            //如果正在排队或者未支付的，提示用户你正在排队或有订单未支付
            return new Result(false, "对不起，您正在排队等待支付，请尽快支付！");
        }
        //1.从队列中获取秒杀商品id
        id = (Long) redisTemplate.boundListOps(SystemConst.CONST_SECKILLGOODS_ID_PREFIX + id).rightPop();
        //2.判断商品是否存在
        if(null == id){
            //3.商品不存在，或库存<=0，返回失败，提示已售罄
            return new Result(false, "对不起，商品已售罄，请您选择其他商品！");
        }
        //4.将用户放入用户集合
        redisTemplate.boundSetOps(SystemConst.CONST_USER_ID_PREFIX + id).add(userId);
        //5.创建OrderRecord对象记录用户下单信息：用户id，商品id，放到OrderRecord队列中
        OrderRecord orderRecord = new OrderRecord(id, userId);
        redisTemplate.boundListOps(OrderRecord.class.getSimpleName()).leftPush(orderRecord);
        //6.通过线程池启动线程处理OrderRecord中的数据，返回成功
        executor.execute(orderCreateThread);
        return new Result(true, "秒杀成功，请您尽快支付！");
    }
}
