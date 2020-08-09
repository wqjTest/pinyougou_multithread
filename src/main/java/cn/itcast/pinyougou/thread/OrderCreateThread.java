package cn.itcast.pinyougou.thread;

import cn.itcast.pinyougou.mapper.TbSeckillGoodsMapper;
import cn.itcast.pinyougou.pojo.TbSeckillGoods;
import cn.itcast.pinyougou.pojo.TbSeckillOrder;
import cn.itcast.pinyougou.utils.IdWorker;
import cn.itcast.pinyougou.utils.OrderRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;

@Component
public class OrderCreateThread  implements  Runnable{

    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private IdWorker idWorker;
    @Resource
    private TbSeckillGoodsMapper seckillGoodsMapper;

    @Override
    public void run() {
        OrderRecord orderRecord = (OrderRecord) redisTemplate.boundListOps(OrderRecord.class.getSimpleName()).rightPop();
        if(null != orderRecord){
            TbSeckillGoods seckillGoods = (TbSeckillGoods) redisTemplate
                    .boundHashOps(TbSeckillGoods.class.getSimpleName()).get(orderRecord.getId());
            //4.生成秒杀订单，将订单保存到redis缓存
            TbSeckillOrder seckillOrder = new TbSeckillOrder();
            seckillOrder.setUserId(orderRecord.getUserId());
            seckillOrder.setSellerId(seckillGoods.getSellerId());
            seckillOrder.setSeckillId(idWorker.nextId());
            seckillOrder.setMoney(seckillGoods.getCostPrice());
            seckillOrder.setCreateTime(new Date());
            seckillOrder.setStatus("0");//0-未支付
            redisTemplate.boundHashOps(TbSeckillOrder.class.getSimpleName()).put(orderRecord.getUserId(), seckillOrder);
            synchronized (OrderCreateThread.class){
                seckillGoods = (TbSeckillGoods) redisTemplate
                        .boundHashOps(TbSeckillGoods.class.getSimpleName()).get(orderRecord.getId());
                //5.秒杀商品库存量-1
                seckillGoods.setStockCount(seckillGoods.getStockCount() - 1);
                //6.判断库存量是否<=0
                if(seckillGoods.getStockCount() <= 0){
                    //7.是，将秒杀商品更新到数据库，删除秒杀商品缓存
                    seckillGoodsMapper.updateByPrimaryKey(seckillGoods);
                    redisTemplate.boundHashOps(TbSeckillGoods.class.getSimpleName()).delete(orderRecord.getId());
                } else {
                    //8.否，将秒杀商品更新到缓存，返回成功
                    redisTemplate.boundHashOps(TbSeckillGoods.class.getSimpleName()).put(orderRecord.getId(), seckillGoods);
                }
            }

        }
    }
}
