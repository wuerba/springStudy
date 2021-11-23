package net.baincheng.www;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.JedisPoolConfig;
import java.io.IOException;



/**
 * redis集群配置
 */

@ConditionalOnExpression(value = "${spring.redis.enable:false}")
@Configuration

public class CacheConfig {

    private final static Logger logger = LoggerFactory.getLogger(CacheConfig.class);

    @Autowired
    private RedisProperties redisProperties;


    /**
     * redis集群连接
     *
     * @return
     */
    @Bean
    public JedisConnectionFactory redisConnectionFactory() {
        RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration(redisProperties.getCluster().getNodes());
        logger.info("集群节点:{}", redisProperties.getCluster().getNodes());
        clusterConfig.setMaxRedirects(redisProperties.getCluster().getMaxRedirects());
        logger.info("Redis集群最大重试次数setMaxRedirects:{}", redisProperties.getCluster().getMaxRedirects());
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        logger.info("连接池最大连接数（使用负值表示没有限制）setMaxTotal:{}", redisProperties.getPool().getMaxActive());
        jedisPoolConfig.setMaxTotal(redisProperties.getPool().getMaxActive());
        logger.info("连接池中的最大空闲连接getMaxIdle:{}", redisProperties.getPool().getMaxIdle());
        jedisPoolConfig.setMaxIdle(redisProperties.getPool().getMaxIdle());
        logger.info("连接池中的最小空闲连接getMinIdle:{}", redisProperties.getPool().getMinIdle());
        jedisPoolConfig.setMinIdle(redisProperties.getPool().getMinIdle());
        logger.info("连接池最大阻塞等待时间（使用负值表示没有限制）setMaxWaitMillis:{}", redisProperties.getPool().getMaxWait());
        jedisPoolConfig.setMaxWaitMillis(redisProperties.getPool().getMaxWait());
        JedisConnectionFactory factory = new JedisConnectionFactory(clusterConfig);
        factory.setTimeout(redisProperties.getTimeout()); //设置连接超时时间
        factory.setDatabase(redisProperties.getDatabase());
        factory.setPassword(redisProperties.getPassword());
        factory.setPoolConfig(jedisPoolConfig);
        return factory;
    }

    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory factory){
        RedisTemplate template=new RedisTemplate();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new JdkSerializationRedisSerializer());
        return template;
    }


    @Bean
    public RedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        StringRedisTemplate template = new StringRedisTemplate(factory);
        setSerializer(template); //设置序列化工具，这样JavaBean不需要实现Serializable接口
        template.afterPropertiesSet();
        return template;
    }

    private void setSerializer(StringRedisTemplate template) {
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer =
                new Jackson2JsonRedisSerializer(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        // Include.NON_NULL 属性为NULL 不序列化
        om.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 允许出现特殊字符和转义符
        om.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        // 允许出现单引号
        om.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        // 字段保留，将null值转为""
        om.getSerializerProvider().setNullValueSerializer(new JsonSerializer<Object>() {
            @Override
            public void serialize(Object o, JsonGenerator jsonGenerator,
                                  SerializerProvider serializerProvider)
                    throws IOException {
                jsonGenerator.writeString("");
            }
        });

        jackson2JsonRedisSerializer.setObjectMapper(om);
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
    }
}
