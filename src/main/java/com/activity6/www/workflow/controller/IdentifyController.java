package com.activity6.www.workflow.controller;

import com.activity6.www.workflow.common.Page;
import com.activity6.www.workflow.common.ResponseData;
import org.activiti.engine.IdentityService;
import org.activiti.engine.identity.Group;
import org.activiti.engine.identity.GroupQuery;
import org.activiti.engine.identity.User;
import org.activiti.engine.identity.UserQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/identify")
public class IdentifyController {

    @Autowired
    private IdentityService identityService;

    /**
     * 用户组/岗位/机构
     *
     * @return
     */


    @GetMapping(value = "/groupList")
    public Page groupList(Page page) {
        page.reason();
        GroupQuery groupQuery = identityService.createGroupQuery();
        long count = groupQuery.count();
        List<Group> groups = groupQuery.listPage(page.getFirstRow(), page.getPageSize());
        page.setRows(groups);
        page.setTotal(count);
        return page;
    }

    /**
     * 用户列表
     *
     * @return
     */

    @GetMapping(value = "/userList")
    public Page userList(Page page) {
        page.reason();
        UserQuery userQuery = identityService.createUserQuery();
        long count = userQuery.count();
        List<User> users = userQuery.listPage(page.getFirstRow(), page.getPageSize());
        page.setRows(users);
        page.setTotal(count);
        return page;
    }


}
