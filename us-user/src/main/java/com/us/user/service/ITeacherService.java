package com.us.user.service;

import com.us.common.domain.dto.PageDTO;
import com.us.user.domain.query.UserPageQuery;
import com.us.user.domain.vo.TeacherPageVO;

/**
 * <p>
 * 教师详情表 服务类
 * </p>
 *
 * @author 虎哥
 * @since 2022-07-12
 */
public interface ITeacherService{
    PageDTO<TeacherPageVO> queryTeacherPage(UserPageQuery pageQuery);

}
