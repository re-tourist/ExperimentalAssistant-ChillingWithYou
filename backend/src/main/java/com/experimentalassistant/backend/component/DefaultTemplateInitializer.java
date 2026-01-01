package com.experimentalassistant.backend.component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.experimentalassistant.backend.entity.Template;
import com.experimentalassistant.backend.entity.TemplateField;
import com.experimentalassistant.backend.entity.MetricDef;
import com.experimentalassistant.backend.entity.TemplateMetricDef;
import com.experimentalassistant.backend.mapper.TemplateMapper;
import com.experimentalassistant.backend.mapper.TemplateFieldMapper;
import com.experimentalassistant.backend.mapper.MetricDefMapper;
import com.experimentalassistant.backend.mapper.TemplateMetricDefMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

@Component
@Slf4j
public class DefaultTemplateInitializer implements CommandLineRunner {

    private final TemplateMapper templateMapper;
    private final TemplateFieldMapper templateFieldMapper;
    private final MetricDefMapper metricDefMapper;
    private final TemplateMetricDefMapper templateMetricDefMapper;
    private final TransactionTemplate transactionTemplate;

    public DefaultTemplateInitializer(TemplateMapper templateMapper,
                                      TemplateFieldMapper templateFieldMapper,
                                      MetricDefMapper metricDefMapper,
                                      TemplateMetricDefMapper templateMetricDefMapper,
                                      PlatformTransactionManager transactionManager) {
        this.templateMapper = templateMapper;
        this.templateFieldMapper = templateFieldMapper;
        this.metricDefMapper = metricDefMapper;
        this.templateMetricDefMapper = templateMetricDefMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            transactionTemplate.executeWithoutResult((status) -> {
                log.info("Checking for default template...");

                Long count = templateMapper.selectCount(new LambdaQueryWrapper<Template>().eq(Template::getIsDefault, true));
                if (count != null && count > 0) {
                    log.info("Default template already exists.");
                    return;
                }

                log.info("Creating default template...");

                Template template = new Template();
                template.setName("Default Deep Learning Template");
                template.setDescription("Standard template for DL experiments with common hyperparameters");
                template.setDomain("Deep Learning");
                template.setIsDefault(true);
                template.setCreatedAt(LocalDateTime.now());
                template.setUpdatedAt(LocalDateTime.now());
                templateMapper.insert(template);

                createField(template.getId(), "model", "Model Name", "TEXT", true, null, 1, null);
                createField(template.getId(), "dataset", "Dataset", "TEXT", true, null, 2, null);
                createField(template.getId(), "epochs", "Epochs", "NUMBER", true, "100", 3, null);
                createField(template.getId(), "batch_size", "Batch Size", "NUMBER", true, "32", 4, null);
                createField(template.getId(), "learning_rate", "Learning Rate", "NUMBER", true, "0.001", 5, null);
                createField(template.getId(), "optimizer", "Optimizer", "SELECT", true, "Adam", 6, "[\"Adam\", \"SGD\", \"RMSprop\"]");
                createField(template.getId(), "seed", "Random Seed", "NUMBER", false, "42", 7, null);
                createField(template.getId(), "note", "Notes", "TEXTAREA", false, null, 8, null);

                linkMetric(template.getId(), "acc", "MAX", 1);
                linkMetric(template.getId(), "loss", "MIN", 2);

                log.info("Default template created successfully with ID: {}", template.getId());
            });
        } catch (Exception e) {
            log.warn("Skip default template initialization: {}", e.getMessage());
        }
    }

    private void createField(Long templateId, String key, String label, String type, boolean required, String defaultVal, int order, String options) {
        TemplateField field = new TemplateField();
        field.setTemplateId(templateId);
        field.setFieldKey(key);
        field.setLabel(label);
        field.setFieldType(type);
        field.setIsRequired(required);
        field.setDefaultValue(defaultVal);
        field.setSortOrder(order);
        field.setOptionsJson(options);
        templateFieldMapper.insert(field);
    }

    private void linkMetric(Long templateId, String name, String direction, int order) {
        MetricDef metric = metricDefMapper.selectOne(new LambdaQueryWrapper<MetricDef>().eq(MetricDef::getName, name));
        if (metric == null) {
            metric = new MetricDef();
            metric.setName(name);
            metric.setDirection(direction);
            metric.setDescription("Default " + name + " metric");
            metricDefMapper.insert(metric);
        }

        TemplateMetricDef tm = new TemplateMetricDef();
        tm.setTemplateId(templateId);
        tm.setMetricDefId(metric.getId());
        tm.setIsDefault(true);
        tm.setSortOrder(order);
        templateMetricDefMapper.insert(tm);
    }
}
