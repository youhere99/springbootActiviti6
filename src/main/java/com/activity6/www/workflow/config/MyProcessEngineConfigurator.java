package com.activity6.www.workflow.config;

import org.activiti.spring.SpringAsyncExecutor;
import org.activiti.spring.SpringProcessEngineConfiguration;
import org.activiti.spring.boot.ActivitiProperties;
import org.activiti.spring.boot.DataSourceProcessEngineAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.IOException;
import java.util.List;
//分布式主键
@Configuration
public class MyProcessEngineConfigurator {

//    @Autowired
//    protected ActivitiProperties activitiProperties;
//
//    @Autowired
//    private DataSource dataSource;
//
//    @Autowired
//    private PlatformTransactionManager platformTransactionManager;
//
//    @Autowired
//    private SpringAsyncExecutor springAsyncExecutor;
//
//    @Autowired
//    private ResourcePatternResolver resourceLoader;
//
//    @Autowired
//    private DataSourceProcessEngineAutoConfiguration.DataSourceProcessEngineConfiguration dataSourceProcessEngineConfiguration;
//
    @Autowired
    private MyActivitiIDGenerator myActivitiIDGenerator;
    
    @Autowired
    private SpringProcessEngineConfiguration springProcessEngineConfiguration;


    @Bean
    @ConditionalOnMissingBean
    public LobHandler lobHandler() {
        return new DefaultLobHandler();
    }

   @PostConstruct
   public void processEngineConfigurationImpl(){
       springProcessEngineConfiguration.setIdGenerator(myActivitiIDGenerator);
   }

//    @Bean
//    public SpringProcessEngineConfiguration springProcessEngineConfiguration() throws IOException {
//
//        List<Resource> procDefResources = dataSourceProcessEngineConfiguration.discoverProcessDefinitionResources(this.resourceLoader, this.activitiProperties.getProcessDefinitionLocationPrefix(), this.activitiProperties.getProcessDefinitionLocationSuffixes(), this.activitiProperties.isCheckProcessDefinitions());
//        SpringProcessEngineConfiguration conf = dataSourceProcessEngineConfiguration.processEngineConfigurationBean((Resource[]) procDefResources.toArray(new Resource[procDefResources.size()]), dataSource, platformTransactionManager, springAsyncExecutor);
//        conf.setDeploymentName(StringUtils.hasText(this.activitiProperties.getDeploymentName()) ? conf.getDeploymentName() : conf.getDeploymentName());
//        conf.setDatabaseSchema(StringUtils.hasText(this.activitiProperties.getDatabaseSchema()) ? conf.getDatabaseSchema() : conf.getDatabaseSchema());
//        conf.setDatabaseSchemaUpdate(StringUtils.hasText(this.activitiProperties.getDatabaseSchemaUpdate()) ? conf.getDatabaseSchemaUpdate() : conf.getDatabaseSchemaUpdate());
//        conf.setDbIdentityUsed(this.activitiProperties.isDbIdentityUsed());
//        conf.setDbHistoryUsed(this.activitiProperties.isDbHistoryUsed());
//        conf.setAsyncExecutorActivate(this.activitiProperties.isAsyncExecutorActivate());
//        conf.setMailServerHost(this.activitiProperties.getMailServerHost());
//        conf.setMailServerPort(this.activitiProperties.getMailServerPort());
//        conf.setMailServerUsername(this.activitiProperties.getMailServerUserName());
//        conf.setMailServerPassword(this.activitiProperties.getMailServerPassword());
//        conf.setMailServerDefaultFrom(this.activitiProperties.getMailServerDefaultFrom());
//        conf.setMailServerUseSSL(this.activitiProperties.isMailServerUseSsl());
//        conf.setMailServerUseTLS(this.activitiProperties.isMailServerUseTls());
//        conf.setHistoryLevel(this.activitiProperties.getHistoryLevel());
//        conf.setIdGenerator(myActivitiIDGenerator);
//        return conf;
//    }
}