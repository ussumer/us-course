package com.us.learning.domain.vo;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.us.common.utils.CollUtils;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 学习计划页面的返回值（统计部分 + 分页部分）
 * <p>课前资料只提供了原型截图未提供源码，字段按 day03 笔记 1.2.4 的接口规格定义，
 * 并实现了笔记中用到的 emptyPage / pageInfo 两个组装方法</p>
 */
@Data
@ApiModel(description = "学习计划页面的返回值")
public class LearningPlanPageVO {

    @ApiModelProperty("本周学习积分")
    private Integer weekPoints;

    @ApiModelProperty("本周已学完小节数量")
    private Integer weekFinished;

    @ApiModelProperty("本周计划学习小节数量")
    private Integer weekTotalPlan;

    @ApiModelProperty("总条数")
    private Long total;

    @ApiModelProperty("总页数")
    private Long pages;

    @ApiModelProperty("当前页数据")
    private List<LearningPlanVO> list;

    public LearningPlanPageVO emptyPage(Page<?> page) {
        this.total = page.getTotal();
        this.pages = page.getPages();
        this.list = CollUtils.emptyList();
        return this;
    }

    public LearningPlanPageVO pageInfo(Long total, Long pages, List<LearningPlanVO> list) {
        this.total = total;
        this.pages = pages;
        this.list = list;
        return this;
    }
}
