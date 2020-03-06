package com.theodore.dao;


import com.theodore.pojo.Account;


public interface AccountDao {

    Account queryAccountByCardNo(String cardNo) throws Exception;

    int updateAccountByCardNo(Account account) throws Exception;
}
