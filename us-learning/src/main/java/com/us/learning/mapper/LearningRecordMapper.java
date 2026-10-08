package com.us.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.us.learning.domain.po.LearningRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 学习记录表 Mapper 接口
 * </p>
 */
public interface LearningRecordMapper extends BaseMapper<LearningRecord> {

    @Insert("INSERT IGNORE INTO learning_record(id,lesson_id,section_id,user_id,moment,finished,finish_time) " +
            "VALUES(#{id},#{lessonId},#{sectionId},#{userId},#{moment},#{finished},#{finishTime})")
    int insertIfAbsent(LearningRecord record);

    @Update("UPDATE learning_record SET moment = #{moment} WHERE id = #{id} AND finished = 0")
    int updatePendingMoment(@Param("id") Long id, @Param("moment") Integer moment);

    @Update("UPDATE learning_lesson SET latest_section_id = #{sectionId}, latest_learn_time = NOW() WHERE id = #{lessonId}")
    int updateLatestLearning(@Param("lessonId") Long lessonId, @Param("sectionId") Long sectionId);

}
