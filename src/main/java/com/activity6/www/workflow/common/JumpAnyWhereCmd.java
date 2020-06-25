package com.activity6.www.workflow.common;

import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.Process;
import org.activiti.engine.impl.history.HistoryManager;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.activiti.engine.impl.persistence.entity.TaskEntityManager;
import org.activiti.engine.impl.util.ProcessDefinitionUtil;

/**
 * 跳转任意节点命令
 * Create by Kalvin on 2020/5/1.
 */
public class JumpAnyWhereCmd implements Command<Void> {


    private  String taskId;

    private  String targetNodeId;

    private  String reason;

    /**
     * @param taskId 当前任务ID
     * @param targetNodeId 目标节点定义ID
     */
    public JumpAnyWhereCmd(String taskId, String targetNodeId,String reason) {
        this.taskId = taskId;
        this.targetNodeId = targetNodeId;
        this.reason = reason;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        // 获取任务实例管理类
        TaskEntityManager taskEntityManager = commandContext.getTaskEntityManager();
        // 获取当前任务实例
        TaskEntity currentTask = taskEntityManager.findById(this.taskId);

        // 获取当前节点的执行实例
        ExecutionEntity execution = currentTask.getExecution();
        String executionId = execution.getId();

        // 获取流程定义id
        String processDefinitionId = execution.getProcessDefinitionId();
        // 获取目标节点
        Process process = ProcessDefinitionUtil.getProcess(processDefinitionId);
        FlowElement flowElement = process.getFlowElement(this.targetNodeId);
        // 获取历史管理
        HistoryManager historyManager = commandContext.getHistoryManager();
        // 通知当前活动结束(更新act_hi_actinst)
        historyManager.recordActivityEnd(execution, this.reason);
        // 通知任务节点结束(更新act_hi_taskinst)
        historyManager.recordTaskEnd(this.taskId, this.reason);
        // 删除正在执行的当前任务
        taskEntityManager.delete(taskId);

        // 此时设置执行实例的当前活动节点为目标节点
        execution.setCurrentFlowElement(flowElement);

        // 向operations中压入继续流程的操作类
        commandContext.getAgenda().planContinueProcessOperation(execution);

//        execution = (ExecutionEntity) runtimeService.createProcessInstanceQuery().singleResult();
//        CommandContext cmmContext = Context.getCommandContext();
//        ExecutionEntityManager exeEntityManager = cmmContext.getExecutionEntityManager();


        return null;
    }
}
