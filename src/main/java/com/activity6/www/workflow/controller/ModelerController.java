package com.activity6.www.workflow.controller;

import com.activity6.www.workflow.common.JumpAnyWhereCmd;
import com.activity6.www.workflow.common.Page;
import com.activity6.www.workflow.common.ResponseData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.odysseus.el.ExpressionFactoryImpl;
import de.odysseus.el.util.SimpleContext;
import lombok.SneakyThrows;
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
import org.apache.ibatis.jdbc.ScriptRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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

    @Autowired
    private JdbcTemplate jdbcTemplate;
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

//    @Resource
//    private TaskEntityManager taskEntityManager;


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
    public List<FlowNode> nextNode(String taskId,String var) {
        Map<String, Object> vars = new HashMap<>();
//        vars.put("isAgree",var);
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
        List<FlowNode> flowElements = new ArrayList<>();
        for (SequenceFlow sequenceFlow : outgoingFlows) {
            //获取的下个节点不一定是userTask的任务节点，所以要判断是否是任务节点
            FlowElement flowElement = sequenceFlow.getTargetFlowElement();
//            if (flowElement instanceof UserTask) {
//                userTask = (UserTask) flowElement;
//                flowNode = (FlowNode) flowElement;
                String conditionExpression = sequenceFlow.getConditionExpression();
                boolean condition = isCondition(conditionExpression, vars);
                if(condition){
                    flowNode  = (FlowNode)flowElement;
                    flowNode.getIncomingFlows().clear();
                    flowNode.getOutgoingFlows().clear();
                    flowElements.add(flowNode);
                }
//            }
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
        return flowElements;
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


   //导入流程
   @SneakyThrows
    @PostMapping(value = "/importResource")
    public ResponseData importResource(@RequestPart(value = "file") MultipartFile file, String flag, String flowId)  {
        Assert.notNull(flag, "标识不能为空！");
        StringBuilder sbr = new StringBuilder();
        try (InputStream inputStream = file.getInputStream();
             InputStreamReader ips = new InputStreamReader(inputStream);
             BufferedReader reader = new BufferedReader(ips)) {
            String line;
            while ((line = reader.readLine()) != null) {
                sbr.append(line);
            }
        }
        if ("1".equals(flag)){
            ScriptRunner runner = new ScriptRunner(jdbcTemplate.getDataSource().getConnection());
            runner.setAutoCommit(true);
            runner.setStopOnError(true);
            runner.setEscapeProcessing(false);
            runner.setSendFullScript(true);
            String scriptStr = sbr.toString().replace("nullFlowId", flowId);
            //替换脚本中的flowId
            ByteArrayInputStream inputStream = new ByteArrayInputStream(scriptStr.getBytes());
            runner.runScript(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            return ResponseData.ok("导入成功");
        }
        if("2".equals(flag)){
            //解析脚本内容，返回JSON
            String[] split = sbr.toString().split("\\('");
            String hexStr = split[1].split("'\\)")[0];
            String str = "0123456789ABCDEF";

            char[] hexs = hexStr.toCharArray();
            byte[] bytes = new byte[hexStr.length() / 2];
            int n;
            for (int i = 0; i < bytes.length; i++) {
                n = str.indexOf(hexs[2 * i]) * 16;
                n += str.indexOf(hexs[2 * i + 1]);
                bytes[i] = (byte) (n & 0xff);
            }
            String resultJson = new String(bytes);
            return ResponseData.ok(resultJson);
        }
        return ResponseData.ok();
    }

    //导出流程
    @SneakyThrows
    @GetMapping(value = "/exportScript")
    public void nextNode(String flowId, HttpServletRequest request, HttpServletResponse response) {
        String modelSql = "SELECT * FROM ACT_GE_BYTEARRAY WHERE ID_ in (SELECT EDITOR_SOURCE_VALUE_ID_ FROM ACT_RE_MODEL where ID_= ?)";
        List<Map<String, String>> query = jdbcTemplate.query(modelSql, new Object[]{flowId}, (resultSet, i) -> {
            final LobHandler lobHandler = new DefaultLobHandler();
            byte[] blobAsBytes = lobHandler.getBlobAsBytes(resultSet, 5);
            String bytes = new String(blobAsBytes);
            HashMap<String, String> map = new HashMap<>(6);
            map.put("ID_", resultSet.getString(1));
            map.put("REV_", resultSet.getString(2));
            map.put("NAME_", resultSet.getString(3));
            map.put("DEPLOYMENT_ID_", resultSet.getString(4));
            map.put("BYTES_", bytes);
            map.put("GENERATED_", resultSet.getString(6));
            return map;
        });
        Map<String, String> map = query.get(0);
        //输出脚本使用hex字符
        String bytes = map.get("BYTES_");
        char[] chars = "0123456789ABCDEF".toCharArray();
        byte[] bs = bytes.getBytes();
        StringBuilder sb = new StringBuilder();
        int bit;
        for (int i = 0; i < bs.length; i++) {
            bit = (bs[i] & 0X0F0) >> 4;
            sb.append(chars[bit]);
            bit = bs[i] & 0X0F;
            sb.append(chars[bit]);
        }
        String deployment_id_ = map.get("DEPLOYMENT_ID_");
        String fileName = map.get("NAME_");
        String scriptBase = "DECLARE\n" +
                "    v_blob BLOB;\n" +
                "BEGIN\n" +
                "    v_blob :=HEXTORAW('v5');\n" +
                "\n" +
                "    INSERT INTO \"AML_PLATFORM\".\"ACT_GE_BYTEARRAY\"(\"ID_\", \"REV_\", \"NAME_\", \"DEPLOYMENT_ID_\", \"BYTES_\", \"GENERATED_\")\n" +
                "    values ('v1', 'v2', 'v3', 'v4', v_blob, 'v6');\n" +
                "end;";
        String generated_ = map.get("GENERATED_");
        String rev_ = map.get("REV_");
        String replace = scriptBase.replace("v1", map.get("ID_"))
                .replace("v2", rev_ == null ? "" : rev_)
                .replace("v3", fileName == null ? "" : fileName)
                .replace("v4", deployment_id_ == null ? "nullFlowId" : "nullFlowId")
                .replace("v5", sb.toString())
                .replace("v6", generated_ == null ? "" : generated_);
        byte[] baseBytes = replace.getBytes(Charset.forName("UTF-8"));
        if (request.getHeader("User-Agent").toLowerCase().indexOf("msie") > 0 || request.getHeader("User-Agent").indexOf("like Gecko") > 0) {
            //IE浏览器
            fileName = URLEncoder.encode(fileName, "UTF-8");
        } else {
            fileName = new String(fileName.getBytes("UTF-8"), "iso-8859-1");
        }
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".sql");
        ServletOutputStream ops = response.getOutputStream();
        ops.write(baseBytes);
    }

}
