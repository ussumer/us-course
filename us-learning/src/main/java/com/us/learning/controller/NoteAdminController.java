package com.us.learning.controller;

import com.us.common.domain.dto.PageDTO;
import com.us.learning.domain.query.NoteAdminQuery;
import com.us.learning.domain.vo.NoteDetailVO;
import com.us.learning.domain.vo.NotePageVO;
import com.us.learning.service.INoteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 学习笔记 前端控制器（管理端，day13~14 分组实战）
 * </p>
 */
@RestController
@RequestMapping("/admin/notes")
@Api(tags = "学习笔记相关接口-管理端")
@RequiredArgsConstructor
public class NoteAdminController {

    private final INoteService noteService;

    @ApiOperation("管理端分页查询笔记")
    @GetMapping("page")
    public PageDTO<NotePageVO> queryNotePageForAdmin(NoteAdminQuery query) {
        return noteService.queryNotePageForAdmin(query);
    }

    @ApiOperation("管理端查询笔记详情")
    @GetMapping("/{id}")
    public NoteDetailVO queryNoteDetailForAdmin(@ApiParam("笔记id") @PathVariable("id") Long id) {
        return noteService.queryNoteDetailForAdmin(id);
    }

    @ApiOperation("隐藏或显示笔记")
    @PutMapping("/{id}/hidden/{hidden}")
    public void updateNoteHidden(@ApiParam("笔记id") @PathVariable("id") Long id,
                                 @PathVariable("hidden") Boolean hidden) {
        noteService.updateNoteHidden(id, hidden);
    }
}
