package com.theodore.service.impl;


import com.theodore.dao.AccountDao;
import com.theodore.pojo.Account;
import com.theodore.service.TransferService;
import com.theodore.sterotype.Autowired;
import com.theodore.sterotype.Repository;
import com.theodore.sterotype.Service;
import com.theodore.sterotype.Transactional;

@Service
@Transactional
public class TransferServiceImpl implements TransferService {

    @Autowired
    @Repository(value = "jdbcAccountDaoImpl")
    private AccountDao accountDao;


    @Override
    public void transfer(String fromCardNo, String toCardNo, int money) throws Exception {

            Account from = accountDao.queryAccountByCardNo(fromCardNo);
            Account to = accountDao.queryAccountByCardNo(toCardNo);

            from.setMoney(from.getMoney() - money);
            to.setMoney(to.getMoney() + money);

            accountDao.updateAccountByCardNo(to);

            int i = 1/0;
            accountDao.updateAccountByCardNo(from);



    }
}
