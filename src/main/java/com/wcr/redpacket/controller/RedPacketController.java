package com.wcr.redpacket.controller;

import com.wcr.redpacket.pojo.ResultEntity;
import com.wcr.redpacket.service.RedPacketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.print.DocFlavor;

/**
 * 红包的控制类
 * 整个红包流程可以分为三部分
 * 1.发红包：携带用户的id,红包大小，红包个数，这里使用布隆过滤器
 * 检查用户是否是真实用户，如果是真实用户将这条记录加入到数据库中，
 * 同时将数据写入缓存，这些操作交给具体的业务类来实现。将标志为设置为true
 * 同时这个业务也充当一个消费者的角色，不断轮询标志位
 * 当标志位为true的时候说明抢红包开始，通过线程池开始处理阻塞队列中的请求
 *这一步就是开始拆分红包，同时将红包订单写入到缓存中。
 * 2.抢红包：采用阻塞队列来限流，限制每个红包的并发量
 * 布隆过滤器避免非真实用户请求，之后拆红包再进行异步处理红包分发
 * 的问题，相当于把抢红包分成两部分来做，抢到红包和拆开红包
 * 3.拆红包，抢到红包的用户就可以拆红包，去缓存中查看抢到的红包信息
 * */
@RestController
@RequestMapping("/api/redpacket")
public class RedPacketController {
    @Autowired
    private RedPacketService redPacketService;
    @RequestMapping("/addpacket/{uid}/{account}/{num}")
    public ResultEntity addPacket(@PathVariable("uid") Integer uid,
                                  @PathVariable("account") Integer account,
                                  @PathVariable("num") Integer num){
        return redPacketService.addRedPacket(uid,account,num);
    }
    //采用漏桶算法来进行限流，加入到阻塞队列中，阻塞队列的容量是数据库最大连接数的3倍
    @RequestMapping("/getpacket/{uid}/{redpacketId}")
    public ResultEntity getPacket(@PathVariable("uid") Integer uid,@PathVariable("redpacketId") String redUUID){
        //布隆过滤器过滤掉非真实客货
        //阻塞队列限流，不同用户抢红包和拆红包是异步的
        ResultEntity resultEntity=redPacketService.getRedPacket(uid,redUUID);
//        System.out.println("抢红包结束");
        return resultEntity;
    }
    //测试高并发下阻塞队列的size是否到3000就拒绝请求
    @RequestMapping("/check/{redpacketId}")
    public String checkPacket(@PathVariable("redpacketId") String redUUID){
        return "size"+redPacketService.getSize(redUUID);
    }

}
