package com.activity6.www.workflow.common;

import java.io.Serializable;
import java.util.List;

public class Page<E> implements Serializable {


    //当前页面号
    private int currentPage;
    //每页行数
    private int pageSize;
    //总行数
    private long total;
    //总行数
    private List<E> rows;
    //起始行
    private int firstRow;

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public List<E> getRows() {
        return rows;
    }

    public void setRows(List<E> rows) {
        this.rows = rows;
    }

    public int getFirstRow() {
        return firstRow;
    }

    public void setFirstRow(int firstRow) {
        this.firstRow = firstRow;
    }

    public Page() {

    }

    public void reason() {
        int currentPage = this.getCurrentPage() <= 0 ? 1 : this.getCurrentPage();
        int pageSize = this.getPageSize() <= 0 ? 10 : this.getPageSize();
        this.setCurrentPage(currentPage);
        this.setPageSize(pageSize);
        this.setFirstRow((currentPage - 1) * pageSize);
    }
}