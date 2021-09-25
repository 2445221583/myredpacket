package com.wcr.redpacket.service;

import com.wcr.redpacket.config.RedisExpandLockExpireTask;
import com.wcr.redpacket.mapper.RedPacketMapper;
import com.wcr.redpacket.mapper.RedPacketOrderMapper;
import com.wcr.redpacket.pojo.RedPacket;
import com.wcr.redpacket.pojo.RedPacketExample;
import com.wcr.redpacket.pojo.RedPacketOrder;
import com.wcr.redpacket.pojo.ResultEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RedPacketService {

    @Autowired
    private RedPacketMapper redPacketMapper;

    @Autowired
    private RedisService redisService;

    @Autowired
    private BloomFilterService bloomFilterService;
    //注入线程池
    @Autowired
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedPacketOrderMapper redPacketOrderMapper;

    //用于拼接redis中红包余额和数量key的字符串
    public static final String REMAINING_NUM = ":remaining_num";

    public static final String REMAINING_AMOUNT = ":remaining_amount";

    public static final String REMAINING_USER =":getpacket_user" ;

    public static final Integer maxSize=3000;
//    //阻塞队列限流
//    private BlockingQueue<String> requestQueue = new ArrayBlockingQueue(maxSize);
    //用一个map保存所有的阻塞队列
    private ConcurrentHashMap<String,BlockingQueue> redpacketQueueMap=new ConcurrentHashMap<>();

    private ConcurrentHashMap<String, AtomicInteger> requestSizeMap=new ConcurrentHashMap<>();

    private volatile ConcurrentHashMap<String,Object> isLoging=new ConcurrentHashMap<>();

    public List<RedPacket> findAll(){
        return redPacketMapper.selectByExample(new RedPacketExample());
    }


    public ResultEntity addRedPacket(Integer uid,Integer account,Integer num){

        //1.布隆过滤器过滤
        //初始化布隆过滤器
        bloomFilterService.InitBloomMap();
        if(!bloomFilterService.isExitUser(uid)){
            return ResultEntity.failed("无效用户，请登录后再发红包");
        }



        //2.数据库插入数据
        //创建数据，封装红包对象
        RedPacket redPacket=new RedPacket();
        Date date=new Date(System.currentTimeMillis());
        redPacket.setCreateTime(date);
        redPacket.setUpdateTime(date);
        //生成uuid
        redPacket.setRedPacketId(UUID.randomUUID().toString().replace("-",""));
        redPacket.setRemainingAmount(account);
        redPacket.setTotalPacket(num);
        redPacket.setRemainingAmount(account);
        redPacket.setRemainingPacket(num);
        redPacket.setTotalAmount(account);
        redPacket.setUid(uid);
        //标志位设置成false,表示抢红包还未开始
        isLoging.put(redPacket.getRedPacketId(),true);
//        //初始化布隆过滤器
//        bloomFilterService.InitBloomMap();

        //3.redis加入缓存
        int result = redPacketMapper.insertSelective(redPacket);
        if(result==0){
            return ResultEntity.failed("红包发送失败");
        }
        boolean res=false;
        //设置红包金额，写入缓存
        res=redisService.setValue(redPacket.getRedPacketId()+REMAINING_AMOUNT,redPacket.getRemainingAmount());
        //设置红包数量，写入缓存
        res=redisService.setValue(redPacket.getRedPacketId()+REMAINING_NUM,redPacket.getRemainingPacket());
        //初始化抢到红包的用户缓存，抢到红包后将他们加入缓存
        redisTemplate.boundListOps(redPacket.getRedPacketId()+REMAINING_USER);
        //发送红包的同时给红包创建阻塞队列，方便后面的限流
        redpacketQueueMap.put(redPacket.getRedPacketId(),new ArrayBlockingQueue<Integer>(maxSize));

        requestSizeMap.put(redPacket.getRedPacketId(),new AtomicInteger(0));

        System.out.println(uid+"用户"+"红包发送成功，抢红包开始！！！！"+"\n红包id为"+redPacket.getRedPacketId());
        //设置标志位，抢红包开始
        isLoging.put(redPacket.getRedPacketId(),true);

        //开启之后轮询标志位，处理阻塞队列中的请求
        //异步处理阻塞队列中的请求,传入对应红包的uuid
        requestProcessSevice(redPacket.getRedPacketId());

        return ResultEntity.suucessful();
    }


    //主要是对阻塞队列中的请求进行异步处理
    //这里就是生产者消费中的消费者
    private void requestProcessSevice(String packetUUID) {
        //拿到对应的阻塞队列
        BlockingQueue blockingQueue=redpacketQueueMap.get(packetUUID);
        //判断抢红包是否开启
        while((Boolean)isLoging.get(packetUUID)){
            if(!blockingQueue.isEmpty()){
                Integer uid=(Integer) blockingQueue.poll();
                threadPoolTaskExecutor.execute(
                        new Runnable() {
                            @Override
                            public void run() {
                                String lockname="lock"+packetUUID;
                                //实际抢红包的业务逻辑
                                //尝试获取锁
                                //需要设置定时锁否则会导致死锁
                                Boolean lock = redisTemplate.opsForValue().setIfAbsent(lockname,packetUUID+uid,3,TimeUnit.SECONDS);

                                //获取成功则开始进行抢红包操作
                                if(lock){
                                    RedisExpandLockExpireTask redisExpandLockExpireTask=new RedisExpandLockExpireTask(lockname,packetUUID+uid,true,3,redisService);
                                    Thread thread=new Thread(redisExpandLockExpireTask);
                                    //thread.setDaemon(true);
                                    thread.start();
                                    Integer num=(Integer) redisTemplate.opsForValue().get(packetUUID+REMAINING_NUM);
                                    if(num<=0){
                                        isLoging.put(packetUUID,false);
                                        //Packetisloging=(boolean)isLoging.get(packetUUID);
                                        redisTemplate.delete(lockname);
                                        return;
                                    }
                                    //修改库存缓存
                                    redisTemplate.opsForValue().set(packetUUID+REMAINING_NUM,--num);


                                    RedPacketExample redPacketExample=new RedPacketExample();
                                    redPacketExample.createCriteria().andRedPacketIdEqualTo(packetUUID);
                                    RedPacket redPacket=new RedPacket();
                                    redPacket.setRemainingPacket((Integer) redisTemplate.opsForValue().get(packetUUID+REMAINING_NUM));
                                    RedPacketOrder redPacketOrder=new RedPacketOrder();
                                    //修改金额缓存，数据库数值
                                    //如果num等于0，那么就是最后一个红包
                                    if(num==0){
                                        int remainaccount=(int)redisTemplate.opsForValue().get(packetUUID+REMAINING_AMOUNT);
                                        //修改缓存中的红包余额
                                        redisTemplate.opsForValue().set(packetUUID+REMAINING_AMOUNT,0);
                                        //修改数据库中的红包余额
                                        redPacket.setRemainingAmount(0);
                                        //设置抢到红包用户的红包余额
                                        redPacketOrder.setAmount(remainaccount);

                                    }
                                    else{
                                        //如果不为最后一个红包
                                        int redisRemain=(Integer) redisTemplate.opsForValue().get(packetUUID+REMAINING_AMOUNT);
                                        int remainaccount=redisRemain/(num+1)*2;
                                        //设置每个用户抢到红包的随机数
                                        int usergetaccount=new Random().nextInt(remainaccount);
                                        //修改缓存中的余额
                                        redisTemplate.opsForValue().set(packetUUID+REMAINING_AMOUNT,redisRemain-usergetaccount);
                                        //修改数据库中的余额
                                        redPacket.setRemainingAmount(redisRemain-usergetaccount);
                                        //设置抢到红包用户的红包余额
                                        redPacketOrder.setAmount(usergetaccount);
                                    }

                                    //将抢到红包的用户id加入到redis缓存
                                    redisTemplate.boundListOps(packetUUID+REMAINING_USER).leftPush(uid);

                                    //将抢到红包的用户数据插入数据库

                                    redPacketOrder.setUid(uid);
//                                    redPacketOrder.setAmount(100);
                                    redPacketOrder.setRedPacketId(packetUUID);

                                    redPacketMapper.updateByExampleSelective(redPacket,redPacketExample);
                                    redPacketOrderMapper.insert(redPacketOrder);

                                    //释放锁
                                    redisTemplate.delete(lockname);
                                }
                                //如果没有获取锁，那么需要重入，防止库存遗留的问题
                                else{
                                    try {
                                        Thread.sleep(2000);
                                        run();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }

                            }
                        }
                );
            }
        }
    }

    //抢红包的逻辑
    public ResultEntity getRedPacket(Integer uid,String redUUID) {
        //根据布隆过滤器判断是否是真实请求
        if(!bloomFilterService.isExitUser(uid)){
            System.out.println("非真实请求");
            return ResultEntity.failed("请登录后再抢红包");
        }
        //从map中拿出阻塞队列
        BlockingQueue blockingQueue=redpacketQueueMap.get(redUUID);
        AtomicInteger size=requestSizeMap.get(redUUID);
        if(size.getAndIncrement()<=maxSize){
            try {
                blockingQueue.put(uid);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //System.out.println("入队成功，抢到红包");
            return ResultEntity.suucessful();
        }
        //System.out.println("抢红包失败"+size);
        return ResultEntity.failed("来晚了哟！！！红包被抢完了");
    }

    public Integer getSize(String UUID) {
        return redpacketQueueMap.get(UUID).size();
    }
}
