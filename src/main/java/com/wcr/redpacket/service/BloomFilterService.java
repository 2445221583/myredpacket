package com.wcr.redpacket.service;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.wcr.redpacket.mapper.UserMapper;
import com.wcr.redpacket.pojo.User;
import com.wcr.redpacket.pojo.UserExample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BloomFilterService {
    @Autowired
    private UserMapper userMapper;

    private BloomFilter<Integer> bloomFilter;



    //初始化布隆过滤器
    public void InitBloomMap(){
        List<User> list=userMapper.selectByExample(new UserExample());
        if(list!=null){
            bloomFilter=BloomFilter.create(Funnels.integerFunnel(),list.size());
            for(User user:list){
                bloomFilter.put(user.getId());
            }
        }
    }
    //判断用户是否存在
    public boolean isExitUser(Integer id){
        return bloomFilter.mightContain(id);
    }
}
