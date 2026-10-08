package com.us.message.service;

import com.us.api.dto.sms.SmsInfoDTO;
import com.us.api.dto.user.UserDTO;
import com.us.message.domain.po.NoticeTemplate;

import java.util.List;

public interface ISmsService {
    void sendMessageByTemplate(NoticeTemplate noticeTemplate, List<UserDTO> users);

    void sendMessage(SmsInfoDTO smsInfoDTO);

    void sendMessageAsync(SmsInfoDTO smsInfoDTO);
}
