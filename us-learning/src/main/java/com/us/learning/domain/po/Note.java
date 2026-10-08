package com.us.learning.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 学习笔记（day13~14 分组实战：学习笔记功能）
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("note")
public class Note implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 笔记id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 笔记作者id
     */
    private Long userId;

    /**
     * 课程id
     */
    private Long courseId;

    /**
     * 章id
     */
    private Long chapterId;

    /**
     * 小节id
     */
    private Long sectionId;

    /**
     * 笔记内容
     */
    private String content;

    /**
     * 笔记记录时的视频时间点，单位秒
     */
    private Integer noteMoment;

    /**
     * 是否为私密笔记，true：私密，false：公开
     */
    private Boolean isPrivate;

    /**
     * 是否在用户端隐藏（管理端操作），true：隐藏
     */
    private Boolean hidden;

    /**
     * 采集自哪条笔记的id，null表示原创笔记
     */
    private Long gatherId;

    /**
     * 被采集次数
     */
    private Integer gatherTimes;

    /**
     * 课程名称（冗余字段，便于管理端按课程名搜索、避免列表页反复调用课程服务）
     */
    private String courseName;

    /**
     * 章名称（冗余字段）
     */
    private String chapterName;

    /**
     * 小节名称（冗余字段）
     */
    private String sectionName;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
