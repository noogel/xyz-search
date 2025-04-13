package noogel.xyz.search.infrastructure.utils.sqlite;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.ReentrantLock;

@Aspect
@Component
@Slf4j
public class SqliteLockAspect {

    private static final ReentrantLock LOCK = new ReentrantLock();

    @Pointcut("@annotation(noogel.xyz.search.infrastructure.utils.sqlite.SqliteLock)")
    public void pointCut() {

    }

    @Around("pointCut()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        LOCK.lock();
        try {
            return pjp.proceed();
        } finally {
            LOCK.unlock();
        }
    }
}
