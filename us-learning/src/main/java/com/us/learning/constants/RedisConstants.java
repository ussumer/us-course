package com.us.learning.constants;

public interface RedisConstants {
    /**
     * 签到记录的KEY前缀，后缀是 用户id + 年月，例如：sign:uid:100:202401
     */
    String SIGN_RECORD_KEY_PREFIX = "sign:uid:";

    /**
     * 积分榜单的KEY前缀，后缀是 年月，例如：points:board:202401（day08）
     */
    String POINTS_BOARD_KEY_PREFIX = "points:board:";
}