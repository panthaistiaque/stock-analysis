package com.ihit.stock.repository;

import com.ihit.stock.model.AppUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    List<AppUser> findAllByOrderByIdAsc();

    @Query("SELECT u FROM AppUser u WHERE " +
           "(CAST(:search AS String) IS NULL OR :search = '' OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(CAST(:roleFilter AS String) IS NULL OR :roleFilter = '' OR u.role = :roleFilter)")
    Page<AppUser> findAllFiltered(@Param("search") String search, @Param("roleFilter") String roleFilter, Pageable pageable);
}
