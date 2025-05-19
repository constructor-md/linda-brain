package com.awesome.lindabrain.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * 雪花算法ID生成工具类
 * 基于本机网络地址计算数据中心ID和机器ID
 */
public class SnowflakeIdUtil {

    // 起始的时间戳 (2020-01-01)
    private final static long START_TIMESTAMP = 1577836800000L;

    // 机器ID所占的位数
    private final static long MACHINE_ID_BITS = 5L;

    // 数据标识ID所占的位数
    private final static long DATACENTER_ID_BITS = 5L;

    // 支持的最大机器ID，结果是31 (2^5-1)
    private final static long MAX_MACHINE_ID = -1L ^ (-1L << MACHINE_ID_BITS);

    // 支持的最大数据标识ID，结果是31 (2^5-1)
    private final static long MAX_DATACENTER_ID = -1L ^ (-1L << DATACENTER_ID_BITS);

    // 序列在机器ID中所占的位数
    private final static long SEQUENCE_BITS = 12L;

    // 机器ID向左移12位
    private final static long MACHINE_ID_SHIFT = SEQUENCE_BITS;

    // 数据标识id向左移17位(12+5)
    private final static long DATACENTER_ID_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS;

    // 时间戳向左移22位(12+5+5)
    private final static long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS + DATACENTER_ID_BITS;

    // 生成序列的掩码，这里为4095 (0b111111111111=0xfff=4095)
    private final static long SEQUENCE_MASK = -1L ^ (-1L << SEQUENCE_BITS);

    // 工作机器ID(0~31)
    private static long machineId;

    // 数据中心ID(0~31)
    private static long datacenterId;

    // 毫秒内序列(0~4095)
    private static long sequence = 0L;

    // 上次生成ID的时间戳
    private static long lastTimestamp = -1L;

    static {
        try {
            // 根据本机网络地址计算数据中心ID和机器ID
            InetAddress ip = InetAddress.getLocalHost();
            NetworkInterface network = null;
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                NetworkInterface n = en.nextElement();
                if (!n.isLoopback() && n.isUp()) {
                    network = n;
                    break;
                }
            }

            if (network == null) {
                // 如果找不到合适的网络接口，使用IP地址的哈希值
                datacenterId = (ip.getHostAddress().hashCode() >>> 16) & MAX_DATACENTER_ID;
                machineId = (ip.getHostAddress().hashCode() & MAX_MACHINE_ID);
            } else {
                // 使用网络接口的MAC地址计算
                byte[] mac = network.getHardwareAddress();
                if (mac != null) {
                    datacenterId = ((mac[mac.length - 2] & 0x0F) | ((mac[mac.length - 1] & 0xFF) << 8)) & MAX_DATACENTER_ID;
                    machineId = (mac[mac.length - 1] & 0xFF) & MAX_MACHINE_ID;
                } else {
                    // 如果没有MAC地址，使用IP地址的哈希值
                    datacenterId = (ip.getHostAddress().hashCode() >>> 16) & MAX_DATACENTER_ID;
                    machineId = (ip.getHostAddress().hashCode() & MAX_MACHINE_ID);
                }
            }

            // 确保生成的ID在有效范围内
            datacenterId = datacenterId % (MAX_DATACENTER_ID + 1);
            machineId = machineId % (MAX_MACHINE_ID + 1);

        } catch (Exception e) {
            // 发生异常时，使用默认值或随机值
            datacenterId = 0L;
            machineId = 0L;
            e.printStackTrace();
        }
    }

    /**
     * 获得下一个ID (线程安全)
     * @return SnowflakeId
     */
    public static synchronized long nextId() {
        long timestamp = time();

        // 如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过这个时候应当抛出异常
        if (timestamp < lastTimestamp) {
            throw new RuntimeException(
                    String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
        }

        // 如果是同一时间生成的，则进行毫秒内序列
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            // 毫秒内序列溢出
            if (sequence == 0) {
                // 阻塞到下一个毫秒,获得新的时间戳
                timestamp = tilNextMillis(lastTimestamp);
            }
        }
        // 时间戳改变，毫秒内序列重置
        else {
            sequence = 0L;
        }

        // 上次生成ID的时间戳
        lastTimestamp = timestamp;

        // 移位并通过或运算拼到一起组成64位的ID
        return ((timestamp - START_TIMESTAMP) << TIMESTAMP_LEFT_SHIFT) //
                | (datacenterId << DATACENTER_ID_SHIFT) //
                | (machineId << MACHINE_ID_SHIFT) //
                | sequence;
    }

    /**
     * 阻塞到下一个毫秒，直到获得新的时间戳
     * @param lastTimestamp 上次生成ID的时间戳
     * @return 当前时间戳
     */
    protected static long tilNextMillis(long lastTimestamp) {
        long timestamp = time();
        while (timestamp <= lastTimestamp) {
            timestamp = time();
        }
        return timestamp;
    }

    /**
     * 返回以毫秒为单位的当前时间
     * @return 当前时间(毫秒)
     */
    protected static long time() {
        return System.currentTimeMillis();
    }

    // 测试
    public static void main(String[] args) {
        for (int i = 0; i < 1000; i++) {
            System.out.println(nextId());
        }
    }
}