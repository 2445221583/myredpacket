package com.wcr.redpacket.pojo;
/**
 * 封装返回信息的类
 * */
public class ResultEntity {
    private String message;
    private String result;
    private static final String SUCESS="sucess";
    private static final String FAILED="failed";

    public ResultEntity(String message, String result) {
        this.message = message;
        this.result = result;
    }

    public ResultEntity() {
    }
    public static ResultEntity suucessful(){
        return new ResultEntity(SUCESS,null);
    }
    public static ResultEntity failed(String result){
        return new ResultEntity(FAILED,result);
    }
}
