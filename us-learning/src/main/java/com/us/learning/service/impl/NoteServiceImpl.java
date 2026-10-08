package com.us.learning.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.us.api.cache.CategoryCache;
import com.us.api.client.course.CourseClient;
import com.us.api.client.user.UserClient;
import com.us.api.dto.course.CatalogueDTO;
import com.us.api.dto.course.CourseFullInfoDTO;
import com.us.api.dto.user.UserDTO;
import com.us.common.autoconfigure.mq.RabbitMqHelper;
import com.us.common.constants.MqConstants;
import com.us.common.domain.dto.PageDTO;
import com.us.common.exceptions.BadRequestException;
import com.us.common.exceptions.BizIllegalException;
import com.us.common.utils.BeanUtils;
import com.us.common.utils.CollUtils;
import com.us.common.utils.StringUtils;
import com.us.common.utils.UserContext;
import com.us.learning.domain.dto.NoteEditDTO;
import com.us.learning.domain.dto.NoteFormDTO;
import com.us.learning.domain.po.Note;
import com.us.learning.domain.query.NoteAdminQuery;
import com.us.learning.domain.query.NotePageQuery;
import com.us.learning.domain.vo.NoteDetailVO;
import com.us.learning.domain.vo.NotePageVO;
import com.us.learning.domain.vo.NoteVO;
import com.us.learning.mapper.NoteMapper;
import com.us.learning.service.INoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 学习笔记 服务实现类（day13~14 分组实战）
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoteServiceImpl extends ServiceImpl<NoteMapper, Note> implements INoteService {

    private final CourseClient courseClient;

    private final UserClient userClient;

    private final CategoryCache categoryCache;

    private final RabbitMqHelper mqHelper;

    /**
     * 新增笔记（笔记 3.1.1 + 2.3：写笔记后发送MQ消息增加积分）
     */
    @Override
    public void saveNote(NoteFormDTO dto) {
        Long userId = UserContext.getUser();
        // 1.查询课程信息（同时拿到课程名称与目录，用于冗余存储，便于后续管理端搜索与展示）
        CourseFullInfoDTO courseInfo = courseClient.getCourseInfoById(dto.getCourseId(), true, false);
        if (courseInfo == null) {
            throw new BadRequestException("课程不存在");
        }
        // 2.转PO
        Note note = BeanUtils.copyBean(dto, Note.class);
        note.setUserId(userId);
        note.setHidden(false);
        note.setGatherTimes(0);
        note.setIsPrivate(Boolean.TRUE.equals(dto.getIsPrivate()));
        note.setCourseName(courseInfo.getName());
        fillChapterAndSectionName(note, courseInfo, dto.getChapterId(), dto.getSectionId());
        // 3.保存
        save(note);
        // 4.发送MQ消息，增加用户积分（积分规则：写笔记+N，每日有上限）
        mqHelper.send(MqConstants.Exchange.LEARNING_EXCHANGE, MqConstants.Key.WRITE_NOTE, userId);
    }

    /**
     * 采集笔记（笔记 3.1.2）：把他人笔记复制到我的笔记中并做私密处理
     */
    @Override
    @Transactional
    public void gatherNote(Long id) {
        Long userId = UserContext.getUser();
        // 1.查询被采集的笔记
        Note note = getById(id);
        if (note == null) {
            throw new BadRequestException("笔记不存在");
        }
        // 2.不能采集自己的笔记
        if (userId.equals(note.getUserId())) {
            throw new BizIllegalException("不能采集自己的笔记");
        }
        // 2.1.私密笔记或已被管理端隐藏的笔记不允许被采集（否则可绕过"全部笔记"的过滤直接拿内容）
        if (Boolean.TRUE.equals(note.getIsPrivate())) {
            throw new BizIllegalException("私密笔记不能被采集");
        }
        if (Boolean.TRUE.equals(note.getHidden())) {
            throw new BizIllegalException("该笔记已被隐藏，不能被采集");
        }
        // 3.幂等校验：是否已经采集过
        Integer count = lambdaQuery()
                .eq(Note::getUserId, userId)
                .eq(Note::getGatherId, id)
                .count();
        if (count != null && count > 0) {
            throw new BizIllegalException("已经采集过该笔记");
        }
        // 4.复制一条笔记到我的笔记中，且强制私密
        Note myNote = new Note();
        myNote.setUserId(userId);
        myNote.setCourseId(note.getCourseId());
        myNote.setChapterId(note.getChapterId());
        myNote.setSectionId(note.getSectionId());
        myNote.setContent(note.getContent());
        myNote.setNoteMoment(note.getNoteMoment());
        myNote.setIsPrivate(true);
        myNote.setHidden(false);
        myNote.setGatherId(id);
        myNote.setGatherTimes(0);
        myNote.setCourseName(note.getCourseName());
        myNote.setChapterName(note.getChapterName());
        myNote.setSectionName(note.getSectionName());
        save(myNote);
        // 5.原笔记被采集次数+1
        incrGatherTimes(id, 1);
        // 6.发送MQ消息，增加用户积分
        mqHelper.send(MqConstants.Exchange.LEARNING_EXCHANGE, MqConstants.Key.WRITE_NOTE, userId);
    }

    /**
     * 取消采集笔记（笔记 3.1.2）
     */
    @Override
    @Transactional
    public void removeGatherNote(Long id) {
        Long userId = UserContext.getUser();
        // 1.查询我采集的笔记（gatherId为被采集的原笔记id）
        //   这里用list而不是one()，避免并发下出现重复采集记录时直接抛TooManyResultsException
        List<Note> gathers = lambdaQuery()
                .eq(Note::getUserId, userId)
                .eq(Note::getGatherId, id)
                .list();
        if (CollUtils.isEmpty(gathers)) {
            // 没有采集过，直接结束
            return;
        }
        // 2.删除我的采集记录（正常只有一条，重复采集产生的脏数据一并清理）
        removeByIds(gathers.stream().map(Note::getId).collect(Collectors.toList()));
        // 3.原笔记被采集次数-1
        incrGatherTimes(id, -1);
    }

    /**
     * 编辑笔记（笔记 3.1.3：只能修改内容和是否私密）
     */
    @Override
    public void updateNote(Long id, NoteEditDTO dto) {
        Long userId = UserContext.getUser();
        // 1.查询笔记
        Note note = getById(id);
        if (note == null) {
            throw new BadRequestException("笔记不存在");
        }
        // 2.只能编辑自己的笔记
        if (!userId.equals(note.getUserId())) {
            throw new BizIllegalException("不能编辑他人笔记");
        }
        // 3.更新（笔记与课程、章节绑定后不可修改）
        Note n = new Note();
        n.setId(id);
        n.setContent(dto.getContent());
        n.setIsPrivate(Boolean.TRUE.equals(dto.getIsPrivate()));
        updateById(n);
    }

    /**
     * 删除笔记（笔记 3.1.3）
     */
    @Override
    @Transactional
    public void deleteNote(Long id) {
        Long userId = UserContext.getUser();
        // 1.查询笔记
        Note note = getById(id);
        if (note == null) {
            return;
        }
        // 2.只能删除自己的笔记
        if (!userId.equals(note.getUserId())) {
            throw new BizIllegalException("不能删除他人笔记");
        }
        // 3.删除
        removeById(id);
        // 4.如果删除的是采集来的笔记，源笔记的采集次数要-1
        if (note.getGatherId() != null) {
            incrGatherTimes(note.getGatherId(), -1);
        }
    }

    /**
     * 用户端分页查询笔记（笔记 3.1.4）
     * 全部笔记：只展示公开、未隐藏、非采集的笔记；我的笔记：展示自己的全部笔记（含私密与采集的）
     */
    @Override
    public PageDTO<NoteVO> queryNotePage(NotePageQuery query) {
        Long userId = UserContext.getUser();
        boolean onlyMine = Boolean.TRUE.equals(query.getOnlyMine());
        // 1.分页查询
        Page<Note> page = lambdaQuery()
                .eq(query.getCourseId() != null, Note::getCourseId, query.getCourseId())
                .eq(query.getSectionId() != null, Note::getSectionId, query.getSectionId())
                .eq(onlyMine, Note::getUserId, userId)
                .eq(!onlyMine, Note::getIsPrivate, false)
                .eq(!onlyMine, Note::getHidden, false)
                .isNull(!onlyMine, Note::getGatherId)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        // 2.处理数据
        List<Note> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(page);
        }
        // 2.1.查询作者信息
        Set<Long> uIds = records.stream().map(Note::getUserId).collect(Collectors.toSet());
        List<UserDTO> users = userClient.queryUserByIds(uIds);
        Map<Long, UserDTO> userMap = CollUtils.isEmpty(users) ?
                Collections.emptyMap() :
                users.stream().collect(Collectors.toMap(UserDTO::getId, u -> u));
        // 2.2.转换VO
        List<NoteVO> list = new ArrayList<>(records.size());
        for (Note n : records) {
            NoteVO vo = BeanUtils.copyBean(n, NoteVO.class, (s, t) -> t.setIsGathered(s.getGatherId() != null));
            UserDTO author = userMap.get(n.getUserId());
            if (author != null) {
                vo.setAuthorId(author.getId());
                vo.setAuthorName(author.getName());
                vo.setAuthorIcon(author.getIcon());
            }
            list.add(vo);
        }
        return PageDTO.of(page, list);
    }

    /**
     * 管理端分页查询笔记（笔记 3.1.5）：只展示用户发布的公开笔记（不含私密、不含采集）
     */
    @Override
    public PageDTO<NotePageVO> queryNotePageForAdmin(NoteAdminQuery query) {
        // 1.分页查询
        Page<Note> page = lambdaQuery()
                .like(StringUtils.isNotBlank(query.getName()), Note::getCourseName, query.getName())
                .eq(query.getHidden() != null, Note::getHidden, query.getHidden())
                .ge(query.getBeginTime() != null, Note::getCreateTime, query.getBeginTime())
                .le(query.getEndTime() != null, Note::getCreateTime, query.getEndTime())
                .eq(Note::getIsPrivate, false)
                .isNull(Note::getGatherId)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        // 2.处理数据
        List<Note> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(page);
        }
        // 2.1.查询作者信息
        Set<Long> uIds = records.stream().map(Note::getUserId).collect(Collectors.toSet());
        List<UserDTO> users = userClient.queryUserByIds(uIds);
        Map<Long, UserDTO> userMap = CollUtils.isEmpty(users) ?
                Collections.emptyMap() :
                users.stream().collect(Collectors.toMap(UserDTO::getId, u -> u));
        // 2.2.转换VO
        List<NotePageVO> list = new ArrayList<>(records.size());
        for (Note n : records) {
            NotePageVO vo = BeanUtils.copyBean(n, NotePageVO.class);
            // 被采集次数：PO中是gatherTimes，接口中叫usedTimes
            vo.setUsedTimes(n.getGatherTimes());
            UserDTO author = userMap.get(n.getUserId());
            if (author != null) {
                vo.setAuthorName(author.getName());
            }
            list.add(vo);
        }
        return PageDTO.of(page, list);
    }

    /**
     * 管理端查询笔记详情（笔记 3.1.6）
     */
    @Override
    public NoteDetailVO queryNoteDetailForAdmin(Long id) {
        // 1.查询笔记
        Note note = getById(id);
        if (note == null) {
            throw new BadRequestException("笔记不存在");
        }
        NoteDetailVO vo = BeanUtils.copyBean(note, NoteDetailVO.class);
        // 2.被采集次数
        vo.setUsedTimes(note.getGatherTimes());
        // 3.课程分类名称（形如：IT互联网/后端开发/Java）
        CourseFullInfoDTO course = courseClient.getCourseInfoById(note.getCourseId(), false, false);
        if (course != null && course.getThirdCateId() != null) {
            vo.setCategoryNames(categoryCache.getNameByLv3Id(course.getThirdCateId()));
        }
        // 4.作者信息（名称、电话）
        UserDTO author = userClient.queryUserById(note.getUserId());
        if (author != null) {
            vo.setAuthorName(author.getName());
            vo.setAuthorPhone(author.getCellPhone());
        }
        // 5.引用人（采集过该笔记的用户名称列表）
        List<Note> gathers = lambdaQuery().eq(Note::getGatherId, id).list();
        if (CollUtils.isEmpty(gathers)) {
            vo.setGathers(CollUtils.emptyList());
            return vo;
        }
        Set<Long> uIds = gathers.stream().map(Note::getUserId).collect(Collectors.toSet());
        List<UserDTO> users = userClient.queryUserByIds(uIds);
        vo.setGathers(CollUtils.isEmpty(users) ? CollUtils.emptyList() :
                users.stream().map(UserDTO::getName).collect(Collectors.toList()));
        return vo;
    }

    /**
     * 隐藏或显示笔记（笔记 3.1.7）
     */
    @Override
    public void updateNoteHidden(Long id, Boolean hidden) {
        // 1.查询笔记
        Note note = getById(id);
        if (note == null) {
            throw new BadRequestException("笔记不存在");
        }
        // 2.更新隐藏状态
        Note n = new Note();
        n.setId(id);
        n.setHidden(hidden);
        updateById(n);
    }

    /**
     * 从课程目录树中查找章、节名称并填充到笔记上
     */
    private void fillChapterAndSectionName(Note note, CourseFullInfoDTO courseInfo, Long chapterId, Long sectionId) {
        List<CatalogueDTO> chapters = courseInfo.getChapters();
        if (CollUtils.isEmpty(chapters)) {
            return;
        }
        for (CatalogueDTO chapter : chapters) {
            // 1.匹配章
            if (chapterId != null && chapterId.equals(chapter.getId())) {
                note.setChapterName(chapter.getName());
            }
            if (CollUtils.isEmpty(chapter.getSections())) {
                continue;
            }
            // 2.匹配节
            for (CatalogueDTO section : chapter.getSections()) {
                if (sectionId != null && sectionId.equals(section.getId())) {
                    note.setSectionName(section.getName());
                    if (note.getChapterName() == null) {
                        // 只传了节id，章名从父节点补齐
                        note.setChapterName(chapter.getName());
                    }
                    return;
                }
            }
        }
    }

    /**
     * 增减笔记的被采集次数（不会小于0）
     */
    private void incrGatherTimes(Long noteId, int increment) {
        lambdaUpdate()
                .setSql("gather_times = gather_times + " + increment)
                .eq(Note::getId, noteId)
                .gt(increment < 0, Note::getGatherTimes, 0)
                .update();
    }
}
