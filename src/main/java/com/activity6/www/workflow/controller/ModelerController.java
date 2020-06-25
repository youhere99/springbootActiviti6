package com.activity6.www.workflow.controller;

import com.activity6.www.workflow.common.JumpAnyWhereCmd;
import com.activity6.www.workflow.common.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.odysseus.el.ExpressionFactoryImpl;
import de.odysseus.el.util.SimpleContext;
import lombok.extern.slf4j.Slf4j;
import org.activiti.bpmn.model.*;
import org.activiti.editor.constants.ModelDataJsonConstants;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.impl.TaskServiceImpl;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.ExecutionEntityManager;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.persistence.entity.TaskEntityManager;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.Model;
import org.activiti.engine.repository.ModelQuery;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.image.ProcessDiagramGenerator;
import org.activiti.image.impl.DefaultProcessDiagramGenerator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程控制器
 * liuzhize 2019年3月7日下午3:28:14
 */
@Controller
@Slf4j
public class ModelerController {

    private static final Logger logger = LoggerFactory.getLogger(ModelerController.class);

    @Resource
    private RepositoryService repositoryService;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private HistoryService historyService;
    @Resource
    private RuntimeService runtimeService;
    @Resource
    private TaskService taskService;

    @Resource
    private TaskEntityManager taskEntityManager;


    /**
     * 跳转编辑器页面
     *
     * @return
     */
    @GetMapping("editor")
    public String editor() {
        return "modeler";
    }


    /**
     * 创建模型
     *
     * @param response
     * @throws IOException
     */
    @RequestMapping("/create")
    public void create(HttpServletResponse response, String name, String key) throws IOException {
        logger.info("创建模型入参name：{},key:{}", name, key);
        Model model = repositoryService.newModel();
        ObjectNode modelNode = objectMapper.createObjectNode();
        modelNode.put(ModelDataJsonConstants.MODEL_NAME, name);
        modelNode.put(ModelDataJsonConstants.MODEL_DESCRIPTION, "");
        modelNode.put(ModelDataJsonConstants.MODEL_REVISION, 1);
        model.setName(name);
        model.setKey(key);
        model.setMetaInfo(modelNode.toString());
        repositoryService.saveModel(model);
        createObjectNode(model.getId());
        response.sendRedirect("editor?modelId=" + model.getId());
        logger.info("创建模型结束，返回模型ID：{}", model.getId());

    }

    /**
     * 创建模型时完善ModelEditorSource
     *
     * @param modelId
     */
    @SuppressWarnings("deprecation")
    private void createObjectNode(String modelId) {
        logger.info("创建模型完善ModelEditorSource入参模型ID：{}", modelId);
        ObjectNode editorNode = objectMapper.createObjectNode();
        editorNode.put("id", "canvas");
        editorNode.put("resourceId", "canvas");
        ObjectNode stencilSetNode = objectMapper.createObjectNode();
        stencilSetNode.put("namespace", "http://b3mn.org/stencilset/bpmn2.0#");
        editorNode.put("stencilset", stencilSetNode);
        try {
            repositoryService.addModelEditorSource(modelId, editorNode.toString().getBytes("utf-8"));
        } catch (Exception e) {
            logger.info("创建模型时完善ModelEditorSource服务异常：{}", e);
        }
        logger.info("创建模型完善ModelEditorSource结束");
    }

    /**
     * 发布模型
     *
     * @param modelId 模型ID
     * @return
     */
    @ResponseBody
    @RequestMapping("/publish")
    public Object publish(String modelId) {
        logger.info("流程部署入参modelId：{}", modelId);
        Map<String, String> map = new HashMap<String, String>();
        try {
            Model modelData = repositoryService.getModel(modelId);
            byte[] bytes = repositoryService.getModelEditorSource(modelData.getId());
            if (bytes == null) {
                logger.info("部署ID:{}的模型数据为空，请先设计流程并成功保存，再进行发布", modelId);
                map.put("code", "FAILURE");
                return map;
            }
            JsonNode modelNode = new ObjectMapper().readTree(bytes);
            BpmnModel model = new BpmnJsonConverter().convertToBpmnModel(modelNode);
            Deployment deployment = repositoryService.createDeployment()
                    .name(modelData.getName())
                    .addBpmnModel(modelData.getKey() + ".bpmn20.xml", model)
                    .deploy();
            modelData.setDeploymentId(deployment.getId());
            repositoryService.saveModel(modelData);
            map.put("code", "SUCCESS");
        } catch (Exception e) {
            logger.info("部署modelId:{}模型服务异常：{}", modelId, e);
            map.put("code", "FAILURE");
        }
        logger.info("流程部署出参map：{}", map);
        return map;
    }

