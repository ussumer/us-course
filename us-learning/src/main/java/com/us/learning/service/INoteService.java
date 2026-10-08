package com.us.learning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.us.common.domain.dto.PageDTO;
import com.us.learning.domain.dto.NoteEditDTO;
import com.us.learning.domain.dto.NoteFormDTO;
import com.us.learning.domain.po.Note;
import com.us.learning.domain.query.NoteAdminQuery;
import com.us.learning.domain.query.NotePageQuery;
import com.us.learning.domain.vo.NoteDetailVO;
import com.us.learning.domain.vo.NotePageVO;
import com.us.learning.domain.vo.NoteVO;

/**
 * <p>
 * 学习笔记 服务类（day13~14 分组实战）
 * </p>
 */
public interface INoteService extends IService<Note> {

    /**
     * 新增笔记（笔记 3.1.1）
     *
     * @param dto 笔记表单
     */
    void saveNote(NoteFormDTO dto);

    /**
     * 采集笔记（笔记 3.1.2）
     *
     * @param id 被采集的笔记id
     */
    void gatherNote(Long id);

    /**
     * 取消采集笔记（笔记 3.1.2）
     *
     * @param id 被采集的笔记id
     */
    void removeGatherNote(Long id);

    /**
     * 编辑笔记（笔记 3.1.3）
     *
     * @param id  笔记id
     * @param dto 笔记表单
     */
    void updateNote(Long id, NoteEditDTO dto);

    /**
     * 删除笔记（笔记 3.1.3）
     *
     * @param id 笔记id
     */
    void deleteNote(Long id);

    /**
     * 用户端分页查询笔记（笔记 3.1.4）
     *
     * @param query 查询条件
     * @return 笔记分页结果
     */
    PageDTO<NoteVO> queryNotePage(NotePageQuery query);

    /**
     * 管理端分页查询笔记（笔记 3.1.5）
     *
     * @param query 查询条件
     * @return 笔记分页结果
     */
    PageDTO<NotePageVO> queryNotePageForAdmin(NoteAdminQuery query);

    /**
     * 管理端查询笔记详情（笔记 3.1.6）
     *
     * @param id 笔记id
     * @return 笔记详情
     */
    NoteDetailVO queryNoteDetailForAdmin(Long id);

    /**
     * 隐藏或显示笔记（笔记 3.1.7）
     *
     * @param id     笔记id
     * @param hidden 是否隐藏
     */
    void updateNoteHidden(Long id, Boolean hidden);
}
