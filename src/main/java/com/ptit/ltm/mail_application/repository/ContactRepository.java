package com.ptit.ltm.mail_application.repository;

import com.ptit.ltm.mail_application.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {
    List<Contact> findByUserIdOrderByNameAsc(Long userId);
}
