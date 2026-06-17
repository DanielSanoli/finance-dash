package com.sanoli.financedash.repository;

import com.sanoli.financedash.domain.TokenPurpose;
import com.sanoli.financedash.domain.UserActionToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserActionTokenRepository extends JpaRepository<UserActionToken, UUID> {

    Optional<UserActionToken> findByTokenAndPurposeAndUsedFalse(String token, TokenPurpose purpose);
}
