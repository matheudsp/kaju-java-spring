package com.valedosol.kaju.feature.auth.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.valedosol.kaju.common.exception.ResourceNotFoundException;
import com.valedosol.kaju.feature.auth.model.Account;
import com.valedosol.kaju.feature.auth.repository.AccountRepository;

import java.util.List;


@Service
public class AccountService {

    private final AccountRepository accountRepository;
    
    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }
    
    /**
     * Get all accounts - not cached as this could be a large dataset
     */
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }
    
    /**
     * Get account by ID with caching
     */
    @Cacheable(value = "accounts", key = "#id")
    public Account getAccountById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));
    }
    
    /**
     * Get account by email with caching
     */
    @Cacheable(value = "accounts", key = "#email")
    public Account getAccountByEmail(String email) {
        return accountRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with email: " + email));
    }
    
    /**
     * Create a new account - we add to cache after creation
     */
    @CachePut(value = "accounts", key = "#result.id")
    @Transactional
    public Account createAccount(Account account) {
        return accountRepository.save(account);
    }
    
    /**
     * Update an account - we update the cache with the new values
     */
    @CachePut(value = "accounts", key = "#account.id")
    @Transactional
    public Account updateAccount(Account account) {
        if (!accountRepository.existsById(account.getId())) {
            throw new ResourceNotFoundException("Account not found with id: " + account.getId());
        }
        return accountRepository.save(account);
    }
    
    /**
     * Delete an account - we evict it from the cache
     */
    @CacheEvict(value = "accounts", key = "#id")
    @Transactional
    public void deleteAccount(Long id) {
        if (!accountRepository.existsById(id)) {
            throw new ResourceNotFoundException("Account not found with id: " + id);
        }
        accountRepository.deleteById(id);
    }
    
    /**
     * Update account remaining sends - common operation that would benefit from caching
     */
    @CachePut(value = "accounts", key = "#id")
    @Transactional
    public Account updateRemainingWeeklySends(Long id, Integer sends) {
        Account account = getAccountById(id);
        account.setRemainingWeeklySends(sends);
        return accountRepository.save(account);
    }
}