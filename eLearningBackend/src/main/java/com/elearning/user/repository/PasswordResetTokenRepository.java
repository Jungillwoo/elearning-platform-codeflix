package com.elearning.user.repository;

import com.elearning.user.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
  // 비밀번호 재설정 토큰 조회/저장용 Repository

  Optional<PasswordResetToken> findByToken(String token);

  @Modifying
  @Transactional
  void deleteByEmail(String email); // 기존 토큰 제거용
}
