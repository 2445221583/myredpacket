package com.wcr.redpacket.config;

import com.wcr.redpacket.service.RedisService;

/**
 * 守护线程
 * */
public class RedisExpandLockExpireTask implements Runnable{
    private String lockname;
    private String lockvalue;
    private boolean isRun;
    private long expire;
    private RedisService redisService;

    public RedisExpandLockExpireTask(String lockname, String lockvalue, boolean isRun, long expire, RedisService redisService) {
        this.lockname = lockname;
        this.lockvalue = lockvalue;
        this.isRun = isRun;
        this.expire = expire;
        this.redisService = redisService;
    }

    @Override
    public void run() {
        long sleepTime=expire*2/3;
        while(isRun){
            try {
                Thread.sleep(sleepTime);
                //执行lua续锁
                boolean res=redisService.luaScriptExpandLockExpire(lockname,lockvalue,expire);
//                if(res){
//                    System.out.println("续锁成功");
//                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }finally {
                taskStop();
            }
        }
    }

    public void taskStop(){
        isRun=false;
    }
}
