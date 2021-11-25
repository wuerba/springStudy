package net.baincheng.www;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.TransportMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@Configuration
public class RedissonConfig {
    private final static Logger logger = LoggerFactory.getLogger(RedissonConfig.class);
    @Autowired
    private RedisProperties redisProperties;

    @Bean
    public RedissonClient redissonClient() throws IOException {

        List<String> clusterNodes = new ArrayList<>();
        for (int i = 0; i < redisProperties.getCluster().getNodes().size(); i++) {
            clusterNodes.add("redis://" + redisProperties.getCluster().getNodes().get(i));
        }
        Config config = new Config();
        config.setTransportMode(TransportMode.NIO);
        config.useClusterServers() //这是用的集群server
                .setScanInterval(2000) //设置集群状态扫描时间
                .addNodeAddress(clusterNodes.toArray(new String[clusterNodes.size()]))
                .setPassword(redisProperties.getPassword());
        RedissonClient redisson = Redisson.create(config);
        logger.info("TransportMode:"+config.getTransportMode());
        logger.info("cluster nodes:"+clusterNodes);
        logger.info("检测是否配置成功:"+ redisson.getConfig().toJSON());
        return redisson;
    }
}