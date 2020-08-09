package cn.itcast.pinyougou.task;

import cn.itcast.pinyougou.mapper.TbSeckillGoodsMapper;
import cn.itcast.pinyougou.pojo.TbSeckillGoods;
import cn.itcast.pinyougou.pojo.TbSeckillGoodsExample;
import cn.itcast.pinyougou.utils.SystemConst;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@Component
public class SeckillGoodsToRedisTask {

    @Resource
    private TbSeckillGoodsMapper seckillGoodsMapper;
    @Resource
    private RedisTemplate redisTemplate;

    @Scheduled(cron = "0/30 * * * * ?")
    public void importToRedis(){
        //1.查询合法的秒杀商品数据：状态为有效（status=1），库存量>0（stockCount> 0），秒杀开始时间<=当前时间<秒杀结束时间
        TbSeckillGoodsExample example = new TbSeckillGoodsExample();
        TbSeckillGoodsExample.Criteria criteria = example.createCriteria();
        Date date = new Date();
        criteria.andStatusEqualTo("1")
                .andStockCountGreaterThan(0)
                .andStartTimeLessThanOrEqualTo(date)
                .andEndTimeGreaterThan(date);
        List<TbSeckillGoods> tbSeckillGoods = seckillGoodsMapper.selectByExample(example);
        //2.将数据存入redis
        for(TbSeckillGoods good : tbSeckillGoods){
            redisTemplate.boundHashOps(TbSeckillGoods.class.getSimpleName()).put(good.getId(), good);
            //为每一个商品创建一个队列，队列中放和库存量相同数据量的商品id
            createQueue(good.getId(), good.getStockCount());
        }
    }

    private void createQueue(Long id, Integer stockCount){
        if(stockCount > 0){
            for(int i=0; i<stockCount; i++){
                redisTemplate.boundListOps(SystemConst.CONST_SECKILLGOODS_ID_PREFIX+id).leftPush(id);
            }
        }
    }
}
