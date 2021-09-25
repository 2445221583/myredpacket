package com.wcr.redpacket.mapper;

import com.wcr.redpacket.pojo.RedPacketOrder;
import com.wcr.redpacket.pojo.RedPacketOrderExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface RedPacketOrderMapper {
    int countByExample(RedPacketOrderExample example);

    int deleteByExample(RedPacketOrderExample example);

    int deleteByPrimaryKey(Integer id);

    int insert(RedPacketOrder record);

    int insertSelective(RedPacketOrder record);

    List<RedPacketOrder> selectByExample(RedPacketOrderExample example);

    RedPacketOrder selectByPrimaryKey(Integer id);

    int updateByExampleSelective(@Param("record") RedPacketOrder record, @Param("example") RedPacketOrderExample example);

    int updateByExample(@Param("record") RedPacketOrder record, @Param("example") RedPacketOrderExample example);

    int updateByPrimaryKeySelective(RedPacketOrder record);

    int updateByPrimaryKey(RedPacketOrder record);
}