package com.jifuwei.dynamic.logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigChangeListener;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author jifuwei
 */
@Slf4j
public class DynamicLoggerConfiguration implements ConfigChangeListener {

    /**
     * root 级别控制
     */
    private static final String LOGGER_LEVEL = "logger_level";

    /**
     * 精细化 logger 级别控制
     */
    private static final String LOGGER_LEVEL_DETAIL = "logger_level_detail";

    /**
     * apollo config
     */
    private Config apolloConfig;

    /**
     * 注入 apollo config
     *
     * @param apolloConfig
     */
    public DynamicLoggerConfiguration(Config apolloConfig) {
        this.apolloConfig = apolloConfig;
    }

    /**
     * 初始化方法
     */
    public void init() {
        // 初始化风控监听action配置
        String level = apolloConfig.getProperty(LOGGER_LEVEL, Level.ERROR.name());
        setRootLoggerLevel(Level.valueOf(level));

        // 注册监听
        apolloConfig.addChangeListener(this);
    }

    public void onChange(ConfigChangeEvent changeEvent) {
        if (changeEvent.changedKeys().contains(LOGGER_LEVEL)) {
            String newValue = changeEvent.getChange(LOGGER_LEVEL).getNewValue();
            try {
                setRootLoggerLevel(Level.valueOf(newValue));
            } catch (Exception e) {
                log.error("loggerLevel onChange error", e);
            }
        }
        if (changeEvent.changedKeys().contains(LOGGER_LEVEL_DETAIL)) {
            String newValue = changeEvent.getChange(LOGGER_LEVEL_DETAIL).getNewValue();
            try {
                parseLoggerConfig(newValue);
            } catch (Exception e) {
                log.error("loggerLevel detail onChange error", e);
            }
        }
    }

    public void setRootLoggerLevel(Level level) {
        try {
            // 获取日志上下文
            LoggerContext logContext = LoggerContext.getContext(false);
            Configuration configuration = logContext.getConfiguration();
            LoggerConfig loggerConfig = configuration.getRootLogger();
            loggerConfig.setLevel(level);
            logContext.updateLoggers();

            log.info("### update rootLoggerLevel {}", level);

            // 设置root 后 需要重置当前类及指定精细化配置的 logger
            setLoggerLevel(this.getClass().getName(), Level.INFO.name());
            setLoggerLevel(this.getClass().getSuperclass().getName(), Level.INFO.name());
            reConfig();
        } catch (Exception e) {
            log.error("setRootLoggerLevel error", e);
        }

    }

    public void setLoggerLevel(String name, String level) {
        //是否初始化成功
        boolean flag = isSetLoggerLevel(name, level);

        //该类未注册就注册
        if (!flag && initCustomClass(name)) {
            //初始化未注册的类
            boolean setLoggerLevel = isSetLoggerLevel(name, level);
            if (!setLoggerLevel) {
                log.error("### class -> {} not use log", name);
            }
        }
    }

    private boolean initCustomClass(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (Exception e) {
            log.error("init {} failed", className);
            return false;
        }
    }

    public boolean isSetLoggerLevel(String name, String level) {
        // 是否没有匹配到
        boolean flag = false;
        try {
            Level newLevel = Level.valueOf(level);
            LoggerContext logContext = LoggerContext.getContext(false);
            if (logContext.hasLogger(name)) {
                //精确匹配
                Logger logger = logContext.getLogger(name);
                logger.setLevel(newLevel);
                log.info("### update {} logger level {}", name, newLevel);
                flag = true;
            } else {
                //正则匹配
                Collection<Logger> loggers = logContext.getLoggers();
                for (Logger logger : loggers) {
                    if (Pattern.matches(name, logger.getName())) {
                        logger.setLevel(newLevel);
                        log.info("update {} logger level {}", name, level);
                        flag = true;
                    }
                }
            }

        } catch (Exception e) {
            log.error("setLoggerLevel error", e);
        }
        return flag;
    }

    public void reConfig() {
        String detail = apolloConfig.getProperty(LOGGER_LEVEL_DETAIL, null);
        if (StringUtils.isNotEmpty(detail)) {
            parseLoggerConfig(detail);
        }
    }

    public void parseLoggerConfig(String value) {
        Map<String, String> config = JSON.parseObject(value, new TypeReference<Map<String, String>>() {
        });

        if (null == config || config.isEmpty()) {
            return;
        }

        config.forEach(this::setLoggerLevel);
    }
}
