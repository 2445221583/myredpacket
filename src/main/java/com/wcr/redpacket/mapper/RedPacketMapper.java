package com.wcr.redpacket.mapper;


import com.wcr.redpacket.pojo.RedPacket;
import com.wcr.redpacket.pojo.RedPacketExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface RedPacketMapper {
    int countByExample(RedPacketExample example);

    int deleteByExample(RedPacketExample example);

    int deleteByPrimaryKey(Integer id);

    int insert(RedPacket record);

    int insertSelective(RedPacket record);

    List<RedPacket> selectByExample(RedPacketExample example);

    RedPacket selectByPrimaryKey(Integer id);

    int updateByExampleSelective(@Param("record") RedPacket record, @Param("example") RedPacketExample example);

    int updateByExample(@Param("record") RedPacket record, @Param("example") RedPacketExample example);

    int updateByPrimaryKeySelective(RedPacket record);

    int updateByPrimaryKey(RedPacket record);
}