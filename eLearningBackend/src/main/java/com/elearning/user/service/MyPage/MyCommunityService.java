package com.elearning.user.service.MyPage;

import com.elearning.course.entity.Board;
import com.elearning.course.entity.Comment;
import com.elearning.course.repository.BoardLikeRepository;
import com.elearning.course.repository.BoardRepository;
import com.elearning.course.repository.CommentRepository;
import com.elearning.user.dto.MyPage.MyCommunityDTO;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MyCommunityService {
  private final BoardRepository boardRepository;
  private final BoardLikeRepository boardLikeRepository;
  private final CommentRepository commentRepository;

  // 게시글 탭, 사용자가 작성한 게시글 목록을 반환
  public List<MyCommunityDTO> getMyPosts(Long userId) {
    return boardRepository.findAllByUserIdAndIsDelFalse(userId)
      .stream().map(this::toDTO)
      .collect(Collectors.toList());
  }

  // 좋아요 탭, 사용자가 좋아요 누른 게시글 목록을 반환
  public List<MyCommunityDTO> getMyLikedPosts(Long userId) {
    return boardLikeRepository.findBoardsByUserId(userId)
      .stream().map(this::toDTO)
      .collect(Collectors.toList());
  }

  // 댓글 탭, 사용자가 댓글을 단 게시글 목록을 반환
  public List<MyCommunityDTO> getMyCommentedPosts(Long userId) {
    List<Object[]> results = commentRepository.findBoardAndCommentByUserId(userId);
    return results.stream().map(result -> {
      Board board = (Board) result[0];
      Comment comment = (Comment) result[1];
      return toDTO(board, comment.getId());
    }).collect(Collectors.toList());
  }

  // Board 엔티티를 프론트에 필요한 MyCommunityDTO로 변환
  private MyCommunityDTO toDTO(Board board) {
    return toDTO(board, null);
  }

  // Board 엔티티와 댓글 ID를 프론트에 필요한 MyCommunityDTO로 변환
  private MyCommunityDTO toDTO(Board board, Long commentId) {
    // 게시판 타입이 질문이고, 댓글이 하나라도 달렸는지 체크
    boolean isQuestion = board.getBname() == Board.BoardType.질문및답변;
    boolean hasComment = commentRepository.countByBoardIdAndIsDelFalse(board.getId()) > 0;

    return MyCommunityDTO.builder()
      .id(board.getId())
      .commentId(commentId)  // 댓글 ID 추가
      .title(board.getSubject())
      .content(board.getContent())
      .category(mapBoardTypeToCategory(board.getBname().name())) // ENUM → 문자열 매핑
      .createdAt(board.getRegDate())
      .commentCount(commentRepository.countByBoardIdAndIsDelFalse(board.getId()))
      .likeCount(boardLikeRepository.countByBoardId(board.getId()))
      .viewCount(board.getViewCount())
      .thumbnailUrl(null) // 게시글 썸네일이 없으므로 현재는 null
      .solved(isQuestion && hasComment)
      .isNew(board.getRegDate().isAfter(LocalDateTime.now().minusDays(3))) // 최근 3일 게시글이면 NEW
      .isTrending(board.getViewCount() >= 300) // 조회수 300 이상이면 인기글
      .build();
  }

  //  BoardType ENUM 값을 프론트에서 쓰는 카테고리 문자열로 변환
  private String mapBoardTypeToCategory(String bname) {
    return switch (bname) {
      case "질문및답변" -> "question";
      case "프로젝트" -> "project";
      case "자유게시판" -> "free";
      default -> "other"; // 프론트에서 "기타"로 처리
    };
  }

  // 게시글 삭제 (소프트 삭제)
  public boolean deleteMyPost(Long postId, Long userId) {
    Board board = boardRepository.findById(postId)
      .orElseThrow(() -> new EntityNotFoundException("게시글을 찾을 수 없습니다."));

    // 본인의 게시글인지 확인
    if (!board.getUser().getId().equals(userId)) {
      throw new SecurityException("본인의 게시글만 삭제할 수 있습니다.");
    }

    board.setDel(true);
    boardRepository.save(board);
    return true;
  }

  // 댓글 삭제 (소프트 삭제)
  public boolean deleteMyComment(Long commentId, Long userId) {
    System.out.println("댓글 삭제 서비스 시작 - commentId: " + commentId + ", userId: " + userId);
    try {
      Comment comment = commentRepository.findById(commentId)
          .orElseThrow(() -> new EntityNotFoundException("댓글을 찾을 수 없습니다."));

      System.out.println("댓글 작성자 ID: " + comment.getUser().getId());
      // 본인의 댓글인지 확인
      if (!comment.getUser().getId().equals(userId)) {
          System.out.println("권한 없음 - 댓글 작성자: " + comment.getUser().getId() + ", 요청자: " + userId);
          throw new SecurityException("본인의 댓글만 삭제할 수 있습니다.");
      }

      comment.setDel(true);
      Comment savedComment = commentRepository.save(comment);
      System.out.println("댓글 삭제 완료 - commentId: " + commentId + ", 삭제 상태: " + savedComment.isDel());
      return savedComment.isDel();
    } catch (Exception e) {
      System.out.println("댓글 삭제 중 오류 발생: " + e.getMessage());
      e.printStackTrace();
      return false;
    }
  }
}