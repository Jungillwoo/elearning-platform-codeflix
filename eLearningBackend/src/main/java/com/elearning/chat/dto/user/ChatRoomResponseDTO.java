package com.elearning.chat.dto.user;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatRoomResponseDTO { // 채팅방 응답 dto
  private Long roomId;
  private String name;
  private String lastMessage;
  private String time;
  private String lastMessageAt;
  private int unreadCount;
  private boolean isInstructor;
  private int participantCount;
  private String profileUrl;
}
