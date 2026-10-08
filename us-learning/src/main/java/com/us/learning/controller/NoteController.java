package com.us.learning.controller;

import com.us.common.domain.dto.PageDTO;
import com.us.learning.domain.dto.NoteEditDTO;
import com.us.learning.domain.dto.NoteFormDTO;
import com.us.learning.domain.query.NotePageQuery;
import com.us.learning.domain.vo.NoteVO;
import com.us.learning.service.INoteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * <p>
 * 学习笔记 前端控制器（用户端，day13~14 分组实战）
 * </p>
 */
@RestController
@RequestMapping("/notes")
@Api(tags = "学习笔记相关接口")
@RequiredArgsConstructor
public class NoteController {

    private final INoteService noteService;

    @ApiOperation("新增笔记")
    @PostMapping
    public void saveNote(@RequestBody @Valid NoteFormDTO dto) {
        noteService.saveNote(dto);
    }

    @ApiOperation("采集笔记")
    @PostMapping("/gathers/{id}")
    public void gatherNote(@ApiParam("被采集的笔记id") @PathVariable("id") Long id) {
        noteService.gatherNote(id);
    }

    @ApiOperation("取消采集笔记")
    @DeleteMapping("/gathers/{id}")
    public void removeGatherNote(@ApiParam("被采集的笔记id") @PathVariable("id") Long id) {
        noteService.removeGatherNote(id);
    }

    @ApiOperation("编辑笔记")
    @PutMapping("/{id}")
    public void updateNote(@ApiParam("笔记id") @PathVariable("id") Long id,
                           @RequestBody @Valid NoteEditDTO dto) {
        noteService.updateNote(id, dto);
    }

    @ApiOperation("删除笔记")
    @DeleteMapping("/{id}")
    public void deleteNote(@ApiParam("笔记id") @PathVariable("id") Long id) {
        noteService.deleteNote(id);
    }

    @ApiOperation("用户端分页查询笔记")
    @GetMapping("/page")
    public PageDTO<NoteVO> queryNotePage(NotePageQuery query) {
        return noteService.queryNotePage(query);
    }
}
