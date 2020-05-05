package com.itbounds.demo.locks.zk;

import com.itbounds.demo.locks.config.MyConfig;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent.Type;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs.Ids;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @Description 分布式锁的Zookeeper实现
 * @Author blake
 * @Date 2020/4/12 10:37 下午
 * @Version 1.0
 */
public class ZkDistributedLock {

  // zkClient instance
  private CuratorFramework zkClient;

  // WorkSpace
  private static final String WORKSPACE = "/lock_workspace";

  // 锁名称 - 对应业务类型
  private String lockName;


  public ZkDistributedLock(String lockName) {
    this.lockName = lockName;
    // 初始化
    init();
  }

  public void init() {
    // 获取zk客户端
    AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(
        MyConfig.class);
    zkClient = (CuratorFramework)applicationContext.getBean(CuratorFramework.class);
    zkClient.start();

    // 创建workspace - 使用持久节点
    try {
      if (Objects.isNull(zkClient.checkExists().forPath(WORKSPACE))) {
        zkClient.create().creatingParentsIfNeeded()
            .withMode(CreateMode.PERSISTENT)
            .withACL(Ids.OPEN_ACL_UNSAFE)
            .forPath(WORKSPACE);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // 使用 zk 的临时节点
  public Boolean getLock() {
    while (true) {
      String lockPath = WORKSPACE + "/" + lockName;
      try {
        if (Objects.isNull(zkClient.checkExists().forPath(lockPath))) {
          zkClient.create().creatingParentsIfNeeded()
              .withMode(CreateMode.EPHEMERAL)
              .withACL(Ids.OPEN_ACL_UNSAFE)
              .forPath(lockPath);
          System.out.println(" get lock successfully! ");
          return true;
        } else {
          // 注册监听 & 进入阻塞
          registerWatcherAndAwait();
          System.out.println(" get lock failure! ");
          return false;
        }
      } catch (Exception e) {
        // 注册监听 & 进入阻塞
        try {
          registerWatcherAndAwait();
          System.out.println(" get lock failure! ");
          return false;
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }
    }
  }

  /**
   * 添加监听的同时进入阻塞 & 临时节点删除事件触发再将线程唤醒
   */
  public void registerWatcherAndAwait() throws Exception {

    CountDownLatch countDownLatch = new CountDownLatch(1);

    PathChildrenCache childrenCache = new PathChildrenCache(zkClient, WORKSPACE, true);
    childrenCache.start();

    childrenCache.getListenable().addListener(new PathChildrenCacheListener() {
      @Override
      public void childEvent(CuratorFramework client,
          PathChildrenCacheEvent event) throws Exception {
        if (event.getType().equals(Type.CHILD_REMOVED) && event.getData().getPath()
            .contains(lockName)) {
          // 唤醒当前线程
          countDownLatch.countDown();
        }
      }
    });
    // 线程挂起
    countDownLatch.await();
  }

  /**
   * 释放锁：即 移除分布锁标识的zk节点
   */
  public void releaseLock() {

    String lockPath = WORKSPACE + "/" + lockName;

    try {
      if (Objects.nonNull(zkClient.checkExists().forPath(lockPath))) {
        zkClient.delete().forPath(lockPath);
        System.out.println(" release lock successfully! ");
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println(" release lock failure! ");
    }
  }

}
