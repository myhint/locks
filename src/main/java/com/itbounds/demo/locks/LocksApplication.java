package com.itbounds.demo.locks;

import com.itbounds.demo.locks.constant.LockNameConstants;
import com.itbounds.demo.locks.zk.ZkDistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class LocksApplication {


  public static void main(String[] args) {
    SpringApplication.run(LocksApplication.class, args);

    ZkDistributedLock zkDistributedLock = new ZkDistributedLock(LockNameConstants.DL_TEST);

    Boolean lock = zkDistributedLock.getLock();

    log.info(" ======== 此次抢夺分布式锁成功与否：{} ======== ", lock);

    try {
      Thread.sleep(1500L);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    // 业务处理完成，紧接着将分布锁释放掉
    zkDistributedLock.releaseLock();

  }

}
