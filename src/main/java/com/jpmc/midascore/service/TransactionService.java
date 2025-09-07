package com.jpmc.midascore.service;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.repository.TransactionRepository;
import com.jpmc.midascore.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
//    private final RestTemplate restTemplate;


    public TransactionService(UserRepository userRepository,
                              TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
//        this.restTemplate = restTemplate;
    }

    @Transactional
    public void process(Transaction tx) {
        // 1) Validate users
        UserRecord sender = userRepository.findById(tx.getSenderId());
        UserRecord recipient = userRepository.findById(tx.getRecipientId());
        if (sender == null || recipient == null) {
            log.debug("Discarding tx: invalid sender/recipient (senderId={}, recipientId={})",
                    tx.getSenderId(), tx.getRecipientId());
            return;
        }

        // 2) Validate funds
        if (sender.getBalance() < tx.getAmount()) {
            log.debug("Discarding tx: insufficient funds (senderId={}, bal={}, amt={})",
                    sender.getId(), sender.getBalance(), tx.getAmount());
            return;
        }


        // 4) Apply balances
        //    - Sender pays ONLY the transaction amount
        //    - Recipient receives amount + incentive
        sender.setBalance(sender.getBalance() - tx.getAmount());
        recipient.setBalance(recipient.getBalance() + tx.getAmount());
        userRepository.save(sender);
        userRepository.save(recipient);

        // 5) Persist a record including the incentive
        TransactionRecord record = new TransactionRecord(sender, recipient, tx.getAmount());
        transactionRepository.save(record);
    }
}