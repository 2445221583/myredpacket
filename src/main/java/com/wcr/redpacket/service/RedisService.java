package com.wcr.redpacket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

@Service
public class RedisService {

    private DefaultRedisScript<Boolean> lockLuaScript;

    @Autowired
    private RedisTemplate<String,Object> redisTemplate;
    //加入到redis缓存
    public boolean setValue(String s, Integer remainingAmount) {
        boolean result=false;
        try{
            redisTemplate.opsForValue().set(s,remainingAmount);
            return true;
        }catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }

    /** * 执行lua脚本(锁续时) * @param key * @param value * @param expire * @return */
    public Boolean luaScriptExpandLockExpire(String key,String value,Long expire){
        lockLuaScript = new DefaultRedisScript<Boolean>();
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('expire', KEYS[1],ARGV[2]) else return '0' end";
        lockLuaScript.setScriptText(script);
        lockLuaScript.setResultType(Boolean.class);
        return (Boolean) redisTemplate.execute(lockLuaScript, Arrays.asList(key),value,expire);
    }
}