    /**
     * 模型列表
     *
     * @return
     */
    @ResponseBody
    @RequestMapping("/list")
    public Object models(Page page) {
        page.reason();
        ModelQuery modelQuery = repositoryService.createModelQuery();
        long count = modelQuery.count();
        List<Model> models = modelQuery.listPage(page.getFirstRow(), page.getPageSize());
        page.setRows(models);
        page.setTotal(count);
        return page;
    }

    @ResponseBody
    @RequestMapping("/start/{processDefinitionId}")
    public String start(@PathVariable("processDefinitionId") String processDefinitionId) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinitionId);
        return "success";
    }

    @ResponseBody
    @RequestMapping("/complate/{taskId}/{var}")
    public String complate(@PathVariable("taskId") String taskId,@PathVariable("var") String var) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("isAgree",var);
        taskService.complete(taskId,vars);
        return "success";
    }

    @ResponseBody
    @RequestMapping("/free/{taskId}/{nodeId}")
    public String free(@PathVariable("taskId") String taskId, @PathVariable("nodeId") String nodeId) {
        if (StringUtils.isBlank(nodeId)) {
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());
            FlowNode flowNode = (FlowNode) bpmnModel.getFlowElement(task.getTaskDefinitionKey());
            List<SequenceFlow> incomingFlows = flowNode.getIncomingFlows();
            nodeId = incomingFlows.get(0).getSourceFlowElement().getId();
        }
        CommandExecutor commandExecutor = ((TaskServiceImpl) taskService).getCommandExecutor();
        commandExecutor.execute(new JumpAnyWhereCmd(taskId, nodeId, "reason"));
        return "success";
    }

    /**
     * 撤销流程定义
     *
     * @param modelId 模型ID
     * @return
     */
    @ResponseBody
    @RequestMapping("/revokePublish")
    public Object revokePublish(String modelId) {
        logger.info("撤销发布流程入参modelId：{}", modelId);
        Map<String, String> map = new HashMap<String, String>();
        Model modelData = repositoryService.getModel(modelId);
        if (null != modelData) {
            try {
                /**
                 * 参数不加true:为普通删除，如果当前规则下有正在执行的流程，则抛异常
                 * 参数加true:为级联删除,会删除和当前规则相关的所有信息，包括历史
                 */
                repositoryService.deleteDeployment(modelData.getDeploymentId(), true);
                map.put("code", "SUCCESS");
            } catch (Exception e) {
                logger.error("撤销已部署流程服务异常：{}", e);
                map.put("code", "FAILURE");
            }
        }
        logger.info("撤销发布流程出参map：{}", map);
        return map;
    }

    /**
     * 删除流程实例
     *
     * @param modelId 模型ID
     * @return
     */
    @ResponseBody
    @RequestMapping("/delete")
    public Object deleteProcessInstance(String modelId) {
        logger.info("删除流程实例入参modelId：{}", modelId);
        Map<String, String> map = new HashMap<>();
        Model modelData = repositoryService.getModel(modelId);

        if (null != modelData) {
            try {
                ProcessInstance pi = runtimeService.createProcessInstanceQuery().processDefinitionKey(modelData.getKey()).singleResult();
                if (null != pi) {
                    runtimeService.deleteProcessInstance(pi.getId(), "");
                    historyService.deleteHistoricProcessInstance(pi.getId());

                }

                map.put("code", "SUCCESS");
            } catch (Exception e) {
                logger.error("删除流程实例服务异常：{}", e);
                map.put("code", "FAILURE");
            }
        }

        logger.info("删除流程实例出参map：{}", map);
        return map;
    }

    @RequestMapping(value = "/image/{pid}", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public byte[] definitionImage(@PathVariable("pid") String processDefinitionId) throws IOException {

        BpmnModel model = repositoryService.getBpmnModel(processDefinitionId);
        if (model != null && model.getLocationMap().size() > 0) {
            ProcessDiagramGenerator generator = new DefaultProcessDiagramGenerator();
            InputStream imageStream = generator.generateDiagram(model, "png", new ArrayList<>());
            byte[] buffer = new byte[imageStream.available()];
            imageStream.read(buffer);
            imageStream.close();
            return buffer;
        }


        return new byte[0];
    }

    @GetMapping("/showImage")
    public String image() {

        return "image";
    }


    @RequestMapping(value = "/image2/{pid}", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public byte[] getProcessImage(@PathVariable("pid") String processInstanceId) throws Exception {

        //  获取历史流程实例

        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        if (historicProcessInstance == null) {
            throw new Exception();
        } else {
            // 获取流程定义
            ProcessDefinitionEntity processDefinition = (ProcessDefinitionEntity) repositoryService
                    .getProcessDefinition(historicProcessInstance.getProcessDefinitionId());

            // 获取流程历史中已执行节点，并按照节点在流程中执行先后顺序排序
            List<HistoricActivityInstance> historicActivityInstanceList = historyService
                    .createHistoricActivityInstanceQuery().processInstanceId(processInstanceId)
                    .orderByHistoricActivityInstanceId().asc().list();

            // 已执行的节点ID集合
            List<String> executedActivityIdList = new ArrayList<>();
            @SuppressWarnings("unused") int index = 1;
            log.info("获取已经执行的节点ID");
            for (HistoricActivityInstance activityInstance : historicActivityInstanceList) {
                executedActivityIdList.add(activityInstance.getActivityId());
                log.info("第[" + index + "]个已执行节点=" + activityInstance.getActivityId() + " : " + activityInstance
                        .getActivityName());

                index++;
            }
            // 获取流程图图像字符流
            BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
            DefaultProcessDiagramGenerator generator = new DefaultProcessDiagramGenerator();
            InputStream imageStream = generator.generateDiagram(bpmnModel, "png", executedActivityIdList);
            byte[] buffer = new byte[imageStream.available()];
            imageStream.read(buffer);
            imageStream.close();

            return buffer;
        }

    }

    /**
     * 获取当前流程的上节点/下节点
     *
     * @param taskId
     * @return
     */
    @ResponseBody
    @GetMapping(value = "nextNode")
    public UserTask nextNode(String taskId,String var) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("isAgree",var);
        //任务
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        //当前执行流程任务
        Execution execution = runtimeService.createExecutionQuery().executionId(task.getExecutionId()).singleResult();
        //当前活动节点
        String activityId = execution.getActivityId();
        // 取得已提交的任务
//        HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery()
//                .taskId(taskId).singleResult();
//        ProcessDefinition processDefinition =repositoryService.createProcessDefinitionQuery().processDefinitionKey("processA").latestVersion().singleResult();
        //获得当前流程的活动ID
//         processDefinition = repositoryService.getProcessDefinition(historicTaskInstance.getProcessDefinitionId());

        UserTask userTask = null;
        //根据活动节点获取当前的组件信息
        BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());
        FlowNode flowNode = (FlowNode) bpmnModel.getFlowElement(activityId);
        System.out.println("flowNode1--" + flowNode);
        flowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(activityId);
        System.out.println("flowNode2--" + flowNode);
        //获取该流程组件的之后/
        List<SequenceFlow> outgoingFlows = flowNode.getOutgoingFlows();
        for (SequenceFlow sequenceFlow : outgoingFlows) {
            //获取的下个节点不一定是userTask的任务节点，所以要判断是否是任务节点
            FlowElement flowElement = sequenceFlow.getTargetFlowElement();
            if (flowElement instanceof UserTask) {
                userTask = (UserTask) flowElement;
//                flowNode = (FlowNode) flowElement;
                String conditionExpression = sequenceFlow.getConditionExpression();
                boolean condition = isCondition(conditionExpression, vars);
                System.out.println(condition);
            }
        }

        //之前的组件信息
        List<SequenceFlow> incomingFlows = flowNode.getIncomingFlows();
        for (SequenceFlow sequenceFlow : incomingFlows) {
            //获取的下个节点不一定是userTask的任务节点，所以要判断是否是任务节点
            FlowElement flowElement = sequenceFlow.getSourceFlowElement();
            if (flowElement instanceof UserTask) {
                userTask = (UserTask) flowElement;
            }


        }
        return userTask;
    }


    /**
     * 根据key和value判断el表达式是否通过信息
     * @return
     */
    public boolean isCondition(String el,Map<String, Object> vars) {
        if(vars==null||vars.isEmpty()){
            return  true;
        }
        ExpressionFactory factory = new ExpressionFactoryImpl();
        SimpleContext context = new SimpleContext();
        for (Object k : vars.keySet()) {
            if (vars.get(k) != null) {
                context.setVariable(k.toString(), factory.createValueExpression(vars.get(k), vars.get(k).getClass()));
            }
        }

        ValueExpression e = factory.createValueExpression(context, el, Boolean.class);
        return (Boolean) e.getValue(context);
    }
}
